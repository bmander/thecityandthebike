package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.BikeListItem
import com.thecityandthebike.data.repository.BikeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BikesListState(
    val isLoading: Boolean = false,
    val bikes: List<BikeListItem> = emptyList(),
    val error: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true
)

@HiltViewModel
class BikesListViewModel @Inject constructor(
    private val bikeRepository: BikeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BikesListState())
    val state: StateFlow<BikesListState> = _state.asStateFlow()

    init {
        loadBikes()
    }

    private fun loadBikes() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = bikeRepository.getBikes()) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        bikes = result.data.items,
                        hasMorePages = result.data.items.size + result.data.offset < result.data.total
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

    fun loadMoreBikes() {
        val currentState = _state.value
        if (currentState.isLoadingMore || !currentState.hasMorePages) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            val offset = _state.value.bikes.size

            when (val result = bikeRepository.getBikes(offset = offset)) {
                is ApiResult.Success -> {
                    val newBikes = _state.value.bikes + result.data.items
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        bikes = newBikes,
                        hasMorePages = newBikes.size < result.data.total
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
