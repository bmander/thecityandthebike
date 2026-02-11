package com.thecityandthebike.data.model

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
            val message = response.errorBody()?.string() ?: "Unknown error"
            val error = when (code) {
                401, 403 -> AppError.Auth(code, message)
                422 -> AppError.Validation("", message)
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
