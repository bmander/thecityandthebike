package com.thecityandthebike.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.thecityandthebike.ui.gestures.detectPinchGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
    onBack: () -> Unit,
    onBikeClick: ((String) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null
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
    var isZooming by remember { mutableStateOf(false) }

    // Animate back to default zoom when pinch gesture ends
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
                // Thumbnail layer — loads fast from disk cache
                if (thumbnailUri != null) {
                    AsyncImage(
                        model = thumbnailUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Full-resolution layer — loads on top with crossfade
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUri ?: thumbnailUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Submission photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = ColorPainter(MaterialTheme.colorScheme.errorContainer)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                submission.username?.let { username ->
                    InfoRow(
                        icon = Icons.Default.Person,
                        iconDescription = "User",
                        onClick = onUserClick?.let { { it(submission.userId) } }
                    ) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }

                InfoRow(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    iconDescription = "Bike",
                    onClick = onBikeClick?.let { { it(submission.bikeQrId) } }
                ) {
                    BikeIdBadge(
                        provider = submission.provider,
                        bikeQrId = submission.bikeQrId
                    )
                }

                formattedDate?.let { date ->
                    InfoRow(
                        icon = Icons.Default.CalendarToday,
                        iconDescription = "Date"
                    ) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconDescription: String,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) {
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        } else {
            Modifier.fillMaxWidth()
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BikeIdBadge(provider: String?, bikeQrId: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (provider != null) {
            Text(
                text = provider.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Text(
            text = bikeQrId,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
