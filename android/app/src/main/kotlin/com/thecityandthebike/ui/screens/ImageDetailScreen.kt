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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.thecityandthebike.R
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.TagResponse
import android.graphics.BitmapFactory
import android.widget.Toast
import com.thecityandthebike.ui.components.MaskConfirmView
import com.thecityandthebike.ui.components.MaskPainter
import com.thecityandthebike.ui.components.OwnerActions
import com.thecityandthebike.ui.components.SubmissionInfo
import com.thecityandthebike.ui.components.TagOutlineOverlay
import com.thecityandthebike.ui.components.exportCompositedFromRing
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
    selectedTagId: String? = null,
    onTagTapped: (String?) -> Unit = {},
    isTagDeletable: (TagResponse) -> Boolean = { false },
    onDeleteTag: (String) -> Unit = {},
    isTagMode: Boolean = false,
    isCreatingTag: Boolean = false,
    onEnterTagMode: () -> Unit = {},
    onExitTagMode: () -> Unit = {},
    onCreateTag: (File) -> Unit = {},
    isProcessingMask: Boolean = false,
    processedRing: List<List<Float>>? = null,
    processedMaskWidth: Int = 0,
    processedMaskHeight: Int = 0,
    onProcessMask: (File) -> Unit = {},
    onConfirmTag: (File) -> Unit = {},
    onBackToDrawing: () -> Unit = {},
    onTagClick: ((String) -> Unit)? = null,
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
    var showDeleteTagDialog by remember { mutableStateOf<String?>(null) }

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

    if (showDeleteTagDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteTagDialog = null },
            title = { Text("Delete Tag") },
            text = { Text("Are you sure you want to delete this tag?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTag(showDeleteTagDialog!!)
                        showDeleteTagDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTagDialog = null }) {
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
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = "Home",
                            modifier = Modifier.size(40.dp),
                            colorFilter = if (isSystemInDarkTheme()) {
                                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                                    -1f, 0f, 0f, 0f, 255f,
                                    0f, -1f, 0f, 0f, 255f,
                                    0f, 0f, -1f, 0f, 255f,
                                    0f, 0f, 0f, 1f, 0f
                                )))
                            } else {
                                null
                            }
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
            if (isTagMode && processedRing != null) {
                // Confirming phase: show image with polygon outline overlay
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                MaskConfirmView(
                    imageUri = imageUri,
                    thumbnailUri = thumbnailUri,
                    ring = processedRing,
                    maskWidth = processedMaskWidth,
                    maskHeight = processedMaskHeight
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = onBackToDrawing) {
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            imageUri?.let { uri ->
                                scope.launch {
                                    exportCompositedFromRing(
                                        context, uri, processedRing,
                                        processedMaskWidth, processedMaskHeight
                                    )?.let { onConfirmTag(it) }
                                }
                            }
                        },
                        enabled = !isCreatingTag
                    ) {
                        if (isCreatingTag) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Confirm tag")
                        }
                    }
                }
            } else if (isTagMode) {
                // Drawing phase: mask painter + Discard/Next buttons
                val maskState = rememberMaskPainterState()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                // Obtain original image dimensions for mask export
                var imageDimensions by remember { mutableStateOf<Pair<Int, Int>?>(null) }
                LaunchedEffect(imageUri) {
                    imageUri?.let { uri ->
                        imageDimensions = withContext(Dispatchers.IO) {
                            try {
                                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                val stream = if (uri.scheme == "http" || uri.scheme == "https") {
                                    java.net.URL(uri.toString()).openStream()
                                } else {
                                    context.contentResolver.openInputStream(uri)
                                }
                                stream?.use { BitmapFactory.decodeStream(it, null, options) }
                                if (options.outWidth > 0 && options.outHeight > 0) {
                                    options.outWidth to options.outHeight
                                } else {
                                    null
                                }
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }

                MaskPainter(
                    imageUri = imageUri,
                    thumbnailUri = thumbnailUri,
                    state = maskState
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = onExitTagMode) {
                        Text("Discard")
                    }
                    Button(
                        onClick = {
                            val dims = imageDimensions
                            if (dims != null) {
                                scope.launch {
                                    maskState.exportMask(context, dims.first, dims.second)
                                        ?.let { onProcessMask(it) }
                                }
                            }
                        },
                        enabled = maskState.hasStrokes && !isProcessingMask && imageDimensions != null
                    ) {
                        if (isProcessingMask) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Next")
                        }
                    }
                }
            } else {
                ZoomableImage(
                    imageUri = imageUri,
                    thumbnailUri = thumbnailUri,
                    contentDescription = "Submission photo",
                    overlay = {
                        TagOutlineOverlay(
                            tags = tags,
                            selectedTagId = selectedTagId,
                            onTagTapped = onTagTapped,
                            isTagDeletable = isTagDeletable,
                            onDeleteTag = { tagId -> showDeleteTagDialog = tagId },
                            onTagClick = onTagClick
                        )
                    }
                )
            }

            if (isOwner || (isLoggedIn && !isTagMode)) {
                val context = LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoggedIn && !isTagMode) {
                        OutlinedIconButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Draw on the picture to highlight tags",
                                    Toast.LENGTH_LONG
                                ).show()
                                onEnterTagMode()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = "Add tags"
                            )
                        }
                        Text(
                            text = "Add tags",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (isOwner) {
                        OwnerActions(
                            isDeleting = isDeleting,
                            onDownload = onDownload,
                            onShowDeleteDialog = { showDeleteDialog = true }
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
            isOwner = true,
            isLoggedIn = true
        )
    }
}
