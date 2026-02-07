package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmissionCreate(
    @SerialName("bike_qr_id") val bikeQrId: String,
    @SerialName("image_url_original") val imageUrlOriginal: String,
    @SerialName("image_url_processed") val imageUrlProcessed: String,
    @SerialName("captured_at") val capturedAt: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("user_caption") val userCaption: String? = null
)

@Serializable
data class SubmissionResponse(
    @SerialName("submission_id") val submissionId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("bike_qr_id") val bikeQrId: String,
    @SerialName("image_url_original") val imageUrlOriginal: String? = null,
    @SerialName("image_url_processed") val imageUrlProcessed: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("captured_at") val capturedAt: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("user_caption") val userCaption: String? = null,
    val username: String? = null
)

@Serializable
data class UploadResponse(
    val url: String,
    val filename: String
)
