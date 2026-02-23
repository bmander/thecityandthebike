package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RefreshRequest
import com.thecityandthebike.data.model.dto.ErrorResponse
import com.thecityandthebike.data.model.dto.RegisterRequest
import com.thecityandthebike.data.model.dto.UserResponse
import com.thecityandthebike.data.model.safeApiCall
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    val isLoggedIn: StateFlow<Boolean> get() = tokenManager.isLoggedIn

    suspend fun login(username: String, password: String, androidId: String? = null): ApiResult<Unit> {
        return try {
            val response = apiService.login(LoginRequest(username, password, androidId))
            if (response.isSuccessful) {
                response.body()?.let { tokenResponse ->
                    tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                    // Fetch user info to store isAdmin
                    try {
                        val userResponse = apiService.getCurrentUser()
                        userResponse.body()?.let { user ->
                            tokenManager.saveIsAdmin(user.isAdmin)
                        }
                    } catch (_: Exception) {
                        // Non-critical, continue
                    }
                    ApiResult.Success(Unit)
                } ?: ApiResult.Error(AppError.Server(response.code(), "Empty response"))
            } else if (response.code() == 429) {
                val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
                ApiResult.Error(AppError.RateLimit(retryAfter))
            } else if (response.code() == 403) {
                val errorMessage = try {
                    val body = response.errorBody()?.string()
                    body?.let { Json.decodeFromString<ErrorResponse>(it).detail?.msg }
                } catch (_: Exception) {
                    null
                } ?: "This account or device has been banned"
                ApiResult.Error(AppError.Auth(response.code(), errorMessage))
            } else {
                ApiResult.Error(AppError.Auth(response.code(), "Invalid credentials"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    suspend fun register(username: String, password: String, androidId: String? = null): ApiResult<Unit> {
        return try {
            val response = apiService.register(RegisterRequest(username, password, androidId))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else if (response.code() == 429) {
                val retryAfter = response.headers()["Retry-After"]?.toIntOrNull()
                ApiResult.Error(AppError.RateLimit(retryAfter))
            } else {
                val errorMessage = try {
                    val body = response.errorBody()?.string()
                    body?.let { Json.decodeFromString<ErrorResponse>(it).detail?.msg }
                } catch (_: Exception) {
                    null
                } ?: "Registration failed"
                ApiResult.Error(AppError.Server(response.code(), errorMessage))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    suspend fun getCurrentUser(): ApiResult<UserResponse> {
        return safeApiCall { apiService.getCurrentUser() }
    }

    suspend fun logout() {
        try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                apiService.logout(RefreshRequest(refreshToken))
            }
        } catch (_: Exception) {
            // Best-effort server-side logout
        }
        tokenManager.clearToken()
    }

    suspend fun deleteAccount(): ApiResult<Unit> {
        return try {
            val response = apiService.deleteAccount()
            if (response.isSuccessful) {
                tokenManager.clearToken()
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to delete account"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.hasToken()
    }
}
