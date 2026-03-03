package com.thecityandthebike.data.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    @SerialName("android_id") val androidId: String? = null
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    @SerialName("android_id") val androidId: String? = null
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer"
)

@Serializable
data class MessageResponse(
    val msg: String
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

/**
 * Parses error message from API error bodies that may have `detail` as either:
 * - an object: `{"detail": {"msg": "..."}}`
 * - a list (FastAPI 422): `{"detail": [{"msg": "...", ...}]}`
 * - a string: `{"detail": "..."}`
 */
fun parseErrorMessage(body: String): String? {
    return try {
        val json = Json.parseToJsonElement(body).jsonObject
        when (val detail = json["detail"]) {
            is JsonObject -> detail["msg"]?.jsonPrimitive?.content
            is JsonArray -> detail.jsonArray.firstOrNull()?.jsonObject?.get("msg")?.jsonPrimitive?.content
            is JsonPrimitive -> detail.content
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
