package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String = "bearer"
)

@Serializable
data class MessageResponse(
    val msg: String
)

@Serializable
data class ErrorDetail(
    val msg: String
)

@Serializable
data class ErrorResponse(
    val detail: ErrorDetail? = null
)
