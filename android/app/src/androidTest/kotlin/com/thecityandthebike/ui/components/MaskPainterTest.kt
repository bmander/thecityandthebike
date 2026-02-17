package com.thecityandthebike.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.test.platform.app.InstrumentationRegistry
import com.thecityandthebike.setContentWithTheme
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MaskPainterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testUri = Uri.parse("content://com.thecityandthebike.test/image_0")

    // --- MaskPainter composable ---

    @Test
    fun maskPainter_displaysImage() {
        val state = MaskPainterState()

        composeTestRule.setContentWithTheme {
            MaskPainter(
                imageUri = testUri,
                thumbnailUri = null,
                state = state
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Photo to tag")
            .assertIsDisplayed()
    }

    @Test
    fun maskPainter_fallsBackToThumbnailUri() {
        val state = MaskPainterState()
        val thumbnailUri = Uri.parse("content://com.thecityandthebike.test/thumb_0")

        composeTestRule.setContentWithTheme {
            MaskPainter(
                imageUri = null,
                thumbnailUri = thumbnailUri,
                state = state
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Photo to tag")
            .assertIsDisplayed()
    }

    @Test
    fun maskPainter_dragGestureAddsStroke() {
        val state = MaskPainterState()

        composeTestRule.setContentWithTheme {
            MaskPainter(
                imageUri = testUri,
                thumbnailUri = null,
                state = state
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Photo to tag")
            .performTouchInput {
                swipe(
                    start = Offset(center.x - 50f, center.y),
                    end = Offset(center.x + 50f, center.y),
                    durationMillis = 200
                )
            }

        composeTestRule.waitForIdle()

        assertTrue("Drag gesture should add a stroke", state.strokes.isNotEmpty())
    }

    @Test
    fun maskPainter_multipleDragsAddMultipleStrokes() {
        val state = MaskPainterState()

        composeTestRule.setContentWithTheme {
            MaskPainter(
                imageUri = testUri,
                thumbnailUri = null,
                state = state
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Photo to tag")
            .performTouchInput {
                swipe(
                    start = Offset(center.x - 50f, center.y - 50f),
                    end = Offset(center.x + 50f, center.y - 50f),
                    durationMillis = 200
                )
            }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Photo to tag")
            .performTouchInput {
                swipe(
                    start = Offset(center.x - 50f, center.y + 50f),
                    end = Offset(center.x + 50f, center.y + 50f),
                    durationMillis = 200
                )
            }

        composeTestRule.waitForIdle()

        assertEquals("Two drags should produce two strokes", 2, state.strokes.size)
    }

    @Test
    fun maskPainter_strokeHasMultiplePoints() {
        val state = MaskPainterState()

        composeTestRule.setContentWithTheme {
            MaskPainter(
                imageUri = testUri,
                thumbnailUri = null,
                state = state
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Photo to tag")
            .performTouchInput {
                swipe(
                    start = Offset(center.x - 50f, center.y),
                    end = Offset(center.x + 50f, center.y),
                    durationMillis = 200
                )
            }

        composeTestRule.waitForIdle()

        assertTrue(
            "Stroke should contain multiple points",
            state.strokes.first().points.size >= 2
        )
    }

    @Test
    fun maskPainter_currentStrokeEmptyAfterDragEnds() {
        val state = MaskPainterState()

        composeTestRule.setContentWithTheme {
            MaskPainter(
                imageUri = testUri,
                thumbnailUri = null,
                state = state
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Photo to tag")
            .performTouchInput {
                swipe(
                    start = Offset(center.x - 50f, center.y),
                    end = Offset(center.x + 50f, center.y),
                    durationMillis = 200
                )
            }

        composeTestRule.waitForIdle()

        assertTrue(
            "currentStroke should be empty after drag ends",
            state.currentStroke.isEmpty()
        )
    }

    // --- exportComposited ---

    private fun createTestImageUri(): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val file = File(context.cacheDir, "test_image.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun exportComposited_returnsNullWhenStrokesEmpty() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = MaskPainterState()
        state.canvasSize = IntSize(100, 100)

        assertNull(runBlocking { state.exportComposited(context, createTestImageUri()) })
    }

    @Test
    fun exportComposited_returnsNullWhenCanvasSizeZero() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = MaskPainterState()
        state.strokes.add(StrokePath(listOf(StrokePoint(10f, 10f), StrokePoint(50f, 50f))))

        assertNull(runBlocking { state.exportComposited(context, createTestImageUri()) })
    }

    @Test
    fun exportComposited_returnsNullWhenCanvasWidthZero() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = MaskPainterState()
        state.strokes.add(StrokePath(listOf(StrokePoint(10f, 10f), StrokePoint(50f, 50f))))
        state.canvasSize = IntSize(0, 100)

        assertNull(runBlocking { state.exportComposited(context, createTestImageUri()) })
    }

    @Test
    fun exportComposited_returnsNullWhenCanvasHeightZero() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = MaskPainterState()
        state.strokes.add(StrokePath(listOf(StrokePoint(10f, 10f), StrokePoint(50f, 50f))))
        state.canvasSize = IntSize(100, 0)

        assertNull(runBlocking { state.exportComposited(context, createTestImageUri()) })
    }

    @Test
    fun exportComposited_returnsFileWhenValid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = MaskPainterState()
        state.strokes.add(StrokePath(listOf(StrokePoint(10f, 10f), StrokePoint(50f, 50f))))
        state.canvasSize = IntSize(100, 100)

        val result = runBlocking { state.exportComposited(context, createTestImageUri()) }

        assertNotNull("Should produce an output file", result)
        assertTrue("Output file should exist", result!!.exists())
        assertTrue("Output file should be non-empty", result.length() > 0)
    }

    @Test
    fun exportComposited_outputIsPng() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = MaskPainterState()
        state.strokes.add(StrokePath(listOf(StrokePoint(10f, 10f), StrokePoint(50f, 50f))))
        state.canvasSize = IntSize(100, 100)

        val result = runBlocking { state.exportComposited(context, createTestImageUri()) }

        assertNotNull(result)
        assertTrue("Output filename should end with .png", result!!.name.endsWith(".png"))
    }

    @Test
    fun exportComposited_worksWithHttpUrl() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val state = MaskPainterState()
        state.strokes.add(StrokePath(listOf(StrokePoint(10f, 10f), StrokePoint(50f, 50f))))
        state.canvasSize = IntSize(100, 100)

        // Serve a test PNG over HTTP
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val pngBytes = ByteArrayOutputStream().also { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }.toByteArray()
        bitmap.recycle()

        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(Buffer().write(pngBytes)))
        server.start()

        try {
            val httpUri = Uri.parse(server.url("/test-image.png").toString())
            val result = runBlocking { state.exportComposited(context, httpUri) }

            assertNotNull("Should produce output from HTTP URL", result)
            assertTrue("Output file should exist", result!!.exists())
            assertTrue("Output file should be non-empty", result.length() > 0)
        } finally {
            server.shutdown()
        }
    }

    // --- exportCompositedFromRing ---

    private fun createLargeTestImageUri(width: Int = 400, height: Int = 400): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.RED)
        val file = File(context.cacheDir, "test_large_image.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return Uri.fromFile(file)
    }

    @Test
    fun exportCompositedFromRing_returnsNullWhenRingTooSmall() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ring = listOf(listOf(10f, 10f), listOf(50f, 50f)) // only 2 points
        val result = runBlocking {
            exportCompositedFromRing(context, createLargeTestImageUri(), ring, 400, 400)
        }
        assertNull("Should return null for ring with fewer than 3 points", result)
    }

    @Test
    fun exportCompositedFromRing_cropsToPolygonBoundingBox() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Ring occupies a small region: x in [100,200], y in [100,200] within a 400x400 mask
        val ring = listOf(
            listOf(100f, 100f),
            listOf(200f, 100f),
            listOf(200f, 200f),
            listOf(100f, 200f)
        )
        val result = runBlocking {
            exportCompositedFromRing(context, createLargeTestImageUri(), ring, 400, 400)
        }
        assertNotNull("Should produce output", result)
        assertTrue("Output file should exist", result!!.exists())

        val decoded = BitmapFactory.decodeFile(result.absolutePath)
        assertNotNull("Should decode output bitmap", decoded)

        // The output should be cropped to roughly 100x100, not the full 400x400
        assertTrue(
            "Width (${decoded.width}) should be much smaller than 400",
            decoded.width <= 120
        )
        assertTrue(
            "Height (${decoded.height}) should be much smaller than 400",
            decoded.height <= 120
        )
        decoded.recycle()
    }

    @Test
    fun exportCompositedFromRing_scalesToImageDimensions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Image is 400x400 but mask was drawn at 200x200 scale
        // Ring at mask coords [50,50]-[150,150] maps to image coords [100,100]-[300,300]
        val ring = listOf(
            listOf(50f, 50f),
            listOf(150f, 50f),
            listOf(150f, 150f),
            listOf(50f, 150f)
        )
        val result = runBlocking {
            exportCompositedFromRing(context, createLargeTestImageUri(), ring, 200, 200)
        }
        assertNotNull("Should produce output", result)

        val decoded = BitmapFactory.decodeFile(result!!.absolutePath)
        assertNotNull("Should decode output bitmap", decoded)

        // Scaled bounding box is 200x200 (from 100 to 300 in image coords)
        assertTrue(
            "Width (${decoded.width}) should be around 200, not 400",
            decoded.width in 190..210
        )
        assertTrue(
            "Height (${decoded.height}) should be around 200, not 400",
            decoded.height in 190..210
        )
        decoded.recycle()
    }
}
