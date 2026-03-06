package com.thecityandthebike.viewmodel

import android.net.Uri
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.upload.PendingUpload
import com.thecityandthebike.upload.PendingUploadStatus
import com.thecityandthebike.upload.UploadManager
import com.thecityandthebike.upload.UploadQueue
import com.thecityandthebike.upload.UploadState
import com.thecityandthebike.util.ConnectivityMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UploadManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var uploadQueue: UploadQueue
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private val isOnlineFlow = MutableStateFlow(true)
    private val pendingUploadsFlow = MutableStateFlow<List<PendingUpload>>(emptyList())
    private lateinit var appScope: CoroutineScope
    // Mutable list to simulate queue state changes across getAll() calls
    private val queueState = mutableListOf<PendingUpload>()

    @Before
    fun setup() {
        submissionRepository = mockk()
        uploadQueue = mockk(relaxUnitFun = true)
        connectivityMonitor = mockk()
        appScope = CoroutineScope(SupervisorJob() + testDispatcher)
        every { connectivityMonitor.isOnline } returns isOnlineFlow
        every { uploadQueue.pendingUploads } returns pendingUploadsFlow
        coEvery { uploadQueue.getAll() } answers { queueState.toList() }
        coEvery { uploadQueue.remove(any()) } answers {
            queueState.removeAll { it.id == firstArg<String>() }
        }
        coEvery { uploadQueue.updateStatus(any(), any()) } answers {
            val id = firstArg<String>()
            val status = secondArg<PendingUploadStatus>()
            val idx = queueState.indexOfFirst { it.id == id }
            if (idx >= 0) queueState[idx] = queueState[idx].copy(status = status)
        }
        mockkStatic(Uri::class)
        every { Uri.fromFile(any()) } returns mockk()
    }

    @After
    fun tearDown() {
        appScope.cancel()
        unmockkStatic(Uri::class)
    }

    private fun createUploadManager(): UploadManager {
        return UploadManager(submissionRepository, uploadQueue, connectivityMonitor, appScope)
    }

    private fun makePendingUpload(
        id: String = "test-id",
        bikeQrId: String = "bike1",
        status: PendingUploadStatus = PendingUploadStatus.QUEUED
    ) = PendingUpload(
        id = id,
        imageFileName = "image.jpg",
        bikeQrId = bikeQrId,
        capturedDate = "2026-03-06",
        createdAt = 1000L,
        status = status
    )

    private fun stubSuccessResponse() {
        val response = SubmissionResponse(
            submissionId = "new-1",
            userId = "user1",
            bikeQrId = "bike1",
            imageUrl = "http://example.com/uploaded.jpg"
        )
        coEvery { submissionRepository.createSubmission(any<File>(), any(), any(), any()) } returns ApiResult.Success(response)
    }

    @Test
    fun `upload enqueues and processes queue on success`() = testScope.runTest {
        val uri = mockk<Uri>()
        val pending = makePendingUpload()
        coEvery { uploadQueue.enqueue(uri, "bike1", null, any()) } answers {
            queueState.add(pending)
            pending
        }
        every { uploadQueue.getImageFile("test-id") } returns File("/fake/image.jpg")
        stubSuccessResponse()

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uploadManager.state.value is UploadState.Success)
        coVerify { uploadQueue.remove("test-id") }
    }

    @Test
    fun `upload returns error when enqueue fails`() = testScope.runTest {
        val uri = mockk<Uri>()
        coEvery { uploadQueue.enqueue(uri, "bike1", null, any()) } returns null

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = uploadManager.state.value
        assertTrue(state is UploadState.Error)
        assertEquals("Could not read image file", (state as UploadState.Error).message)
    }

    @Test
    fun `offline enqueue stores without API call`() = testScope.runTest {
        isOnlineFlow.value = false
        val uri = mockk<Uri>()
        val pending = makePendingUpload()
        coEvery { uploadQueue.enqueue(uri, "bike1", null, any()) } answers {
            queueState.add(pending)
            pending
        }

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { submissionRepository.createSubmission(any<File>(), any(), any(), any()) }
        assertEquals(1, queueState.size)
    }

    @Test
    fun `coming online triggers queue processing`() = testScope.runTest {
        isOnlineFlow.value = false
        val pending = makePendingUpload()
        queueState.add(pending)
        every { uploadQueue.getImageFile("test-id") } returns File("/fake/image.jpg")
        stubSuccessResponse()

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate reconnect
        isOnlineFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uploadManager.state.value is UploadState.Success)
        coVerify { uploadQueue.remove("test-id") }
    }

    @Test
    fun `network error keeps item in queue as FAILED`() = testScope.runTest {
        val uri = mockk<Uri>()
        val pending = makePendingUpload()
        coEvery { uploadQueue.enqueue(uri, "bike1", null, any()) } answers {
            queueState.add(pending)
            pending
        }
        every { uploadQueue.getImageFile("test-id") } returns File("/fake/image.jpg")
        coEvery { submissionRepository.createSubmission(any<File>(), any(), any(), any()) } returns
            ApiResult.Error(AppError.Network(java.io.IOException("timeout")))

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uploadManager.state.value is UploadState.Error)
        assertEquals(PendingUploadStatus.FAILED, queueState.first().status)
        assertEquals(1, queueState.size)
    }

    @Test
    fun `4xx error removes item from queue`() = testScope.runTest {
        val uri = mockk<Uri>()
        val pending = makePendingUpload()
        coEvery { uploadQueue.enqueue(uri, "bike1", null, any()) } answers {
            queueState.add(pending)
            pending
        }
        every { uploadQueue.getImageFile("test-id") } returns File("/fake/image.jpg")
        coEvery { submissionRepository.createSubmission(any<File>(), any(), any(), any()) } returns
            ApiResult.Error(AppError.Validation("field", "Invalid submission"))

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uploadManager.state.value is UploadState.Error)
        assertTrue(queueState.isEmpty())
    }

    @Test
    fun `retryUpload resets FAILED to QUEUED and processes`() = testScope.runTest {
        val pending = makePendingUpload(status = PendingUploadStatus.FAILED)
        queueState.add(pending)
        every { uploadQueue.getImageFile("test-id") } returns File("/fake/image.jpg")
        stubSuccessResponse()

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadManager.retryUpload()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uploadManager.state.value is UploadState.Success)
        assertTrue(queueState.isEmpty())
    }

    @Test
    fun `server error keeps item in queue as FAILED`() = testScope.runTest {
        val uri = mockk<Uri>()
        val pending = makePendingUpload()
        coEvery { uploadQueue.enqueue(uri, "bike1", null, any()) } answers {
            queueState.add(pending)
            pending
        }
        every { uploadQueue.getImageFile("test-id") } returns File("/fake/image.jpg")
        coEvery { submissionRepository.createSubmission(any<File>(), any(), any(), any()) } returns
            ApiResult.Error(AppError.Server(500, "Internal Server Error"))

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uploadManager.state.value is UploadState.Error)
        assertEquals(PendingUploadStatus.FAILED, queueState.first().status)
        assertEquals(1, queueState.size)
    }

    @Test
    fun `multiple queued items process sequentially`() = testScope.runTest {
        val pending1 = makePendingUpload(id = "id-1", bikeQrId = "bike1")
        val pending2 = makePendingUpload(id = "id-2", bikeQrId = "bike2")
        queueState.addAll(listOf(pending1, pending2))
        every { uploadQueue.getImageFile("id-1") } returns File("/fake/image1.jpg")
        every { uploadQueue.getImageFile("id-2") } returns File("/fake/image2.jpg")

        val response1 = SubmissionResponse(
            submissionId = "s1", userId = "user1", bikeQrId = "bike1",
            imageUrl = "http://example.com/1.jpg"
        )
        val response2 = SubmissionResponse(
            submissionId = "s2", userId = "user1", bikeQrId = "bike2",
            imageUrl = "http://example.com/2.jpg"
        )
        coEvery { submissionRepository.createSubmission(any<File>(), eq("bike1"), any(), any()) } returns ApiResult.Success(response1)
        coEvery { submissionRepository.createSubmission(any<File>(), eq("bike2"), any(), any()) } returns ApiResult.Success(response2)

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        // Trigger processing by simulating reconnect
        isOnlineFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        isOnlineFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        // Both items should have been removed
        assertTrue(queueState.isEmpty())
        coVerify { uploadQueue.remove("id-1") }
        coVerify { uploadQueue.remove("id-2") }
        // Final state is the last success
        assertTrue(uploadManager.state.value is UploadState.Success)
    }

    @Test
    fun `multiple items stops on retryable error`() = testScope.runTest {
        val pending1 = makePendingUpload(id = "id-1", bikeQrId = "bike1")
        val pending2 = makePendingUpload(id = "id-2", bikeQrId = "bike2")
        queueState.addAll(listOf(pending1, pending2))
        every { uploadQueue.getImageFile("id-1") } returns File("/fake/image1.jpg")
        every { uploadQueue.getImageFile("id-2") } returns File("/fake/image2.jpg")

        // First item fails with network error
        coEvery { submissionRepository.createSubmission(any<File>(), eq("bike1"), any(), any()) } returns
            ApiResult.Error(AppError.Network(java.io.IOException("timeout")))

        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        isOnlineFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        isOnlineFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        // First item should be FAILED, second still QUEUED (processing stopped)
        assertEquals(2, queueState.size)
        assertEquals(PendingUploadStatus.FAILED, queueState[0].status)
        assertEquals(PendingUploadStatus.QUEUED, queueState[1].status)
        // Second item's API should never have been called
        coVerify(exactly = 0) { submissionRepository.createSubmission(any<File>(), eq("bike2"), any(), any()) }
    }

    @Test
    fun `clearError resets state to idle`() = testScope.runTest {
        val uploadManager = createUploadManager()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadManager.clearError()
        assertEquals(UploadState.Idle, uploadManager.state.value)
    }
}
