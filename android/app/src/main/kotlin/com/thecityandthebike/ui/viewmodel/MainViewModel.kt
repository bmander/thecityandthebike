package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.model.ApiResult
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
    val totalSubmissions: Int = 0
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
                    is UploadState.Success -> refreshSubmissions()
                    is UploadState.Error -> {
                        _state.value = _state.value.copy(error = uploadState.message)
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
                        totalSubmissions = result.data.total,
                        hasMorePages = result.data.items.size + result.data.offset < result.data.total
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
            val offset = _state.value.submissions.size
            when (val result = submissionRepository.getSubmissions(offset = offset)) {
                is ApiResult.Success -> {
                    val newSubmissions = _state.value.submissions + result.data.items
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        submissions = newSubmissions,
                        totalSubmissions = result.data.total,
                        hasMorePages = newSubmissions.size < result.data.total
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
