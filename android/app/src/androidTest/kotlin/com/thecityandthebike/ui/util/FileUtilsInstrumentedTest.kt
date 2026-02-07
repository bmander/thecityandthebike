package com.thecityandthebike.ui.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.exifinterface.media.ExifInterface
import com.thecityandthebike.util.createImageFileAndUri
import com.thecityandthebike.util.cropToSquare
import com.thecityandthebike.util.stripMetadata
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    private fun createTestImageFileWithExif(width: Int, height: Int): File {
        val file = createTestImageFile(width, height)
        val exif = ExifInterface(file.absolutePath)
        exif.setLatLong(40.7128, -74.0060)
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "10/1")
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0")
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, "12:00:00")
        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, "2025:01:01")
        exif.setAttribute(ExifInterface.TAG_MAKE, "TestMake")
        exif.setAttribute(ExifInterface.TAG_MODEL, "TestModel")
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "TestSoftware")
        exif.setAttribute(ExifInterface.TAG_LENS_MAKE, "TestLensMake")
        exif.setAttribute(ExifInterface.TAG_LENS_MODEL, "TestLensModel")
        exif.setAttribute(ExifInterface.TAG_DATETIME, "2025:01:01 12:00:00")
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2025:01:01 12:00:00")
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, "2025:01:01 12:00:00")
        exif.setAttribute(ExifInterface.TAG_ARTIST, "Test Artist")
        exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "Test Copyright")
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "Test Description")
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Test Comment")
        exif.setAttribute(ExifInterface.TAG_CAMERA_OWNER_NAME, "Test Owner")
        exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, "123456")
        exif.saveAttributes()
        return file
    }

    @Test
    fun stripMetadata_removesGpsData() {
        val file = createTestImageFileWithExif(100, 100)
        val exifBefore = ExifInterface(file.absolutePath)
        assertNotNull("GPS latitude should be set before strip",
            exifBefore.getAttribute(ExifInterface.TAG_GPS_LATITUDE))

        stripMetadata(file)

        val exif = ExifInterface(file.absolutePath)
        assertNull("GPS latitude should be removed",
            exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull("GPS longitude should be removed",
            exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))
        assertNull("GPS altitude should be removed",
            exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE))
        assertNull("GPS timestamp should be removed",
            exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP))
        assertNull("GPS datestamp should be removed",
            exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP))
    }

    @Test
    fun stripMetadata_removesCameraInfo() {
        val file = createTestImageFileWithExif(100, 100)
        stripMetadata(file)

        val exif = ExifInterface(file.absolutePath)
        assertNull("Make should be removed",
            exif.getAttribute(ExifInterface.TAG_MAKE))
        assertNull("Model should be removed",
            exif.getAttribute(ExifInterface.TAG_MODEL))
        assertNull("Software should be removed",
            exif.getAttribute(ExifInterface.TAG_SOFTWARE))
        assertNull("Lens make should be removed",
            exif.getAttribute(ExifInterface.TAG_LENS_MAKE))
        assertNull("Lens model should be removed",
            exif.getAttribute(ExifInterface.TAG_LENS_MODEL))
    }

    @Test
    fun stripMetadata_removesTimestamps() {
        val file = createTestImageFileWithExif(100, 100)
        stripMetadata(file)

        val exif = ExifInterface(file.absolutePath)
        assertNull("DateTime should be removed",
            exif.getAttribute(ExifInterface.TAG_DATETIME))
        assertNull("DateTimeOriginal should be removed",
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
        assertNull("DateTimeDigitized should be removed",
            exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED))
    }

    @Test
    fun cropToSquare_outputHasNoExifMetadata() {
        val file = createTestImageFileWithExif(200, 100)
        val result = cropToSquare(file)

        val exif = ExifInterface(result.absolutePath)
        assertNull("GPS latitude should not be in cropped output",
            exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull("Make should not be in cropped output",
            exif.getAttribute(ExifInterface.TAG_MAKE))
        assertNull("Model should not be in cropped output",
            exif.getAttribute(ExifInterface.TAG_MODEL))
        assertNull("DateTime should not be in cropped output",
            exif.getAttribute(ExifInterface.TAG_DATETIME))
        assertNull("Artist should not be in cropped output",
            exif.getAttribute(ExifInterface.TAG_ARTIST))
        assertNull("Camera owner should not be in cropped output",
            exif.getAttribute(ExifInterface.TAG_CAMERA_OWNER_NAME))
    }
}
