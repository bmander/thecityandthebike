package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thecityandthebike.ui.components.CameraFAB
import com.thecityandthebike.ui.components.ImageGrid
import com.thecityandthebike.ui.components.LoginFAB
import com.thecityandthebike.ui.components.MenuButton
import com.thecityandthebike.ui.viewmodel.MainViewModel
import com.thecityandthebike.util.imageUrlToUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    onLoginClick: () -> Unit,
    onScanQrCode: () -> Unit,
    onShowPrivacyCopyright: () -> Unit = {},
    onImageClick: ((String) -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Combine server submissions with local images
    val submissionsWithImages = state.submissions.filter { it.imageUrlOriginal != null }
    val serverImageUris = submissionsWithImages.map { submission ->
        val url = submission.imageUrlThumbnail ?: submission.imageUrlOriginal!!
        imageUrlToUri(url)
    }
    val allImageUris = state.localImages + serverImageUris

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refreshSubmissions() },
        modifier = Modifier.fillMaxSize()
    ) {
        ImageGrid(
            imageUris = allImageUris,
            modifier = Modifier.fillMaxSize(),
            uploadingUris = state.localImages.toSet(),
            onImageClick = onImageClick?.let { callback ->
                { index ->
                    val localCount = state.localImages.size
                    if (index >= localCount) {
                        val submissionIndex = index - localCount
                        val submissionId = submissionsWithImages[submissionIndex].submissionId
                        callback(submissionId)
                    }
                }
            },
            onLoadMore = { viewModel.loadMoreSubmissions() }
        )

        // Hamburger menu (top left) - always visible
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            var menuExpanded by remember { mutableStateOf(false) }

            MenuButton(onClick = { menuExpanded = true })

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Privacy & Copyright") },
                    onClick = {
                        menuExpanded = false
                        onShowPrivacyCopyright()
                    }
                )
                if (isLoggedIn) {
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

        // Camera FAB (top right) when logged in, Login FAB when not logged in
        if (isLoggedIn) {
            CameraFAB(
                onClick = onScanQrCode,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        } else {
            LoginFAB(
                onClick = onLoginClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
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
