package com.thecityandthebike.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.thecityandthebike.ui.theme.ExtendedTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.thecityandthebike.data.model.dto.TagResponse

@Composable
fun TagOutlineOverlay(
    tags: List<TagResponse>,
    selectedTagId: String? = null,
    onTagTapped: (String?) -> Unit = {},
    isTagDeletable: (TagResponse) -> Boolean = { false },
    onDeleteTag: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var displaySize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("tagOutlineOverlay")
            .onSizeChanged { displaySize = it }
            .pointerInput(tags, displaySize) {
                detectTapGestures { offset ->
                    if (displaySize.width <= 0 || displaySize.height <= 0) return@detectTapGestures

                    val tappedTag = tags.firstOrNull { tag ->
                        val ring = tag.ring ?: return@firstOrNull false
                        val ringWidth = tag.ringWidth ?: return@firstOrNull false
                        val ringHeight = tag.ringHeight ?: return@firstOrNull false
                        if (ring.size < 3 || ringWidth <= 0 || ringHeight <= 0) return@firstOrNull false

                        val scaleX = displaySize.width.toFloat() / ringWidth
                        val scaleY = displaySize.height.toFloat() / ringHeight

                        val scaledRing = ring.map { listOf(it[0] * scaleX, it[1] * scaleY) }
                        isPointInPolygon(offset.x, offset.y, scaledRing)
                    }

                    if (tappedTag?.tagId == selectedTagId && selectedTagId != null) {
                        onTagTapped(null)
                    } else {
                        onTagTapped(tappedTag?.tagId)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (displaySize.width <= 0 || displaySize.height <= 0) return@Canvas

            val outlineColor = Color(0xFF00E676)
            val defaultStrokeWidth = 3f
            val selectedStrokeWidth = 8f

            for (tag in tags) {
                val ring = tag.ring ?: continue
                val ringWidth = tag.ringWidth ?: continue
                val ringHeight = tag.ringHeight ?: continue
                if (ring.size < 3 || ringWidth <= 0 || ringHeight <= 0) continue

                val scaleX = displaySize.width.toFloat() / ringWidth
                val scaleY = displaySize.height.toFloat() / ringHeight

                val strokeWidth = if (tag.tagId == selectedTagId) selectedStrokeWidth else defaultStrokeWidth

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

        if (selectedTagId != null && displaySize.width > 0 && displaySize.height > 0) {
            val selectedTag = tags.find { it.tagId == selectedTagId }
            if (selectedTag != null && isTagDeletable(selectedTag)) {
                val ring = selectedTag.ring
                val ringWidth = selectedTag.ringWidth
                val ringHeight = selectedTag.ringHeight
                if (ring != null && ringWidth != null && ringHeight != null &&
                    ring.size >= 3 && ringWidth > 0 && ringHeight > 0
                ) {
                    val scaleX = displaySize.width.toFloat() / ringWidth
                    val scaleY = displaySize.height.toFloat() / ringHeight
                    val maxX = ring.maxOf { it[0] } * scaleX
                    val minY = ring.minOf { it[1] } * scaleY

                    val density = LocalDensity.current
                    val buttonSizePx = with(density) { 21.dp.toPx() }

                    IconButton(
                        onClick = { onDeleteTag(selectedTag.tagId) },
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (maxX - buttonSizePx / 2).toInt(),
                                    (minY - buttonSizePx / 2).toInt()
                                )
                            }
                            .size(21.dp)
                            .background(
                                color = ExtendedTheme.colorScheme.destructiveAction,
                                shape = CircleShape
                            )
                            .testTag("deleteTagButton")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete tag",
                            tint = ExtendedTheme.colorScheme.onDestructiveAction,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun isPointInPolygon(x: Float, y: Float, polygon: List<List<Float>>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i][0]
        val yi = polygon[i][1]
        val xj = polygon[j][0]
        val yj = polygon[j][1]
        if (((yi > y) != (yj > y)) &&
            (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}
