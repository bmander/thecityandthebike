package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.local.TokenManager
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
    val hasMorePages: Boolean = true,
    val nextCursor: String? = null,
    val isBanning: Boolean = false,
    val currentUserIsAdmin: Boolean = false
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _state = MutableStateFlow(UserState(currentUserIsAdmin = tokenManager.isAdmin()))
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
                        error = detailResult.error.displayMessageOrNull
                    )
                    return@launch
                }
            }

            when (val subsResult = userRepository.getUserSubmissions(userId)) {
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
                        error = subsResult.error.displayMessageOrNull
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

            when (val result = userRepository.getUserSubmissions(userId, cursor = _state.value.nextCursor)) {
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
                        error = result.error.displayMessageOrNull
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

    fun banUser() = toggleBan(ban = true)

    fun unbanUser() = toggleBan(ban = false)

    private fun toggleBan(ban: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isBanning = true)
            val result = if (ban) userRepository.banUser(userId) else userRepository.unbanUser(userId)
            when (result) {
                is ApiResult.Success -> {
                    val detail = _state.value.userDetail
                    _state.value = _state.value.copy(
                        isBanning = false,
                        userDetail = detail?.copy(isBanned = ban)
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isBanning = false,
                        error = result.error.displayMessageOrNull
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
