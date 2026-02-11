package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UserDetailResponse
import com.thecityandthebike.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserState(
    val isLoading: Boolean = false,
    val userDetail: UserDetailResponse? = null,
    val submissions: List<SubmissionResponse> = emptyList(),
    val error: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val detailResult = userRepository.getUserDetail(userId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(userDetail = detailResult.data)
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = detailResult.error.displayMessage
                    )
                    return@launch
                }
            }

            when (val subsResult = userRepository.getUserSubmissions(userId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        submissions = subsResult.data.items,
                        hasMorePages = subsResult.data.items.size + subsResult.data.offset < subsResult.data.total
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
            val offset = _state.value.submissions.size

            when (val result = userRepository.getUserSubmissions(userId, offset = offset)) {
                is ApiResult.Success -> {
                    val newSubmissions = _state.value.submissions + result.data.items
                    _state.value = _state.value.copy(
                        isLoadingMore = false,
                        submissions = newSubmissions,
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
