package com.thecityandthebike.data.model

sealed class AppError {
    data class Network(val cause: Throwable) : AppError()
    data class Auth(val code: Int, val message: String) : AppError()
    data class Validation(val field: String, val message: String) : AppError()
    data class Server(val code: Int, val message: String) : AppError()
    data class RateLimit(val retryAfterSeconds: Int? = null) : AppError()
    data class Unknown(val cause: Throwable) : AppError()

    val displayMessage: String get() = when (this) {
        is Network -> "Network error. Check your connection and try again."
        is Auth -> message
        is Validation -> message
        is Server -> "Server error. Please try again later."
        is RateLimit -> "Too many attempts. Please try again later."
        is Unknown -> "An unexpected error occurred."
    }
}
