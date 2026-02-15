package com.thecityandthebike.data.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class MaskProcessorInstrumentedTest {

    private lateinit var processor: MaskProcessor
    private lateinit var cacheDir: File

    @Before
    fun setup() {
        processor = MaskProcessor()
        cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
    }

    private fun saveBitmapToFile(bitmap: Bitmap, name: String): File {
        val file = File(cacheDir, name)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    private fun createCircleMaskFile(): File {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint().apply { color = Color.WHITE }
        canvas.drawCircle(100f, 100f, 50f, paint)
        val file = saveBitmapToFile(bitmap, "test_circle_mask.png")
        bitmap.recycle()
        return file
    }

    @Test
    fun process_decodesFileAndReturnsValidRing() = runBlocking {
        val file = createCircleMaskFile()
        try {
            val result = processor.process(file)

            assertEquals(200, result.width)
            assertEquals(200, result.height)
            assertTrue("Ring should have at least 4 points", result.ring.size >= 4)
            assertEquals("Ring should be closed", result.ring.first(), result.ring.last())
            result.ring.forEach { coord ->
                assertEquals("Each coordinate should be a pair", 2, coord.size)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun process_nonexistentFileThrows() = runBlocking {
        val file = File(cacheDir, "does_not_exist.png")
        try {
            processor.process(file)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Could not decode image", e.message)
        }
    }

    @Test
    fun process_corruptFileThrows() = runBlocking {
        val file = File(cacheDir, "corrupt_mask.png")
        file.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))
        try {
            processor.process(file)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Could not decode image", e.message)
        } finally {
            file.delete()
        }
    }

    @Test
    fun process_emptyMaskThrows() = runBlocking {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val file = saveBitmapToFile(bitmap, "test_empty_mask.png")
        bitmap.recycle()
        try {
            processor.process(file)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("No contours found in mask", e.message)
        } finally {
            file.delete()
        }
    }

    @Test
    fun process_rectangleMaskReturnsCorrectRegion() = runBlocking {
        val bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(100f, 100f, 200f, 200f, paint)
        val file = saveBitmapToFile(bitmap, "test_rect_mask.png")
        bitmap.recycle()
        try {
            val result = processor.process(file)

            assertEquals(300, result.width)
            assertEquals(300, result.height)

            val xs = result.ring.map { it[0] }
            val ys = result.ring.map { it[1] }
            assertTrue("Ring x-coords should be within rect region", xs.min() >= 95f)
            assertTrue("Ring y-coords should be within rect region", ys.min() >= 95f)
            assertTrue("Ring x-coords should be within rect region", xs.max() <= 205f)
            assertTrue("Ring y-coords should be within rect region", ys.max() <= 205f)
        } finally {
            file.delete()
        }
    }
}
