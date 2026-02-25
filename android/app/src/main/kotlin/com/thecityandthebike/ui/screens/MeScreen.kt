package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thecityandthebike.ui.viewmodel.DeleteAccountState
import com.thecityandthebike.ui.viewmodel.MeViewModel
import com.thecityandthebike.ui.viewmodel.UserState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    state: UserState,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    onLoadMore: () -> Unit = {},
    onClearError: () -> Unit = {},
    deleteAccountState: DeleteAccountState = DeleteAccountState(),
    onConfirmDeleteAccount: () -> Unit = {},
    onClearDeleteError: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Me") },
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
        Box(modifier = Modifier.padding(paddingValues)) {
            MeContent(
                state = state,
                onImageClick = onImageClick,
                onLoadMore = onLoadMore,
                onClearError = onClearError,
                deleteAccountState = deleteAccountState,
                onConfirmDeleteAccount = onConfirmDeleteAccount,
                onClearDeleteError = onClearDeleteError
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    viewModel: MeViewModel,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit,
    deleteAccountState: DeleteAccountState = DeleteAccountState(),
    onConfirmDeleteAccount: () -> Unit = {},
    onClearDeleteError: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MeScreen(
        state = state,
        onBack = onBack,
        onImageClick = onImageClick,
        onLoadMore = { viewModel.loadMoreSubmissions() },
        onClearError = { viewModel.clearError() },
        deleteAccountState = deleteAccountState,
        onConfirmDeleteAccount = onConfirmDeleteAccount,
        onClearDeleteError = onClearDeleteError
    )
}
