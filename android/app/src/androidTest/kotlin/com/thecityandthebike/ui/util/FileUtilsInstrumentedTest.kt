package com.thecityandthebike.ui.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thecityandthebike.util.createImageUri
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
    fun createImageUri_returnsNonNullUri() {
        val uri = createImageUri(context)
        assertNotNull("createImageUri should return a non-null URI", uri)
    }

    @Test
    fun createImageUri_hasContentScheme() {
        val uri = createImageUri(context)
        assertEquals(
            "URI should have content:// scheme",
            "content",
            uri.scheme
        )
    }

    @Test
    fun createImageUri_hasCorrectAuthority() {
        val uri = createImageUri(context)
        val expectedAuthority = "${context.packageName}.fileprovider"
        assertEquals(
            "URI should have correct authority",
            expectedAuthority,
            uri.authority
        )
    }

    @Test
    fun createImageUri_generatesUniqueUris() {
        val uri1 = createImageUri(context)
        // Small delay to ensure different timestamps
        Thread.sleep(10)
        val uri2 = createImageUri(context)

        assertNotEquals(
            "Each call should generate a unique URI",
            uri1,
            uri2
        )
    }

    @Test
    fun createImageUri_pathContainsJpgExtension() {
        val uri = createImageUri(context)
        assertTrue(
            "URI path should contain .jpg extension",
            uri.path?.endsWith(".jpg") == true
        )
    }
}
