package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import com.thecityandthebike.ui.components.LoginFAB
import com.thecityandthebike.ui.components.MenuButton
import com.thecityandthebike.ui.components.PointsAwardedOverlay
import com.thecityandthebike.ui.viewmodel.BikesListViewModel
import com.thecityandthebike.ui.viewmodel.LeaderboardViewModel
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
    viewModel: MainViewModel,
    leaderboardViewModel: LeaderboardViewModel,
    bikesListViewModel: BikesListViewModel,
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    onLoginClick: () -> Unit,
    onScanQrCode: () -> Unit,
    onShowAbout: () -> Unit = {},
    onShowPrivacyCopyright: () -> Unit = {},
    onImageClick: ((String) -> Unit)? = null,
    onUserClick: (String) -> Unit = {},
    onBikeClick: (String) -> Unit = {},
    onShowMe: () -> Unit = {}
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

            // Swipeable content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (tabs[page]) {
                    MainTab.FEED -> FeedContent(
                        viewModel = viewModel,
                        onImageClick = onImageClick
                    )
                    MainTab.LEADERBOARD -> {
                        val leaderboardState by leaderboardViewModel.state.collectAsStateWithLifecycle()
                        LeaderboardContent(
                            state = leaderboardState,
                            onPeriodSelected = { leaderboardViewModel.selectPeriod(it) },
                            onUserClick = onUserClick,
                            onClearError = { leaderboardViewModel.clearError() }
                        )
                    }
                    MainTab.BIKES -> {
                        val bikesListState by bikesListViewModel.state.collectAsStateWithLifecycle()
                        BikesContent(
                            state = bikesListState,
                            onBikeClick = onBikeClick,
                            onLoadMore = { bikesListViewModel.loadMoreBikes() },
                            onClearError = { bikesListViewModel.clearError() }
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
        val mainState by viewModel.state.collectAsStateWithLifecycle()
        mainState.pointsAwarded?.let { points ->
            PointsAwardedOverlay(
                points = points,
                onDismiss = { viewModel.clearPointsAwarded() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    viewModel: MainViewModel,
    onImageClick: ((String) -> Unit)?
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val submissionsWithImages = state.submissions.filter { it.imageUrl != null }
    val submissionImageUris = submissionsWithImages.map { submission ->
        val url = submission.imageUrlThumbnail ?: submission.imageUrl!!
        imageUrlToUri(url)
    }
    val pendingUri = state.pendingUploadUri
    val imageUris = if (pendingUri != null) listOf(pendingUri) + submissionImageUris else submissionImageUris
    val uploadingUris = if (pendingUri != null) setOf(pendingUri) else emptySet()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refreshSubmissions() },
            modifier = Modifier.fillMaxSize()
        ) {
            ImageGrid(
                imageUris = imageUris,
                uploadingUris = uploadingUris,
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
                onLoadMore = { viewModel.loadMoreSubmissions() }
            )
        }

        // Loading indicator (initial data fetch only)
        if (state.isLoading) {
            CircularProgressIndicator(
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
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}
