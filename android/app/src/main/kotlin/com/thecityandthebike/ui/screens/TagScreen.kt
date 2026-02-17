package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thecityandthebike.data.model.dto.TagDetailResponse
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.ui.components.CalendarEntry
import com.thecityandthebike.ui.components.CalendarPhoto
import com.thecityandthebike.ui.components.InfoRow
import com.thecityandthebike.ui.components.groupSubmissionsByDate
import com.thecityandthebike.ui.viewmodel.TagState
import com.thecityandthebike.ui.viewmodel.TagViewModel
import com.thecityandthebike.util.imageUrlToUri
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagScreen(
    viewModel: TagViewModel,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TagScreenContent(
        state = state,
        onBack = onBack,
        onImageClick = onImageClick,
        onUserClick = onUserClick,
        onLoadMore = { viewModel.loadMoreSubmissions() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagScreenContent(
    state: TagState,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tag") },
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
                    state.tagDetail?.let { detail ->
                        item {
                            AsyncImage(
                                model = imageUrlToUri(detail.imageUrl),
                                contentDescription = "Tag image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f),
                                contentScale = ContentScale.Fit
                            )
                        }

                        item {
                            Text(
                                text = "${detail.submissionCount} photo${if (detail.submissionCount != 1) "s" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        if (detail.firstCapturedBy != null) {
                            item {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    detail.firstCapturedBy.let { user ->
                                        InfoRow(
                                            icon = Icons.Default.Person,
                                            iconDescription = "First captured by",
                                            onClick = { onUserClick(user.id) }
                                        ) {
                                            val dateText = detail.firstCapturedAt?.let { date ->
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
