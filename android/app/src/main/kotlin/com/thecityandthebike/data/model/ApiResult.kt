package com.thecityandthebike.data.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val error: AppError) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Empty response"))
            }
        } else {
            val code = response.code()
            val rawBody = response.errorBody()?.string() ?: "Unknown error"
            val message = parseErrorMessage(rawBody) ?: rawBody
            val error = when (code) {
                401, 403 -> AppError.Auth(code, message)
                422 -> AppError.Validation("", message)
                429 -> AppError.RateLimit(response.headers()["Retry-After"]?.toIntOrNull())
                in 500..599 -> AppError.Server(code, message)
                else -> AppError.Server(code, message)
            }
            ApiResult.Error(error)
        }
    } catch (e: IOException) {
        ApiResult.Error(AppError.Network(e))
    } catch (e: Exception) {
        ApiResult.Error(AppError.Unknown(e))
    }
}

fun parseErrorMessage(body: String): String? {
    return try {
        val json = Json.parseToJsonElement(body).jsonObject
        when (val detail = json["detail"]) {
            is JsonObject -> detail["msg"]?.jsonPrimitive?.content
            is JsonArray -> detail.firstOrNull()?.jsonObject?.get("msg")?.jsonPrimitive?.content
            is JsonPrimitive -> detail.content
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
