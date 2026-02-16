package com.thecityandthebike.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsPointInPolygonTest {

    // Simple square: (0,0), (10,0), (10,10), (0,10)
    private val square = listOf(
        listOf(0f, 0f),
        listOf(10f, 0f),
        listOf(10f, 10f),
        listOf(0f, 10f),
    )

    @Test
    fun `point inside square returns true`() {
        assertTrue(isPointInPolygon(5f, 5f, square))
    }

    @Test
    fun `point outside square returns false`() {
        assertFalse(isPointInPolygon(15f, 5f, square))
    }

    @Test
    fun `point above square returns false`() {
        assertFalse(isPointInPolygon(5f, -1f, square))
    }

    @Test
    fun `point below square returns false`() {
        assertFalse(isPointInPolygon(5f, 11f, square))
    }

    // Triangle: (0,0), (10,0), (5,10)
    private val triangle = listOf(
        listOf(0f, 0f),
        listOf(10f, 0f),
        listOf(5f, 10f),
    )

    @Test
    fun `point inside triangle returns true`() {
        assertTrue(isPointInPolygon(5f, 3f, triangle))
    }

    @Test
    fun `point outside triangle returns false`() {
        assertFalse(isPointInPolygon(1f, 9f, triangle))
    }

    // L-shaped concave polygon:
    //
    //   (0,0)---(6,0)
    //     |       |
    //     |  (3,3)---(6,3)
    //     |   |
    //   (0,6)-(3,6)
    //
    // Vertices in order trace the L shape.
    private val lShape = listOf(
        listOf(0f, 0f),
        listOf(6f, 0f),
        listOf(6f, 3f),
        listOf(3f, 3f),
        listOf(3f, 6f),
        listOf(0f, 6f),
    )

    @Test
    fun `point in upper arm of L returns true`() {
        assertTrue(isPointInPolygon(5f, 1f, lShape))
    }

    @Test
    fun `point in lower arm of L returns true`() {
        assertTrue(isPointInPolygon(1f, 5f, lShape))
    }

    @Test
    fun `point in convex hull but outside concave polygon returns false`() {
        // (5, 5) is inside the bounding box and convex hull of the L,
        // but falls in the concave cutout region
        assertFalse(isPointInPolygon(5f, 5f, lShape))
    }

    @Test
    fun `point far outside concave polygon returns false`() {
        assertFalse(isPointInPolygon(20f, 20f, lShape))
    }
}
