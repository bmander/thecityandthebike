package com.thecityandthebike.data.api

import com.thecityandthebike.BuildConfig
import com.thecityandthebike.data.local.TokenManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Give up after 2 attempts to prevent infinite loops
        if (responseCount(response) >= 2) {
            tokenManager.clearToken()
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clearToken()
            return null
        }

        // Use a plain OkHttpClient to avoid circular dependency with AuthInterceptor
        val client = OkHttpClient()
        val requestBody = json.encodeToString(mapOf("refresh_token" to refreshToken))
            .toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url(BuildConfig.BASE_URL + "/auth/refresh")
            .post(requestBody)
            .build()

        val refreshResponse = try {
            client.newCall(refreshRequest).execute()
        } catch (e: Exception) {
            tokenManager.clearToken()
            return null
        }

        if (!refreshResponse.isSuccessful) {
            refreshResponse.close()
            tokenManager.clearToken()
            return null
        }

        val body = refreshResponse.body?.string() ?: run {
            tokenManager.clearToken()
            return null
        }

        return try {
            val jsonObj = json.parseToJsonElement(body).jsonObject
            val newAccessToken = jsonObj["access_token"]?.jsonPrimitive?.content ?: run {
                tokenManager.clearToken()
                return null
            }
            val newRefreshToken = jsonObj["refresh_token"]?.jsonPrimitive?.content ?: run {
                tokenManager.clearToken()
                return null
            }

            tokenManager.saveTokens(newAccessToken, newRefreshToken)

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        } catch (e: Exception) {
            tokenManager.clearToken()
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
