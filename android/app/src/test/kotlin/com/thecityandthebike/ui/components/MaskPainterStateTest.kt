package com.thecityandthebike.ui.components

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MaskPainterStateTest {

    private lateinit var state: MaskPainterState

    @Before
    fun setUp() {
        state = MaskPainterState()
    }

    // --- hasStrokes ---

    @Test
    fun `hasStrokes is false when no strokes added`() {
        assertFalse(state.hasStrokes)
    }

    @Test
    fun `hasStrokes is true after adding a stroke`() {
        state.strokes.add(StrokePath(listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f))))
        assertTrue(state.hasStrokes)
    }

    @Test
    fun `hasStrokes ignores currentStroke`() {
        state.currentStroke = listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f))
        assertFalse(state.hasStrokes)
    }

    // --- allStrokes ---

    @Test
    fun `allStrokes is empty when no strokes and no currentStroke`() {
        assertTrue(state.allStrokes.isEmpty())
    }

    @Test
    fun `allStrokes returns committed strokes when no currentStroke`() {
        val stroke = StrokePath(listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f)))
        state.strokes.add(stroke)

        assertEquals(listOf(stroke), state.allStrokes)
    }

    @Test
    fun `allStrokes includes currentStroke when it has two or more points`() {
        val committed = StrokePath(listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f)))
        state.strokes.add(committed)
        state.currentStroke = listOf(StrokePoint(20f, 20f), StrokePoint(30f, 30f))

        assertEquals(2, state.allStrokes.size)
        assertEquals(committed, state.allStrokes[0])
        assertEquals(StrokePath(state.currentStroke), state.allStrokes[1])
    }

    @Test
    fun `allStrokes excludes currentStroke when it has fewer than two points`() {
        val committed = StrokePath(listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f)))
        state.strokes.add(committed)
        state.currentStroke = listOf(StrokePoint(20f, 20f))

        assertEquals(listOf(committed), state.allStrokes)
    }

    @Test
    fun `allStrokes excludes empty currentStroke`() {
        val committed = StrokePath(listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f)))
        state.strokes.add(committed)
        state.currentStroke = emptyList()

        assertEquals(listOf(committed), state.allStrokes)
    }

    // --- clear ---

    @Test
    fun `clear removes all committed strokes`() {
        state.strokes.add(StrokePath(listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f))))
        state.strokes.add(StrokePath(listOf(StrokePoint(20f, 20f), StrokePoint(30f, 30f))))

        state.clear()

        assertTrue(state.strokes.isEmpty())
    }

    @Test
    fun `clear resets currentStroke`() {
        state.currentStroke = listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f))

        state.clear()

        assertTrue(state.currentStroke.isEmpty())
    }

    @Test
    fun `clear results in empty allStrokes`() {
        state.strokes.add(StrokePath(listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f))))
        state.currentStroke = listOf(StrokePoint(20f, 20f), StrokePoint(30f, 30f))

        state.clear()

        assertTrue(state.allStrokes.isEmpty())
        assertFalse(state.hasStrokes)
    }

    // --- canvasSize ---

    @Test
    fun `canvasSize defaults to zero`() {
        assertEquals(IntSize.Zero, state.canvasSize)
    }

    @Test
    fun `canvasSize can be updated`() {
        state.canvasSize = IntSize(500, 500)
        assertEquals(IntSize(500, 500), state.canvasSize)
    }
}
