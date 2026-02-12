package com.thecityandthebike.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.data.repository.TagRepository
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
    val isDeleted: Boolean = false,
    val tags: List<TagResponse> = emptyList(),
    val isTagMode: Boolean = false,
    val isCreatingTag: Boolean = false,
    val isDeletingTag: Boolean = false,
)

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val tagRepository: TagRepository,
    private val tokenManager: TokenManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val submissionId: String = checkNotNull(savedStateHandle["submissionId"])

    private val _state = MutableStateFlow(ImageDetailState(isLoading = true))
    val state: StateFlow<ImageDetailState> = _state.asStateFlow()

    init {
        loadSubmission()
        loadTags()
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

    private fun loadTags() {
        viewModelScope.launch {
            when (val result = tagRepository.getTags(submissionId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(tags = result.data)
                }
                is ApiResult.Error -> {
                    // Silently fail - tags are supplementary
                }
            }
        }
    }

    fun enterTagMode() {
        _state.value = _state.value.copy(isTagMode = true)
    }

    fun exitTagMode() {
        _state.value = _state.value.copy(isTagMode = false)
    }

    fun createTag(imageFile: java.io.File) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isCreatingTag = true)
            when (val result = tagRepository.createTag(submissionId, imageFile)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isCreatingTag = false,
                        isTagMode = false,
                        tags = listOf(result.data) + _state.value.tags
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isCreatingTag = false,
                        error = "Failed to create tag"
                    )
                }
            }
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeletingTag = true)
            when (tagRepository.deleteTag(tagId)) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        isDeletingTag = false,
                        tags = _state.value.tags.filter { it.tagId != tagId }
                    )
                }
                is ApiResult.Error -> {
                    _state.value = _state.value.copy(
                        isDeletingTag = false,
                        error = "Failed to delete tag"
                    )
                }
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.getUserId() != null
    }

    fun isTagOwner(tag: TagResponse): Boolean {
        return tokenManager.getUserId() == tag.userId
    }
}
