package com.thecityandthebike.ui.gestures

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Detects pinch (two-finger) gestures, reporting zoom, pan, and centroid.
 *
 * Similar to [androidx.compose.foundation.gestures.detectTransformGestures]
 * but only fires for multi-touch and provides explicit start/end callbacks.
 * Single-finger events pass through unconsumed so parent scroll still works.
 */
suspend fun PointerInputScope.detectPinchGestures(
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var hadMultiTouch = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } >= 2) {
                if (!hadMultiTouch) {
                    hadMultiTouch = true
                    onGestureStart()
                }
                onGesture(
                    event.calculateCentroid(useCurrent = false),
                    event.calculatePan(),
                    event.calculateZoom()
                )
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })

        if (hadMultiTouch) {
            onGestureEnd()
        }
    }
}
