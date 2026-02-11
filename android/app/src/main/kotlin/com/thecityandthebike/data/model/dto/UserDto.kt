package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    @SerialName("user_id") val userId: String,
    val username: String,
    val email: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UserDetailResponse(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("submission_count") val submissionCount: Int,
    @SerialName("first_seen_at") val firstSeenAt: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null
)
