package com.thecityandthebike.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun MaskConfirmView(
    imageUri: Uri?,
    thumbnailUri: Uri?,
    ring: List<List<Float>>,
    maskWidth: Int,
    maskHeight: Int,
    modifier: Modifier = Modifier,
) {
    var displaySize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { displaySize = it }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri ?: thumbnailUri)
                .crossfade(true)
                .build(),
            contentDescription = "Photo to tag",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (ring.size >= 3 && displaySize.width > 0 && displaySize.height > 0) {
            val scaleX = displaySize.width.toFloat() / maskWidth
            val scaleY = displaySize.height.toFloat() / maskHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                val outlineColor = Color(0xFF00E676)
                val strokeWidth = 3f

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
}
