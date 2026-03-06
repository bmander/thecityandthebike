package com.thecityandthebike.upload

import kotlinx.serialization.Serializable

@Serializable
data class PendingUpload(
    val id: String,
    val imageFileName: String,
    val bikeQrId: String,
    val side: String? = null,
    val capturedDate: String,
    val createdAt: Long,
    val status: PendingUploadStatus = PendingUploadStatus.QUEUED
)

@Serializable
enum class PendingUploadStatus {
    QUEUED,
    UPLOADING,
    FAILED
}
