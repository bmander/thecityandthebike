package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.BikeDetailResponse
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.BikeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BikeState(
    val isLoading: Boolean = false,
    val bikeDetail: BikeDetailResponse? = null,
    val submissions: List<SubmissionResponse> = emptyList(),
    val error: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val nextCursor: String? = null
)

@HiltViewModel
class BikeViewModel @Inject constructor(
    private val bikeRepository: BikeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val bikeQrId: String = checkNotNull(savedStateHandle["bikeQrId"])

    private val _state = MutableStateFlow(BikeState())
    val state: StateFlow<BikeState> = _state.asStateFlow()

    init {
        loadBike()
    }

    private fun loadBike() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val detailResult = bikeRepository.getBikeDetail(bikeQrId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(bikeDetail = detailResult.data)
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = detailResult.error.displayMessage
                    )
                    return@launch
                }
            }

            when (val subsResult = bikeRepository.getBikeSubmissions(bikeQrId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        submissions = subsResult.data.items,
                        hasMorePages = subsResult.data.hasMore,
                        nextCursor = subsResult.data.nextCursor
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = subsResult.error.displayMessage
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

            when (val result = bikeRepository.getBikeSubmissions(bikeQrId, cursor = _state.value.nextCursor)) {
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

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
