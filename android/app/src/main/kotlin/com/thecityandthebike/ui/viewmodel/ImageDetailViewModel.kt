package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImageDetailState(
    val isLoading: Boolean = false,
    val submission: SubmissionResponse? = null,
    val error: String? = null,
    val isOwner: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false
)

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val submissionId: String = checkNotNull(savedStateHandle["submissionId"])

    private val _state = MutableStateFlow(ImageDetailState(isLoading = true))
    val state: StateFlow<ImageDetailState> = _state.asStateFlow()

    init {
        loadSubmission()
    }

    private fun loadSubmission() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = submissionRepository.getSubmission(submissionId)) {
                is ApiResult.Success -> {
                    val submission = result.data
                    val isOwner = checkOwnership(submission.userId)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        submission = submission,
                        isOwner = isOwner
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.error.displayMessage
                    )
                }
            }
        }
    }

    private fun checkOwnership(submissionUserId: String): Boolean {
        return tokenManager.getUserId() == submissionUserId
    }

    fun deleteSubmission() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true)
            when (submissionRepository.deleteSubmission(submissionId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(isDeleting = false, isDeleted = true)
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        error = "Failed to delete submission"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
