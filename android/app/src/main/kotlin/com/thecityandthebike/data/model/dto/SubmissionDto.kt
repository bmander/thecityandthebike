package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmissionResponse(
    @SerialName("submission_id") val submissionId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("bike_qr_id") val bikeQrId: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_url_thumbnail") val imageUrlThumbnail: String? = null,
    @SerialName("captured_date") val capturedDate: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("user_caption") val userCaption: String? = null,
    val username: String? = null,
    val provider: String? = null,
    val side: String? = null
)

@Serializable
data class PaginatedSubmissions(
    val items: List<SubmissionResponse>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class CursorPaginatedSubmissions(
    val items: List<SubmissionResponse>,
    @SerialName("next_cursor") val nextCursor: String? = null,
    @SerialName("has_more") val hasMore: Boolean = false
)

@Serializable
data class UploadResponse(
    val url: String,
    val filename: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null
)
