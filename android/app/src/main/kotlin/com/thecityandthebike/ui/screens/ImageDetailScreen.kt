package com.thecityandthebike.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.ui.theme.ExtendedTheme
import com.thecityandthebike.ui.components.BikeIdBadge
import com.thecityandthebike.ui.components.InfoRow
import com.thecityandthebike.ui.components.ZoomableImage
import com.thecityandthebike.util.imageUrlToUri
import androidx.compose.ui.tooling.preview.Preview
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(
    submission: SubmissionResponse,
    onBack: () -> Unit,
    onBikeClick: ((String) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null,
    isOwner: Boolean = false,
    isDeleting: Boolean = false,
    onDelete: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
    val imageUri = submission.imageUrl?.let { imageUrlToUri(it) }
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

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Photo") },
            text = { Text("Are you sure you want to delete this photo? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
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
            ZoomableImage(
                imageUri = imageUri,
                thumbnailUri = thumbnailUri,
                contentDescription = "Submission photo"
            )

            if (isOwner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, end = 16.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Row {
                            OutlinedIconButton(
                                onClick = onDownload,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download photo"
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = { showDeleteDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = ExtendedTheme.colorScheme.destructiveAction,
                                    contentColor = ExtendedTheme.colorScheme.onDestructiveAction
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete photo"
                                )
                            }
                        }
                    }
                }
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
                            style = MaterialTheme.typography.titleMedium
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
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImageDetailScreenPreview() {
    TheCityAndTheBikeTheme {
        ImageDetailScreen(
            submission = SubmissionResponse(
                submissionId = "preview-1",
                userId = "user-1",
                bikeQrId = "BIKE-42",
                capturedDate = "2026-02-11",
                username = "bmander",
                provider = "citibike"
            ),
            onBack = {},
            isOwner = true
        )
    }
}
