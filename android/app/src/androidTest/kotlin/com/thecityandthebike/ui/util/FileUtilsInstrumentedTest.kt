package com.thecityandthebike.ui.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thecityandthebike.util.createImageFileAndUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileUtilsInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

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
}
