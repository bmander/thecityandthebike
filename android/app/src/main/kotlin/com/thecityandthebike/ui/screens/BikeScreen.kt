package com.thecityandthebike.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thecityandthebike.ui.components.ImageGrid
import com.thecityandthebike.ui.viewmodel.BikeViewModel
import com.thecityandthebike.util.imageUrlToUri
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeScreen(
    viewModel: BikeViewModel,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    val bikeLabel = state.bikeDetail?.let { detail ->
        if (detail.provider != null) {
            "${detail.provider.replaceFirstChar { it.uppercase() }}: ${detail.bikeQrId}"
        } else {
            "Bike: ${detail.bikeQrId}"
        }
    } ?: viewModel.bikeQrId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bikeLabel) },
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
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    state.bikeDetail?.let { detail ->
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "${detail.submissionCount} photo${if (detail.submissionCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            detail.firstSeenAt?.let { firstSeen ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "First seen: ${formatDateTime(firstSeen)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            detail.lastSeenAt?.let { lastSeen ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Last seen: ${formatDateTime(lastSeen)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val imageUris = state.submissions.mapNotNull { submission ->
                        (submission.imageUrlThumbnail ?: submission.imageUrlOriginal)
                            ?.let { imageUrlToUri(it) }
                    }

                    ImageGrid(
                        imageUris = imageUris,
                        modifier = Modifier.fillMaxSize(),
                        onImageClick = { index ->
                            state.submissions.getOrNull(index)?.submissionId?.let { id ->
                                onImageClick(id)
                            }
                        },
                        onLoadMore = { viewModel.loadMoreSubmissions() }
                    )
                }
            }
        }
    }
}

private fun formatDateTime(isoString: String): String {
    return try {
        val zonedDateTime = ZonedDateTime.parse(isoString)
        DateTimeFormatter.ofPattern("MMM d, yyyy").format(zonedDateTime)
    } catch (_: Exception) {
        isoString
    }
}
