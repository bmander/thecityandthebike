package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RefreshRequest
import com.thecityandthebike.data.model.dto.RegisterRequest
import com.thecityandthebike.data.model.dto.UserResponse
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    val isLoggedIn: StateFlow<Boolean> get() = tokenManager.isLoggedIn

    suspend fun login(username: String, password: String): AuthResult<Unit> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                response.body()?.let { tokenResponse ->
                    tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                    AuthResult.Success(Unit)
                } ?: AuthResult.Error("Empty response")
            } else {
                AuthResult.Error("Invalid credentials")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun register(username: String, email: String, password: String): AuthResult<Unit> {
        return try {
            val response = apiService.register(RegisterRequest(username, email, password))
            if (response.isSuccessful) {
                AuthResult.Success(Unit)
            } else if (response.code() == 409) {
                AuthResult.Error("User already exists")
            } else {
                AuthResult.Error("Registration failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getCurrentUser(): AuthResult<UserResponse> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    AuthResult.Success(user)
                } ?: AuthResult.Error("Empty response")
            } else {
                AuthResult.Error("Failed to get user")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error")
        }
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
