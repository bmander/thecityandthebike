package com.thecityandthebike.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thecityandthebike.ui.components.CameraFAB
import com.thecityandthebike.ui.components.ImageGrid
import com.thecityandthebike.ui.components.LoadingIndicator
import com.thecityandthebike.ui.components.LoginFAB
import com.thecityandthebike.ui.components.MenuButton
import com.thecityandthebike.ui.components.PointsAwardedOverlay
import com.thecityandthebike.ui.viewmodel.BikesListState
import com.thecityandthebike.ui.viewmodel.BikesListViewModel
import com.thecityandthebike.ui.viewmodel.LeaderboardPeriod
import com.thecityandthebike.ui.viewmodel.LeaderboardState
import com.thecityandthebike.ui.viewmodel.LeaderboardViewModel
import com.thecityandthebike.ui.viewmodel.MainState
import com.thecityandthebike.ui.viewmodel.MainViewModel
import com.thecityandthebike.util.imageUrlToUri
import kotlinx.coroutines.launch

private enum class MainTab(val title: String) {
    FEED("Feed"),
    LEADERBOARD("Leaderboard"),
    BIKES("Bikes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainState: MainState,
    leaderboardState: LeaderboardState,
    bikesListState: BikesListState,
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    onLoginClick: () -> Unit,
    onScanQrCode: () -> Unit,
    onShowAbout: () -> Unit = {},
    onShowScoreRules: () -> Unit = {},
    onShowPrivacyCopyright: () -> Unit = {},
    onImageClick: ((String) -> Unit)? = null,
    onUserClick: (String) -> Unit = {},
    onBikeClick: (String) -> Unit = {},
    onShowMe: () -> Unit = {},
    onRefreshSubmissions: () -> Unit = {},
    onLoadMoreSubmissions: () -> Unit = {},
    onRetryUpload: () -> Unit = {},
    onClearMainError: () -> Unit = {},
    onClearPointsAwarded: () -> Unit = {},
    onSelectPeriod: (LeaderboardPeriod) -> Unit = {},
    onClearLeaderboardError: () -> Unit = {},
    onLoadMoreBikes: () -> Unit = {},
    onClearBikesError: () -> Unit = {}
) {
    val tabs = MainTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Sync tab selection with pager
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { /* just observe for recomposition */ }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Top header: tab bar + hamburger menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.weight(1f)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(tab.title) }
                        )
                    }
                }

                Box {
                    var menuExpanded by remember { mutableStateOf(false) }

                    MenuButton(onClick = { menuExpanded = true })

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                menuExpanded = false
                                onShowAbout()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Score Rules") },
                            onClick = {
                                menuExpanded = false
                                onShowScoreRules()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Privacy & Copyright") },
                            onClick = {
                                menuExpanded = false
                                onShowPrivacyCopyright()
                            }
                        )
                        if (isLoggedIn) {
                            DropdownMenuItem(
                                text = { Text("Me") },
                                onClick = {
                                    menuExpanded = false
                                    onShowMe()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Log out") },
                                onClick = {
                                    menuExpanded = false
                                    onLogout()
                                }
                            )
                        }
                    }
                }
            }

            // Offline banner
            if (mainState.isOffline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "You're offline \u2014 uploads will resume when connected",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Swipeable content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    MainTab.FEED -> FeedContent(
                        state = mainState,
                        onImageClick = onImageClick,
                        onRefresh = onRefreshSubmissions,
                        onRetryUpload = onRetryUpload,
                        onLoadMore = onLoadMoreSubmissions,
                        onClearError = onClearMainError
                    )
                    MainTab.LEADERBOARD -> {
                        LeaderboardContent(
                            state = leaderboardState,
                            onPeriodSelected = onSelectPeriod,
                            onUserClick = onUserClick,
                            onClearError = onClearLeaderboardError
                        )
                    }
                    MainTab.BIKES -> {
                        BikesContent(
                            state = bikesListState,
                            onBikeClick = onBikeClick,
                            onLoadMore = onLoadMoreBikes,
                            onClearError = onClearBikesError
                        )
                    }
                }
            }
        }

        // Camera FAB (bottom center) when logged in, Login FAB when not logged in
        if (isLoggedIn) {
            CameraFAB(
                onClick = onScanQrCode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            )
        } else {
            LoginFAB(
                onClick = onLoginClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            )
        }

        // Points awarded overlay
        mainState.pointsAwarded?.let { breakdown ->
            PointsAwardedOverlay(
                breakdown = breakdown,
                onDismiss = onClearPointsAwarded,
                onShowScoreRules = onShowScoreRules
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    leaderboardViewModel: LeaderboardViewModel,
    bikesListViewModel: BikesListViewModel,
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    onLoginClick: () -> Unit,
    onScanQrCode: () -> Unit,
    onShowAbout: () -> Unit = {},
    onShowScoreRules: () -> Unit = {},
    onShowPrivacyCopyright: () -> Unit = {},
    onImageClick: ((String) -> Unit)? = null,
    onUserClick: (String) -> Unit = {},
    onBikeClick: (String) -> Unit = {},
    onShowMe: () -> Unit = {}
) {
    val mainState by viewModel.state.collectAsStateWithLifecycle()
    val leaderboardState by leaderboardViewModel.state.collectAsStateWithLifecycle()
    val bikesListState by bikesListViewModel.state.collectAsStateWithLifecycle()

    MainScreen(
        mainState = mainState,
        leaderboardState = leaderboardState,
        bikesListState = bikesListState,
        isLoggedIn = isLoggedIn,
        onLogout = onLogout,
        onLoginClick = onLoginClick,
        onScanQrCode = onScanQrCode,
        onShowAbout = onShowAbout,
        onShowScoreRules = onShowScoreRules,
        onShowPrivacyCopyright = onShowPrivacyCopyright,
        onImageClick = onImageClick,
        onUserClick = onUserClick,
        onBikeClick = onBikeClick,
        onShowMe = onShowMe,
        onRefreshSubmissions = { viewModel.refreshSubmissions() },
        onLoadMoreSubmissions = { viewModel.loadMoreSubmissions() },
        onRetryUpload = { viewModel.retryUpload() },
        onClearMainError = { viewModel.clearError() },
        onClearPointsAwarded = { viewModel.clearPointsAwarded() },
        onSelectPeriod = { leaderboardViewModel.selectPeriod(it) },
        onClearLeaderboardError = { leaderboardViewModel.clearError() },
        onLoadMoreBikes = { bikesListViewModel.loadMoreBikes() },
        onClearBikesError = { bikesListViewModel.clearError() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    state: MainState,
    onImageClick: ((String) -> Unit)?,
    onRefresh: () -> Unit,
    onRetryUpload: () -> Unit,
    onLoadMore: () -> Unit,
    onClearError: () -> Unit
) {
    val submissionsWithImages = state.submissions.filter { it.imageUrlThumbnail != null }
    val submissionImageUris = submissionsWithImages.map { submission ->
        imageUrlToUri(submission.imageUrlThumbnail!!)
    }
    val pendingUri = state.pendingUploadUri
    val imageUris = if (pendingUri != null) listOf(pendingUri) + submissionImageUris else submissionImageUris
    val uploadingUris = if (pendingUri != null && !state.uploadFailed) setOf(pendingUri) else emptySet()
    val failedUris = if (pendingUri != null && state.uploadFailed) setOf(pendingUri) else emptySet()
    val queuedUriSet = state.queuedUploadUris.toSet() - uploadingUris - failedUris
    val flaggedUris = submissionsWithImages
        .mapIndexedNotNull { index, submission ->
            if ((submission.flagCount ?: 0) > 0) submissionImageUris[index] else null
        }
        .toSet()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            ImageGrid(
                imageUris = imageUris,
                uploadingUris = uploadingUris,
                failedUris = failedUris,
                flaggedUris = flaggedUris,
                queuedUris = queuedUriSet,
                modifier = Modifier.fillMaxSize(),
                onImageClick = onImageClick?.let { callback ->
                    { index ->
                        val adjustedIndex = if (pendingUri != null) index - 1 else index
                        if (adjustedIndex >= 0) {
                            val submissionId = submissionsWithImages[adjustedIndex].submissionId
                            callback(submissionId)
                        }
                    }
                },
                onFailedImageClick = { onRetryUpload() },
                onLoadMore = onLoadMore
            )
        }

        // Loading indicator (initial data fetch only)
        if (state.isLoading) {
            LoadingIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Loading-more indicator (pagination)
        if (state.isLoadingMore) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .size(24.dp),
                strokeWidth = 2.dp
            )
        }

        // Error snackbar
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = if (state.uploadFailed) {
                    {
                        TextButton(onClick = onRetryUpload) {
                            Text("Retry")
                        }
                    }
                } else null,
                dismissAction = {
                    TextButton(onClick = onClearError) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}
