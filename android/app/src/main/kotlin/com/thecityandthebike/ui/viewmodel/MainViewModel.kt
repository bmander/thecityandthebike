package com.thecityandthebike.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.SubmissionCreate
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.util.ImagePreparer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class MainState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val submissions: List<SubmissionResponse> = emptyList(),
    val localImages: List<Uri> = emptyList(),
    val error: String? = null,
    val isUploading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val totalSubmissions: Int = 0
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val imagePreparer: ImagePreparer
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        loadSubmissions()
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

    fun addLocalImage(uri: Uri) {
        _state.value = _state.value.copy(
            localImages = listOf(uri) + _state.value.localImages
        )
    }

    fun uploadAndCreateSubmission(localUri: Uri, bikeQrId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null)

            val imageFile = imagePreparer.prepareImageFile(localUri)

            if (imageFile == null) {
                _state.value = _state.value.copy(
                    isUploading = false,
                    localImages = _state.value.localImages.filter { it != localUri },
                    error = "Could not read image file"
                )
                return@launch
            }

            try {
                // First upload the image
                when (val uploadResult = submissionRepository.uploadImage(imageFile)) {
                    is ApiResult.Success -> {
                        val imageUrl = uploadResult.data.url

                        // Then create the submission
                        val submission = SubmissionCreate(
                            bikeQrId = bikeQrId ?: UUID.randomUUID().toString(),
                            imageUrlOriginal = imageUrl,
                            imageUrlThumbnail = uploadResult.data.thumbnailUrl,
                            imageUrlProcessed = imageUrl,
                            capturedDate = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString()
                        )

                        when (val createResult = submissionRepository.createSubmission(submission)) {
                            is ApiResult.Success -> {
                                _state.value = _state.value.copy(
                                    isUploading = false,
                                    submissions = listOf(createResult.data) + _state.value.submissions,
                                    localImages = _state.value.localImages.filter { it != localUri }
                                )
                            }
                            is ApiResult.Error -> {
                                _state.value = _state.value.copy(
                                    isUploading = false,
                                    localImages = _state.value.localImages.filter { it != localUri },
                                    error = createResult.error.displayMessage
                                )
                            }
                        }
                    }
                    is ApiResult.Error -> {
                        _state.value = _state.value.copy(
                            isUploading = false,
                            localImages = _state.value.localImages.filter { it != localUri },
                            error = uploadResult.error.displayMessage
                        )
                    }
                }
            } finally {
                imageFile.delete()
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
