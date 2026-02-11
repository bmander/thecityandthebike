package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RefreshRequest
import com.thecityandthebike.data.model.dto.RegisterRequest
import com.thecityandthebike.data.model.dto.UserResponse
import com.thecityandthebike.data.model.safeApiCall
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    val isLoggedIn: StateFlow<Boolean> get() = tokenManager.isLoggedIn

    suspend fun login(username: String, password: String): ApiResult<Unit> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                response.body()?.let { tokenResponse ->
                    tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                    ApiResult.Success(Unit)
                } ?: ApiResult.Error(AppError.Server(response.code(), "Empty response"))
            } else {
                ApiResult.Error(AppError.Auth(response.code(), "Invalid credentials"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    suspend fun register(username: String, email: String, password: String): ApiResult<Unit> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else if (response.code() == 409) {
                ApiResult.Error(AppError.Server(409, "User already exists"))
            } else {
                ApiResult.Error(AppError.Auth(response.code(), "Registration failed"))
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

    fun isLoggedIn(): Boolean {
        return tokenManager.hasToken()
    }
}
