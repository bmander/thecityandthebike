package com.thecityandthebike.upload

import android.net.Uri
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.SubmissionCreate
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.di.ApplicationScope
import com.thecityandthebike.util.ImagePreparer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UploadState {
    data object Idle : UploadState
    data object Uploading : UploadState
    data object Success : UploadState
    data class Error(val message: String) : UploadState
}

@Singleton
class UploadManager @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val imagePreparer: ImagePreparer,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    fun uploadAndCreateSubmission(localUri: Uri, bikeQrId: String) {
        appScope.launch {
            _state.value = UploadState.Uploading

            val imageFile = imagePreparer.prepareImageFile(localUri)

            if (imageFile == null) {
                _state.value = UploadState.Error("Could not read image file")
                return@launch
            }

            try {
                when (val uploadResult = submissionRepository.uploadImage(imageFile)) {
                    is ApiResult.Success -> {
                        val imageUrl = uploadResult.data.url

                        val submission = SubmissionCreate(
                            bikeQrId = bikeQrId,
                            imageUrlOriginal = imageUrl,
                            imageUrlThumbnail = uploadResult.data.thumbnailUrl,
                            imageUrlProcessed = imageUrl,
                            capturedDate = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString()
                        )

                        when (val createResult = submissionRepository.createSubmission(submission)) {
                            is ApiResult.Success -> {
                                _state.value = UploadState.Success
                            }
                            is ApiResult.Error -> {
                                _state.value = UploadState.Error(createResult.error.displayMessage)
                            }
                        }
                    }
                    is ApiResult.Error -> {
                        _state.value = UploadState.Error(uploadResult.error.displayMessage)
                    }
                }
            } finally {
                imageFile.delete()
            }
        }
    }

    fun clearError() {
        _state.value = UploadState.Idle
    }
}
