package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val userId: String? = tokenManager.getUserId()

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        val id = userId ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val detailResult = userRepository.getUserDetail(id)) {
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

            when (val subsResult = userRepository.getUserSubmissions(id)) {
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
        val id = userId ?: return
        val currentState = _state.value
        if (currentState.isLoadingMore || !currentState.hasMorePages) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)

            when (val result = userRepository.getUserSubmissions(id, cursor = _state.value.nextCursor)) {
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
