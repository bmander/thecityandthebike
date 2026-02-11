package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val error: String? = null
)

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val submissionId: String = checkNotNull(savedStateHandle["submissionId"])

    private val _state = MutableStateFlow(ImageDetailState())
    val state: StateFlow<ImageDetailState> = _state.asStateFlow()

    init {
        loadSubmission()
    }

    private fun loadSubmission() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = submissionRepository.getSubmission(submissionId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        submission = result.data
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
