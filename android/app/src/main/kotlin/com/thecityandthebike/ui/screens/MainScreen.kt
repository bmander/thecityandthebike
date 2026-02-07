package com.thecityandthebike.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thecityandthebike.BuildConfig
import com.thecityandthebike.ui.components.CameraFAB
import com.thecityandthebike.ui.components.ImageGrid
import com.thecityandthebike.ui.components.LoginFAB
import com.thecityandthebike.ui.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    onLoginClick: () -> Unit,
    onScanQrCode: () -> Unit,
    onImageClick: ((String) -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()

    // Combine server submissions with local images
    val serverImageUris = state.submissions.mapNotNull { submission ->
        submission.imageUrlOriginal?.let { url ->
            if (url.startsWith("http")) Uri.parse(url)
            else Uri.parse(BuildConfig.BASE_URL + url)
        }
    }
    val allImageUris = state.localImages + serverImageUris

    Box(modifier = Modifier.fillMaxSize()) {
        ImageGrid(
            imageUris = allImageUris,
            modifier = Modifier.fillMaxSize(),
            uploadingUris = state.localImages.toSet(),
            onImageClick = onImageClick?.let { callback ->
                { index ->
                    val localCount = state.localImages.size
                    if (index >= localCount) {
                        val submissionIndex = index - localCount
                        val submissionId = state.submissions[submissionIndex].submissionId
                        callback(submissionId)
                    }
                }
            }
        )

        // Logout button (top left) - only shown when logged in
        if (isLoggedIn) {
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onSurface
                )
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
