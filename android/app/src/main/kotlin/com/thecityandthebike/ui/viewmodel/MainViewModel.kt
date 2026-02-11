package com.thecityandthebike.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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
    private val imagePreparer: ImagePreparer,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(
        MainState(
            localImages = savedStateHandle.get<List<String>>(LOCAL_IMAGES_KEY)
                ?.map { Uri.parse(it) } ?: emptyList()
        )
    )
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
        val newImages = listOf(uri) + _state.value.localImages
        _state.value = _state.value.copy(localImages = newImages)
        saveLocalImages(newImages)
    }

    fun uploadAndCreateSubmission(localUri: Uri, bikeQrId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null)

            val imageFile = imagePreparer.prepareImageFile(localUri)

            if (imageFile == null) {
                val newImages = _state.value.localImages.filter { it != localUri }
                _state.value = _state.value.copy(
                    isUploading = false,
                    localImages = newImages,
                    error = "Could not read image file"
                )
                saveLocalImages(newImages)
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
                                val newImages = _state.value.localImages.filter { it != localUri }
                                _state.value = _state.value.copy(
                                    isUploading = false,
                                    submissions = listOf(createResult.data) + _state.value.submissions,
                                    localImages = newImages
                                )
                                saveLocalImages(newImages)
                            }
                            is ApiResult.Error -> {
                                val newImages = _state.value.localImages.filter { it != localUri }
                                _state.value = _state.value.copy(
                                    isUploading = false,
                                    localImages = newImages,
                                    error = createResult.error.displayMessage
                                )
                                saveLocalImages(newImages)
                            }
                        }
                    }
                    is ApiResult.Error -> {
                        val newImages = _state.value.localImages.filter { it != localUri }
                        _state.value = _state.value.copy(
                            isUploading = false,
                            localImages = newImages,
                            error = uploadResult.error.displayMessage
                        )
                        saveLocalImages(newImages)
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

    private fun saveLocalImages(images: List<Uri>) {
        savedStateHandle[LOCAL_IMAGES_KEY] = images.map { it.toString() }
    }

    companion object {
        private const val LOCAL_IMAGES_KEY = "localImages"
    }
}
