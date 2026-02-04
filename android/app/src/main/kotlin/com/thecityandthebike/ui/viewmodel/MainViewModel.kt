package com.thecityandthebike.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thecityandthebike.data.model.dto.SubmissionCreate
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.data.repository.SubmissionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class MainState(
    val isLoading: Boolean = false,
    val submissions: List<SubmissionResponse> = emptyList(),
    val localImages: List<Uri> = emptyList(),
    val error: String? = null,
    val isUploading: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        loadSubmissions()
    }

    fun loadSubmissions() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = submissionRepository.getSubmissions()) {
                is SubmissionResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        submissions = result.data
                    )
                }
                is SubmissionResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = result.message
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

    fun uploadAndCreateSubmission(imageFile: File, bikeQrId: String? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null)

            // First upload the image
            when (val uploadResult = submissionRepository.uploadImage(imageFile)) {
                is SubmissionResult.Success -> {
                    val imageUrl = uploadResult.data.url

                    // Then create the submission
                    val submission = SubmissionCreate(
                        bikeQrId = bikeQrId ?: UUID.randomUUID().toString(),
                        imageUrlOriginal = imageUrl,
                        imageUrlProcessed = imageUrl,
                        capturedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                    )

                    when (val createResult = submissionRepository.createSubmission(submission)) {
                        is SubmissionResult.Success -> {
                            _state.value = _state.value.copy(
                                isUploading = false,
                                submissions = listOf(createResult.data) + _state.value.submissions
                            )
                        }
                        is SubmissionResult.Error -> {
                            _state.value = _state.value.copy(
                                isUploading = false,
                                error = createResult.message
                            )
                        }
                    }
                }
                is SubmissionResult.Error -> {
                    _state.value = _state.value.copy(
                        isUploading = false,
                        error = uploadResult.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
