package com.thecityandthebike.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import com.thecityandthebike.data.model.dto.TagResponse

@Composable
fun TagOutlineOverlay(
    tags: List<TagResponse>,
    modifier: Modifier = Modifier,
) {
    var displaySize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("tagOutlineOverlay")
            .onSizeChanged { displaySize = it }
    ) {
        if (displaySize.width <= 0 || displaySize.height <= 0) return@Canvas

        val outlineColor = Color(0xFF00E676)
        val strokeWidth = 3f

        for (tag in tags) {
            val ring = tag.ring ?: continue
            val ringWidth = tag.ringWidth ?: continue
            val ringHeight = tag.ringHeight ?: continue
            if (ring.size < 3 || ringWidth <= 0 || ringHeight <= 0) continue

            val scaleX = displaySize.width.toFloat() / ringWidth
            val scaleY = displaySize.height.toFloat() / ringHeight

            for (i in ring.indices) {
                val start = ring[i]
                val end = ring[(i + 1) % ring.size]
                drawLine(
                    color = outlineColor,
                    start = Offset(start[0] * scaleX, start[1] * scaleY),
                    end = Offset(end[0] * scaleX, end[1] * scaleY),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
