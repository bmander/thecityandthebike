package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.ScoringBreakdown
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.upload.UploadManager
import com.thecityandthebike.upload.UploadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val submissions: List<SubmissionResponse> = emptyList(),
    val error: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val nextCursor: String? = null,
    val pendingUploadUri: Uri? = null,
    val uploadFailed: Boolean = false,
    val pointsAwarded: List<ScoringBreakdown>? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val uploadManager: UploadManager
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        loadSubmissions()
        observeUploadState()
    }

    private fun observeUploadState() {
        viewModelScope.launch {
            uploadManager.state.collect { uploadState ->
                when (uploadState) {
                    is UploadState.Uploading -> {
                        _state.value = _state.value.copy(
                            pendingUploadUri = uploadState.localUri,
                            uploadFailed = false
                        )
                    }
                    is UploadState.Success -> {
                        _state.value = _state.value.copy(
                            pendingUploadUri = null,
                            uploadFailed = false,
                            pointsAwarded = uploadState.pointsBreakdown.ifEmpty { null }
                        )
                        fetchSubmissions(isRefresh = true)
                    }
                    is UploadState.Error -> {
                        _state.value = _state.value.copy(
                            pendingUploadUri = uploadState.localUri,
                            uploadFailed = true,
                            error = uploadState.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadSubmissions() {
        fetchSubmissions(isRefresh = false)
    }

    fun refreshSubmissions() {
        if (_state.value.isRefreshing) return
        fetchSubmissions(isRefresh = true)
    }

    private fun fetchSubmissions(isRefresh: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = !isRefresh,
                isRefreshing = isRefresh,
                error = null
            )
            when (val result = submissionRepository.getSubmissions()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        submissions = result.data.items,
                        hasMorePages = result.data.hasMore,
                        nextCursor = result.data.nextCursor
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.error.displayMessage
                    )
                }
            }
        }
    }

    fun loadMoreSubmissions() {
        val currentState = _state.value
        if (currentState.isLoadingMore || !currentState.hasMorePages) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            when (val result = submissionRepository.getSubmissions(cursor = _state.value.nextCursor)) {
                is ApiResult.Success -> {
                    val newSubmissions = _state.value.submissions + result.data.items
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        submissions = newSubmissions,
                        hasMorePages = result.data.hasMore,
                        nextCursor = result.data.nextCursor
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        error = result.error.displayMessage
                    )
                }
            }
        }
    }

    fun removeSubmission(submissionId: String) {
        _state.value = _state.value.copy(
            submissions = _state.value.submissions.filter { it.submissionId != submissionId }
        )
    }

    fun retryUpload() {
        uploadManager.retryUpload()
    }

    fun clearError() {
        if (_state.value.uploadFailed) {
            _state.value = _state.value.copy(
                error = null,
                pendingUploadUri = null,
                uploadFailed = false
            )
            uploadManager.clearError()
        } else {
            _state.value = _state.value.copy(error = null)
        }
    }

    fun clearPointsAwarded() {
        _state.value = _state.value.copy(pointsAwarded = null)
    }
}
