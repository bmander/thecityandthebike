package com.thecityandthebike.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.ui.components.MaskPainter
import com.thecityandthebike.ui.components.OwnerActions
import com.thecityandthebike.ui.components.SubmissionInfo
import com.thecityandthebike.ui.components.TagList
import com.thecityandthebike.ui.components.TagModeControls
import com.thecityandthebike.ui.components.ZoomableImage
import com.thecityandthebike.ui.components.rememberMaskPainterState
import com.thecityandthebike.util.imageUrlToUri
import androidx.compose.ui.tooling.preview.Preview
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(
    submission: SubmissionResponse,
    onHome: () -> Unit,
    onBikeClick: ((String) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null,
    isOwner: Boolean = false,
    isDeleting: Boolean = false,
    onDelete: () -> Unit = {},
    onDownload: () -> Unit = {},
    isLoggedIn: Boolean = false,
    tags: List<TagResponse> = emptyList(),
    isTagMode: Boolean = false,
    isCreatingTag: Boolean = false,
    onEnterTagMode: () -> Unit = {},
    onExitTagMode: () -> Unit = {},
    onCreateTag: (File) -> Unit = {},
    onDeleteTag: (String) -> Unit = {},
    isTagOwner: (TagResponse) -> Boolean = { false },
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
                    IconButton(onClick = onHome) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = "Home"
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
            if (isTagMode) {
                val maskState = rememberMaskPainterState()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                MaskPainter(
                    imageUri = imageUri,
                    thumbnailUri = thumbnailUri,
                    state = maskState
                )

                TagModeControls(
                    hasStrokes = maskState.hasStrokes,
                    isCreatingTag = isCreatingTag,
                    onDiscard = onExitTagMode,
                    onSave = {
                        imageUri?.let { uri ->
                            scope.launch {
                                maskState.exportComposited(context, uri)
                                    ?.let { onCreateTag(it) }
                            }
                        }
                    }
                )
            } else {
                ZoomableImage(
                    imageUri = imageUri,
                    thumbnailUri = thumbnailUri,
                    contentDescription = "Submission photo"
                )
            }

            if (isOwner) {
                OwnerActions(
                    isDeleting = isDeleting,
                    onDownload = onDownload,
                    onShowDeleteDialog = { showDeleteDialog = true }
                )
            }

            if (isLoggedIn && !isTagMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    OutlinedIconButton(
                        onClick = onEnterTagMode,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = "Add tag"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SubmissionInfo(
                submission = submission,
                formattedDate = formattedDate,
                onUserClick = onUserClick,
                onBikeClick = onBikeClick
            )

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                TagList(
                    tags = tags,
                    isTagOwner = isTagOwner,
                    onDeleteTag = onDeleteTag
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ImageDetailScreenPreview() {
    TheCityAndTheBikeTheme(dynamicColor = false) {
        ImageDetailScreen(
            submission = SubmissionResponse(
                submissionId = "preview-1",
                userId = "user-1",
                bikeQrId = "BIKE-42",
                capturedDate = "2026-02-11",
                username = "bmander",
                provider = "citibike"
            ),
            onHome = {},
            isOwner = true
        )
    }
}
