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
