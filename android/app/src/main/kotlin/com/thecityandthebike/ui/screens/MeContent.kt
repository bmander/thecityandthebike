package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thecityandthebike.ui.components.CalendarEntry
import com.thecityandthebike.ui.components.CalendarPhoto
import com.thecityandthebike.ui.components.groupSubmissionsByDate
import com.thecityandthebike.ui.viewmodel.DeleteAccountState
import com.thecityandthebike.ui.viewmodel.UserState
import com.thecityandthebike.util.imageUrlToUri
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MeContent(
    state: UserState,
    onImageClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onClearError: () -> Unit,
    deleteAccountState: DeleteAccountState = DeleteAccountState(),
    onConfirmDeleteAccount: () -> Unit = {},
    onClearDeleteError: () -> Unit = {}
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Are you sure?") },
            text = { Text("This will permanently delete your account and all your data. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onConfirmDeleteAccount()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.error != null || deleteAccountState.error != null -> {
                val errorMessage = deleteAccountState.error ?: state.error ?: ""
                val clearAction = if (deleteAccountState.error != null) onClearDeleteError else onClearError
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = clearAction) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
            else -> {
                val dateGroups = remember(state.submissions) {
                    groupSubmissionsByDate(state.submissions)
                }

                val listState = rememberLazyListState()

                val shouldLoadMore = remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItems = layoutInfo.totalItemsCount
                        totalItems > 0 && lastVisibleIndex >= totalItems - 3
                    }
                }

                LaunchedEffect(shouldLoadMore.value) {
                    if (shouldLoadMore.value) {
                        onLoadMore()
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    state.userDetail?.let { detail ->
                        item {
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
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                enabled = !deleteAccountState.isDeleting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth(1f / 3f)
                            ) {
                                if (deleteAccountState.isDeleting) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onError,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Delete My Account")
                                }
                            }
                        }
                    }

                    items(dateGroups, key = { it.dateLabel }) { group ->
                        CalendarEntry(
                            dateLabel = group.dateLabel,
                            yearLabel = group.yearLabel,
                            photos = group.photos.map { photo ->
                                CalendarPhoto(
                                    submissionId = photo.submissionId,
                                    imageUri = imageUrlToUri(photo.imageUrl)
                                )
                            },
                            onImageClick = onImageClick
                        )
                    }
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
