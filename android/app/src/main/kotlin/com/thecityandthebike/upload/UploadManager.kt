package com.thecityandthebike.upload

import android.net.Uri
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.ScoringBreakdown
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.di.ApplicationScope
import com.thecityandthebike.util.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val uploadQueue: UploadQueue,
    private val connectivityMonitor: ConnectivityMonitor,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    val pendingUploads: StateFlow<List<PendingUpload>> = uploadQueue.pendingUploads

    private val processMutex = Mutex()

    init {
        appScope.launch {
            var wasOnline = connectivityMonitor.isOnline.value
            connectivityMonitor.isOnline.collect { online ->
                if (!wasOnline && online) {
                    processQueue()
                }
                wasOnline = online
            }
        }
    }

    fun uploadAndCreateSubmission(localUri: Uri, bikeQrId: String, side: String? = null) {
        appScope.launch {
            _state.value = UploadState.Uploading(localUri)

            val capturedDate = LocalDate.now(ZoneId.of("America/Los_Angeles")).toString()
            val pending = uploadQueue.enqueue(localUri, bikeQrId, side, capturedDate)

            if (pending == null) {
                _state.value = UploadState.Error("Could not read image file", localUri)
                return@launch
            }

            processQueue()
        }
    }

    fun retryUpload() {
        appScope.launch {
            // Reset any FAILED items back to QUEUED
            val uploads = uploadQueue.getAll()
            for (upload in uploads) {
                if (upload.status == PendingUploadStatus.FAILED) {
                    uploadQueue.updateStatus(upload.id, PendingUploadStatus.QUEUED)
                }
            }
            processQueue()
        }
    }

    suspend fun processQueue() {
        if (!processMutex.tryLock()) return
        try {
            while (true) {
                val next = uploadQueue.getAll()
                    .firstOrNull { it.status == PendingUploadStatus.QUEUED }
                    ?: break

                if (!connectivityMonitor.isOnline.value) break

                uploadQueue.updateStatus(next.id, PendingUploadStatus.UPLOADING)
                val imageFile = uploadQueue.getImageFile(next.id)
                val localUri = Uri.fromFile(imageFile)
                _state.value = UploadState.Uploading(localUri)

                when (val result = submissionRepository.createSubmission(
                    imageFile, next.bikeQrId, next.capturedDate, side = next.side
                )) {
                    is ApiResult.Success -> {
                        uploadQueue.remove(next.id)
                        _state.value = UploadState.Success(
                            pointsAwarded = result.data.pointsAwarded ?: 0,
                            pointsBreakdown = result.data.pointsBreakdown ?: emptyList()
                        )
                    }
                    is ApiResult.Error -> {
                        when (result.error) {
                            is AppError.Network -> {
                                uploadQueue.updateStatus(next.id, PendingUploadStatus.FAILED)
                                _state.value = UploadState.Error(result.error.displayMessage, localUri)
                                return
                            }
                            is AppError.Server -> {
                                uploadQueue.updateStatus(next.id, PendingUploadStatus.FAILED)
                                _state.value = UploadState.Error(result.error.displayMessage, localUri)
                                return
                            }
                            is AppError.RateLimit -> {
                                uploadQueue.updateStatus(next.id, PendingUploadStatus.FAILED)
                                _state.value = UploadState.Error(result.error.displayMessage, localUri)
                                return
                            }
                            else -> {
                                // Non-retryable (Auth, Validation, Unknown) — remove from queue
                                uploadQueue.remove(next.id)
                                _state.value = UploadState.Error(result.error.displayMessage, localUri)
                            }
                        }
                    }
                }
            }
        } finally {
            processMutex.unlock()
        }
    }

    fun clearSuccess() {
        if (_state.value is UploadState.Success) {
            _state.value = UploadState.Idle
        }
    }

    fun clearError() {
        _state.value = UploadState.Idle
    }
}
