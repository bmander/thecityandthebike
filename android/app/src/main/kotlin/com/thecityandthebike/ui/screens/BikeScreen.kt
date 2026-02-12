package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.Person
import com.thecityandthebike.data.model.dto.BikeDetailResponse
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.ui.components.BikeIdBadge
import com.thecityandthebike.ui.components.CalendarEntry
import com.thecityandthebike.ui.components.CalendarPhoto
import com.thecityandthebike.ui.components.InfoRow
import com.thecityandthebike.ui.viewmodel.BikeState
import com.thecityandthebike.ui.viewmodel.BikeViewModel
import com.thecityandthebike.util.imageUrlToUri
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DateGroupPhoto(
    val submissionId: String,
    val imageUrl: String
)

data class DateGroup(
    val dateLabel: String,
    val yearLabel: String?,
    val photos: List<DateGroupPhoto>
)

fun groupSubmissionsByDate(submissions: List<SubmissionResponse>): List<DateGroup> {
    val dayFormat = DateTimeFormatter.ofPattern("MMM d")
    val yearFormat = DateTimeFormatter.ofPattern("yyyy")
    return submissions
        .groupBy { submission ->
            submission.capturedDate?.let { dateStr ->
                try {
                    LocalDate.parse(dateStr)
                } catch (_: Exception) {
                    null
                }
            }
        }
        .entries
        .sortedByDescending { it.key }
        .map { (date, submissions) ->
            val photos = submissions.mapNotNull { submission ->
                val url = submission.imageUrlThumbnail ?: submission.imageUrl
                    ?: return@mapNotNull null
                DateGroupPhoto(
                    submissionId = submission.submissionId,
                    imageUrl = url
                )
            }
            DateGroup(
                dateLabel = date?.format(dayFormat) ?: "Unknown date",
                yearLabel = date?.format(yearFormat),
                photos = photos
            )
        }
        .filter { it.photos.isNotEmpty() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeScreen(
    viewModel: BikeViewModel,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val bikeQrId = state.bikeDetail?.bikeQrId ?: viewModel.bikeQrId

    BikeScreenContent(
        state = state,
        bikeQrId = bikeQrId,
        onBack = onBack,
        onImageClick = onImageClick,
        onUserClick = onUserClick,
        onLoadMore = { viewModel.loadMoreSubmissions() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BikeScreenContent(
    state: BikeState,
    bikeQrId: String,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    val provider = state.bikeDetail?.provider

    Scaffold(
        topBar = {
            TopAppBar(
                title = { BikeIdBadge(provider = provider, bikeQrId = bikeQrId) },
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
                    state.bikeDetail?.let { detail ->
                        item {
                            Text(
                                text = "${detail.submissionCount} photo${if (detail.submissionCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        if (detail.owners.isNotEmpty() || detail.firstCapturedBy != null) {
                            item {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    detail.owners.forEach { owner ->
                                        InfoRow(
                                            icon = Icons.Default.Person,
                                            iconDescription = "Owner",
                                            onClick = { onUserClick(owner.user.id) }
                                        ) {
                                            Text(
                                                text = "${owner.user.name} (${owner.submissionCount} photo${if (owner.submissionCount != 1) "s" else ""})",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    detail.firstCapturedBy?.let { user ->
                                        InfoRow(
                                            icon = Icons.Default.Person,
                                            iconDescription = "First captured by",
                                            onClick = { onUserClick(user.id) }
                                        ) {
                                            val dateText = detail.firstSeenAt?.let { date ->
                                                try {
                                                    val parsed = LocalDate.parse(date.substringBefore("T"))
                                                    " on ${parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
                                                } catch (_: Exception) { "" }
                                            } ?: ""
                                            Text(
                                                text = "First captured by ${user.name}$dateText",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
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
    }
}
