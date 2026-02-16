package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagResponse(
    @SerialName("tag_id") val tagId: String,
    @SerialName("submission_id") val submissionId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("image_url") val imageUrl: String,
    val ring: List<List<Float>>? = null,
    @SerialName("ring_width") val ringWidth: Int? = null,
    @SerialName("ring_height") val ringHeight: Int? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class TagDetailResponse(
    @SerialName("tag_id") val tagId: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("submission_count") val submissionCount: Int,
    @SerialName("first_captured_at") val firstCapturedAt: String? = null,
    @SerialName("last_captured_at") val lastCapturedAt: String? = null,
    @SerialName("first_captured_by") val firstCapturedBy: UserSummary? = null,
    @SerialName("last_captured_by") val lastCapturedBy: UserSummary? = null
)
