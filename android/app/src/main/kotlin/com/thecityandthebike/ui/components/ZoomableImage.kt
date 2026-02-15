package com.thecityandthebike.ui.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thecityandthebike.ui.gestures.detectPinchGestures

@Composable
fun ZoomableImage(
    imageUri: Uri?,
    thumbnailUri: Uri?,
    contentDescription: String,
    overlay: @Composable () -> Unit = {},
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isZooming by remember { mutableStateOf(false) }

    LaunchedEffect(isZooming) {
        if (!isZooming && scale > 1f) {
            val startScale = scale
            val startOffsetX = offsetX
            val startOffsetY = offsetY
            Animatable(0f).animateTo(1f, spring()) {
                scale = startScale + (1f - startScale) * value
                offsetX = startOffsetX * (1f - value)
                offsetY = startOffsetY * (1f - value)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clipToBounds()
            .pointerInput(Unit) {
                detectPinchGestures(
                    onGestureStart = { isZooming = true },
                    onGestureEnd = { isZooming = false },
                    onGesture = { centroid, pan, zoom ->
                        val oldScale = scale
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        val effectiveZoom = scale / oldScale

                        offsetX = centroid.x * (1 - effectiveZoom) +
                            offsetX * effectiveZoom + pan.x
                        offsetY = centroid.y * (1 - effectiveZoom) +
                            offsetY * effectiveZoom + pan.y

                        if (scale <= 1f) {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                )
            }
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
                translationX = offsetX
                translationY = offsetY
            }
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (thumbnailUri != null) {
            AsyncImage(
                model = thumbnailUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri ?: thumbnailUri)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            error = ColorPainter(MaterialTheme.colorScheme.errorContainer)
        )

        overlay()
    }
}
