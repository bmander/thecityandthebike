package com.thecityandthebike.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.util.imageUrlToUri
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(
    submission: SubmissionResponse,
    onBack: () -> Unit
) {
    val imageUri = submission.imageUrlOriginal?.let { imageUrlToUri(it) }
    val thumbnailUri = submission.imageUrlThumbnail?.let { imageUrlToUri(it) }

    val formattedDate = submission.capturedDate?.let {
        try {
            val localDate = LocalDate.parse(it)
            DateTimeFormatter.ofPattern("MMM d, yyyy")
                .format(localDate)
        } catch (_: Exception) {
            it
        }
    }

    // Pinch-to-zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    // Animate back to default zoom when gesture ends
    LaunchedEffect(transformableState.isTransformInProgress) {
        if (!transformableState.isTransformInProgress && scale > 1f) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri ?: thumbnailUri)
                    .placeholderMemoryCacheKey(thumbnailUri?.toString())
                    .crossfade(true)
                    .build(),
                contentDescription = "Submission photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clipToBounds()
                    .transformable(state = transformableState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                error = ColorPainter(MaterialTheme.colorScheme.errorContainer)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                submission.username?.let { username ->
                    Text(
                        text = username,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = "Bike: ${submission.bikeQrId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                formattedDate?.let { date ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
