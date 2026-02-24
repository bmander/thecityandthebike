package com.thecityandthebike.upload

import android.net.Uri
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.ScoringBreakdown
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
import javax.inject.Inject
import javax.inject.Singleton

sealed interface UploadState {
    data object Idle : UploadState
    data class Uploading(val localUri: Uri) : UploadState
    data class Success(
        val pointsAwarded: Int = 0,
        val pointsBreakdown: List<ScoringBreakdown> = emptyList()
    ) : UploadState
    data class Error(val message: String, val localUri: Uri) : UploadState
}

@Singleton
class UploadManager @Inject constructor(
    private val submissionRepository: SubmissionRepository,
    private val imagePreparer: ImagePreparer,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    private var lastLocalUri: Uri? = null
    private var lastBikeQrId: String? = null
    private var lastSide: String? = null

    fun uploadAndCreateSubmission(localUri: Uri, bikeQrId: String, side: String? = null) {
        lastLocalUri = localUri
        lastBikeQrId = bikeQrId
        lastSide = side

        appScope.launch {
            _state.value = UploadState.Uploading(localUri)

            val imageFile = imagePreparer.prepareImageFile(localUri)

            if (imageFile == null) {
                _state.value = UploadState.Error("Could not read image file", localUri)
                return@launch
            }

            try {
                val capturedDate = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString()
                when (val result = submissionRepository.createSubmission(imageFile, bikeQrId, capturedDate, side = side)) {
                    is ApiResult.Success -> {
                        _state.value = UploadState.Success(
                            pointsAwarded = result.data.pointsAwarded ?: 0,
                            pointsBreakdown = result.data.pointsBreakdown ?: emptyList()
                        )
                    }
                    is ApiResult.Error -> {
                        _state.value = UploadState.Error(result.error.displayMessage, localUri)
                    }
                }
            } finally {
                imageFile.delete()
            }
        }
    }

    fun retryUpload() {
        val uri = lastLocalUri ?: return
        val bikeQrId = lastBikeQrId ?: return
        uploadAndCreateSubmission(uri, bikeQrId, lastSide)
    }

    fun clearSuccess() {
        if (_state.value is UploadState.Success) {
            _state.value = UploadState.Idle
        }
    }

    fun clearError() {
        _state.value = UploadState.Idle
        lastLocalUri = null
        lastBikeQrId = null
        lastSide = null
    }
}
