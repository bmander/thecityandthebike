package com.thecityandthebike.upload

import android.net.Uri
import com.thecityandthebike.util.ImagePreparer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UploadQueueTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var imagePreparer: ImagePreparer
    private lateinit var filesDir: File

    @Before
    fun setup() {
        imagePreparer = mockk()
        filesDir = tempFolder.root
    }

    private fun createQueue(): UploadQueue {
        val context = mockk<android.content.Context> {
            io.mockk.every { filesDir } returns this@UploadQueueTest.filesDir
        }
        return UploadQueue(context, imagePreparer)
    }

    @Test
    fun `enqueue creates files and returns pending upload`() = runTest {
        val uri = mockk<Uri>()
        val sourceFile = tempFolder.newFile("source.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile

        val queue = createQueue()
        val upload = queue.enqueue(uri, "bike1", "left", "2026-03-06")

        assertNotNull(upload)
        assertEquals("bike1", upload!!.bikeQrId)
        assertEquals("left", upload.side)
        assertEquals("2026-03-06", upload.capturedDate)
        assertEquals(PendingUploadStatus.QUEUED, upload.status)

        // Verify files exist
        val imageFile = queue.getImageFile(upload.id)
        assertTrue(imageFile.exists())
        assertArrayEquals(byteArrayOf(1, 2, 3), imageFile.readBytes())
    }

    @Test
    fun `enqueue returns null when image preparation fails`() = runTest {
        val uri = mockk<Uri>()
        coEvery { imagePreparer.prepareImageFile(uri) } returns null

        val queue = createQueue()
        val upload = queue.enqueue(uri, "bike1", null, "2026-03-06")

        assertNull(upload)
        assertEquals(0, queue.pendingUploads.value.size)
    }

    @Test
    fun `getAll returns enqueued items`() = runTest {
        val uri = mockk<Uri>()
        val sourceFile = tempFolder.newFile("source.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile

        val queue = createQueue()
        queue.enqueue(uri, "bike1", null, "2026-03-06")

        // prepareImageFile returns a new file each time since the old was deleted
        val sourceFile2 = tempFolder.newFile("source2.jpg")
        sourceFile2.writeBytes(byteArrayOf(4, 5, 6))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile2

        queue.enqueue(uri, "bike2", "right", "2026-03-07")

        val all = queue.getAll()
        assertEquals(2, all.size)
        assertEquals("bike1", all[0].bikeQrId)
        assertEquals("bike2", all[1].bikeQrId)
    }

    @Test
    fun `remove cleans up files and state`() = runTest {
        val uri = mockk<Uri>()
        val sourceFile = tempFolder.newFile("source.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile

        val queue = createQueue()
        val upload = queue.enqueue(uri, "bike1", null, "2026-03-06")!!

        queue.remove(upload.id)

        assertEquals(0, queue.getAll().size)
        assertFalse(queue.getImageFile(upload.id).exists())
    }

    @Test
    fun `updateStatus persists new status`() = runTest {
        val uri = mockk<Uri>()
        val sourceFile = tempFolder.newFile("source.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile

        val queue = createQueue()
        val upload = queue.enqueue(uri, "bike1", null, "2026-03-06")!!

        queue.updateStatus(upload.id, PendingUploadStatus.UPLOADING)

        val updated = queue.getAll().first()
        assertEquals(PendingUploadStatus.UPLOADING, updated.status)

        // Verify persistence by creating a new queue from the same directory
        val queue2 = createQueue()
        val reloaded = queue2.getAll().first()
        // Should be reset to QUEUED on init (app killed mid-upload recovery)
        assertEquals(PendingUploadStatus.QUEUED, reloaded.status)
    }

    @Test
    fun `init resets UPLOADING items to QUEUED`() = runTest {
        val uri = mockk<Uri>()
        val sourceFile = tempFolder.newFile("source.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile

        val queue = createQueue()
        val upload = queue.enqueue(uri, "bike1", null, "2026-03-06")!!
        queue.updateStatus(upload.id, PendingUploadStatus.UPLOADING)
        assertEquals(PendingUploadStatus.UPLOADING, queue.getAll().first().status)

        // Create a fresh queue from the same directory — simulates app restart
        val queue2 = createQueue()
        val recovered = queue2.getAll().first()
        assertEquals(PendingUploadStatus.QUEUED, recovered.status)
        assertEquals(upload.id, recovered.id)
    }

    @Test
    fun `init leaves FAILED items as FAILED`() = runTest {
        val uri = mockk<Uri>()
        val sourceFile = tempFolder.newFile("source.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile

        val queue = createQueue()
        val upload = queue.enqueue(uri, "bike1", null, "2026-03-06")!!
        queue.updateStatus(upload.id, PendingUploadStatus.FAILED)

        val queue2 = createQueue()
        val recovered = queue2.getAll().first()
        assertEquals(PendingUploadStatus.FAILED, recovered.status)
    }

    @Test
    fun `init cleans up directories with corrupt metadata`() = runTest {
        // Create a valid entry first
        val uri = mockk<Uri>()
        val sourceFile = tempFolder.newFile("source.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile

        val queue = createQueue()
        queue.enqueue(uri, "bike1", null, "2026-03-06")

        // Manually create a corrupt entry in the queue directory
        val corruptDir = File(File(filesDir, "pending_uploads"), "corrupt-id")
        corruptDir.mkdirs()
        File(corruptDir, "metadata.json").writeText("not valid json{{{")
        File(corruptDir, "image.jpg").writeBytes(byteArrayOf(7, 8, 9))

        // Also create a directory with no metadata at all
        val orphanDir = File(File(filesDir, "pending_uploads"), "orphan-id")
        orphanDir.mkdirs()
        File(orphanDir, "image.jpg").writeBytes(byteArrayOf(4, 5, 6))

        // Create a new queue — simulates app restart
        val queue2 = createQueue()

        // Should only have the valid entry
        assertEquals(1, queue2.getAll().size)
        assertEquals("bike1", queue2.getAll().first().bikeQrId)
        // Corrupt and orphan dirs should be cleaned up
        assertFalse(corruptDir.exists())
        assertFalse(orphanDir.exists())
    }

    @Test
    fun `pendingUploads StateFlow reflects changes`() = runTest {
        val uri = mockk<Uri>()
        val sourceFile = tempFolder.newFile("source.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))
        coEvery { imagePreparer.prepareImageFile(uri) } returns sourceFile

        val queue = createQueue()
        assertEquals(0, queue.pendingUploads.value.size)

        val upload = queue.enqueue(uri, "bike1", null, "2026-03-06")!!
        assertEquals(1, queue.pendingUploads.value.size)

        queue.remove(upload.id)
        assertEquals(0, queue.pendingUploads.value.size)
    }
}
