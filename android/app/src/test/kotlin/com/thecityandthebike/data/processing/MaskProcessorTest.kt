package com.thecityandthebike.data.processing

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MaskProcessorTest {

    private lateinit var processor: MaskProcessor

    @Before
    fun setup() {
        processor = MaskProcessor()
    }

    /** Helper: create a white filled rectangle on a black background. */
    private fun makeRectMask(
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): IntArray {
        val pixels = IntArray(width * height) { 0xFF000000.toInt() } // black opaque
        for (y in top until bottom) {
            for (x in left until right) {
                pixels[y * width + x] = 0xFFFFFFFF.toInt() // white opaque
            }
        }
        return pixels
    }

    /** Helper: create a filled circle (approximate) on a black background. */
    private fun makeCircleMask(
        width: Int,
        height: Int,
        cx: Int,
        cy: Int,
        radius: Int
    ): IntArray {
        val pixels = IntArray(width * height) { 0xFF000000.toInt() }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy <= radius * radius) {
                    pixels[y * width + x] = 0xFFFFFFFF.toInt()
                }
            }
        }
        return pixels
    }

    @Test
    fun `single blob returns valid ring with correct dimensions`() {
        val pixels = makeRectMask(200, 200, 50, 50, 150, 150)
        val result = processor.processPixels(pixels, 200, 200)

        assertEquals(200, result.width)
        assertEquals(200, result.height)
        assertTrue("Ring should have at least 4 points", result.ring.size >= 4)
    }

    @Test
    fun `ring is closed - first equals last`() {
        val pixels = makeRectMask(200, 200, 50, 50, 150, 150)
        val result = processor.processPixels(pixels, 200, 200)
        val ring = result.ring

        assertEquals(
            "Ring should be closed (first == last coordinate)",
            ring.first(),
            ring.last()
        )
    }

    @Test
    fun `multiple blobs keeps largest`() {
        val width = 200
        val height = 200
        val pixels = IntArray(width * height) { 0xFF000000.toInt() }

        // Small blob: 10x10 rectangle at top-left
        for (y in 10 until 20) {
            for (x in 10 until 20) {
                pixels[y * width + x] = 0xFFFFFFFF.toInt()
            }
        }

        // Large blob: 80x80 rectangle
        for (y in 60 until 140) {
            for (x in 60 until 140) {
                pixels[y * width + x] = 0xFFFFFFFF.toInt()
            }
        }

        val result = processor.processPixels(pixels, width, height)
        val ring = result.ring
        val xs = ring.map { it[0] }
        val ys = ring.map { it[1] }

        assertTrue("Ring x-coords should be in large blob region", xs.min() >= 50f)
        assertTrue("Ring y-coords should be in large blob region", ys.min() >= 50f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty mask throws IllegalArgumentException`() {
        val pixels = IntArray(200 * 200) { 0xFF000000.toInt() } // all black
        processor.processPixels(pixels, 200, 200)
    }

    @Test
    fun `ring coordinates are float pairs`() {
        val pixels = makeRectMask(200, 200, 50, 50, 150, 150)
        val result = processor.processPixels(pixels, 200, 200)

        for (coord in result.ring) {
            assertEquals("Each coordinate should have 2 elements", 2, coord.size)
        }
    }

    @Test
    fun `simplification reduces point count for circle`() {
        // A circle contour has many points; simplification should reduce them
        val pixels = makeCircleMask(200, 200, 100, 100, 50)
        val binary = processor.threshold(pixels, 200, 200)
        val contours = processor.traceContours(binary, 200, 200)

        assertTrue("Should find at least one contour", contours.isNotEmpty())
        val largest = contours.maxBy { processor.shoelaceArea(it) }
        val rawCount = largest.size

        val simplified = processor.douglasPeucker(largest, 2.0)

        assertTrue(
            "Simplification should reduce points: $rawCount raw vs ${simplified.size} simplified",
            simplified.size < rawCount
        )
    }

    @Test
    fun `donut shape produces valid external ring`() {
        val width = 200
        val height = 200
        val pixels = IntArray(width * height) { 0xFF000000.toInt() }

        // Outer circle radius 80, inner circle radius 40 (hole)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - 100
                val dy = y - 100
                val distSq = dx * dx + dy * dy
                if (distSq <= 80 * 80 && distSq > 40 * 40) {
                    pixels[y * width + x] = 0xFFFFFFFF.toInt()
                }
            }
        }

        val result = processor.processPixels(pixels, width, height)

        assertTrue("Donut should produce a ring with at least 4 points", result.ring.size >= 4)
        assertEquals("Ring should be closed", result.ring.first(), result.ring.last())
    }

    @Test
    fun `correct dimensions returned for non-square image`() {
        val pixels = makeRectMask(320, 240, 50, 50, 270, 190)
        val result = processor.processPixels(pixels, 320, 240)

        assertEquals(320, result.width)
        assertEquals(240, result.height)
    }

    @Test
    fun `perpendicular distance for collinear points is zero`() {
        val dist = processor.perpendicularDistance(
            Pair(5f, 5f),
            Pair(0f, 0f),
            Pair(10f, 10f)
        )
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun `perpendicular distance for offset point`() {
        val dist = processor.perpendicularDistance(
            Pair(0f, 1f),
            Pair(0f, 0f),
            Pair(10f, 0f)
        )
        assertEquals(1.0, dist, 0.001)
    }

    @Test
    fun `shoelace area of unit square`() {
        val square = listOf(
            Pair(0f, 0f),
            Pair(1f, 0f),
            Pair(1f, 1f),
            Pair(0f, 1f)
        )
        assertEquals(1.0, processor.shoelaceArea(square), 0.001)
    }
}
