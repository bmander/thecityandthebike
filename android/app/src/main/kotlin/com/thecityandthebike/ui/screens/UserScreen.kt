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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thecityandthebike.ui.components.CalendarEntry
import com.thecityandthebike.ui.components.CalendarPhoto
import com.thecityandthebike.ui.components.groupSubmissionsByDate
import com.thecityandthebike.ui.viewmodel.UserState
import com.thecityandthebike.ui.viewmodel.UserViewModel
import com.thecityandthebike.util.imageUrlToUri
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    viewModel: UserViewModel,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    UserScreenContent(
        state = state,
        onBack = onBack,
        onImageClick = onImageClick,
        onLoadMore = { viewModel.loadMoreSubmissions() },
        onBan = { viewModel.banUser() },
        onUnban = { viewModel.unbanUser() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UserScreenContent(
    state: UserState,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onLoadMore: () -> Unit = {},
    onBan: () -> Unit = {},
    onUnban: () -> Unit = {}
) {
    val title = state.userDetail?.username ?: "User"
    var pendingBanAction by remember { mutableStateOf<BanAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    state.userDetail?.let { detail ->
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${detail.submissionCount} photo${if (detail.submissionCount != 1) "s" else ""}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${detail.ownedBikeCount} bike${if (detail.ownedBikeCount != 1) "s" else ""} owned",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val ranksText = detail.leaderboardRanks.joinToString(" | ") { rank ->
                                    val label = rank.period.replaceFirstChar { it.uppercase() }
                                    val value = rank.rank?.let { "#$it" } ?: "--"
                                    "$label: $value"
                                }
                                Text(
                                    text = ranksText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                if (state.currentUserIsAdmin && !detail.isAdmin) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (detail.isBanned) {
                                        BanActionButton(
                                            label = "Unban User",
                                            isLoading = state.isBanning,
                                            color = MaterialTheme.colorScheme.primary,
                                            onClick = { pendingBanAction = BanAction.UNBAN }
                                        )
                                    } else {
                                        BanActionButton(
                                            label = "Ban User",
                                            isLoading = state.isBanning,
                                            color = MaterialTheme.colorScheme.error,
                                            onClick = { pendingBanAction = BanAction.BAN }
                                        )
                                    }
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

        pendingBanAction?.let { action ->
            val username = state.userDetail?.username
            val isBan = action == BanAction.BAN
            AlertDialog(
                onDismissRequest = { pendingBanAction = null },
                title = { Text(if (isBan) "Ban User" else "Unban User") },
                text = {
                    Text(
                        if (isBan) "Are you sure you want to ban $username? They will no longer be able to post content or log in."
                        else "Are you sure you want to unban $username? They will be able to post content and log in again."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingBanAction = null
                            if (isBan) onBan() else onUnban()
                        }
                    ) {
                        Text(
                            text = if (isBan) "Ban" else "Unban",
                            color = if (isBan) MaterialTheme.colorScheme.error else Color.Unspecified
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingBanAction = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private enum class BanAction { BAN, UNBAN }

@Composable
private fun BanActionButton(
    label: String,
    isLoading: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.height(16.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(label)
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
