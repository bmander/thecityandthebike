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
data class ProcessedMaskResponse(
    val ring: List<List<Float>>,
    val width: Int,
    val height: Int
)
