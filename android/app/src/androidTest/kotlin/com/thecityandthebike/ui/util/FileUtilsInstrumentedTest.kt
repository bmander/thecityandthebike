package com.thecityandthebike.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thecityandthebike.util.createImageFileAndUri
import com.thecityandthebike.util.cropToSquare
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileUtilsInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val tempFiles = mutableListOf<File>()

    @After
    fun cleanUpTempFiles() {
        tempFiles.forEach { it.delete() }
        tempFiles.clear()
    }

    private fun createTestImageFile(width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        val file = File(context.cacheDir, "test_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        tempFiles.add(file)
        return file
    }

    @Test
    fun createImageFileAndUri_returnsNonNullPair() {
        val (file, uri) = createImageFileAndUri(context)
        assertNotNull("File should not be null", file)
        assertNotNull("URI should not be null", uri)
    }

    @Test
    fun createImageFileAndUri_uriHasContentScheme() {
        val (_, uri) = createImageFileAndUri(context)
        assertEquals(
            "URI should have content:// scheme",
            "content",
            uri.scheme
        )
    }

    @Test
    fun createImageFileAndUri_uriHasCorrectAuthority() {
        val (_, uri) = createImageFileAndUri(context)
        val expectedAuthority = "${context.packageName}.fileprovider"
        assertEquals(
            "URI should have correct authority",
            expectedAuthority,
            uri.authority
        )
    }

    @Test
    fun createImageFileAndUri_generatesUniqueResults() {
        val (file1, uri1) = createImageFileAndUri(context)
        Thread.sleep(10)
        val (file2, uri2) = createImageFileAndUri(context)

        assertNotEquals("Each call should generate a unique file", file1, file2)
        assertNotEquals("Each call should generate a unique URI", uri1, uri2)
    }

    @Test
    fun createImageFileAndUri_fileHasJpgExtension() {
        val (file, _) = createImageFileAndUri(context)
        assertTrue(
            "File name should end with .jpg",
            file.name.endsWith(".jpg")
        )
    }

    @Test
    fun createImageFileAndUri_fileIsInImagesDir() {
        val (file, _) = createImageFileAndUri(context)
        assertTrue(
            "File should be in the images cache directory",
            file.parentFile?.name == "images"
        )
    }

    @Test
    fun cropToSquare_landscapeImageBecomesSquare() {
        val file = createTestImageFile(200, 100)
        val result = cropToSquare(file)
        val decoded = BitmapFactory.decodeFile(result.absolutePath)
        assertNotNull("Decoded bitmap should not be null", decoded)
        assertEquals("Width should equal the shorter side", 100, decoded!!.width)
        assertEquals("Height should equal the shorter side", 100, decoded.height)
        decoded.recycle()
    }

    @Test
    fun cropToSquare_portraitImageBecomesSquare() {
        val file = createTestImageFile(100, 200)
        val result = cropToSquare(file)
        val decoded = BitmapFactory.decodeFile(result.absolutePath)
        assertNotNull("Decoded bitmap should not be null", decoded)
        assertEquals("Width should equal the shorter side", 100, decoded!!.width)
        assertEquals("Height should equal the shorter side", 100, decoded.height)
        decoded.recycle()
    }

    @Test
    fun cropToSquare_alreadySquareImageUnchangedDimensions() {
        val file = createTestImageFile(100, 100)
        val result = cropToSquare(file)
        val decoded = BitmapFactory.decodeFile(result.absolutePath)
        assertNotNull("Decoded bitmap should not be null", decoded)
        assertEquals("Width should remain 100", 100, decoded!!.width)
        assertEquals("Height should remain 100", 100, decoded.height)
        decoded.recycle()
    }

    @Test
    fun cropToSquare_outputIsValidJpeg() {
        val file = createTestImageFile(200, 100)
        val result = cropToSquare(file)
        val decoded = BitmapFactory.decodeFile(result.absolutePath)
        assertNotNull("Output file should be a valid decodable image", decoded)
        decoded!!.recycle()
    }

    @Test
    fun cropToSquare_corruptFileReturnsFileUnchanged() {
        val file = File(context.cacheDir, "corrupt_${System.currentTimeMillis()}.jpg")
        file.writeBytes("not an image".toByteArray())
        tempFiles.add(file)
        val result = cropToSquare(file)
        assertEquals("Should return the same file object", file, result)
        assertTrue("File should still exist", file.exists())
    }
}
