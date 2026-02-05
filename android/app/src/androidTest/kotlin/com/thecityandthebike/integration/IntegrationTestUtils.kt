package com.thecityandthebike.integration

import android.content.Context
import android.os.Build
import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.LoginRequest
import com.thecityandthebike.data.model.dto.RegisterRequest
import com.thecityandthebike.data.model.dto.TokenResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

object IntegrationTestUtils {

    private const val TAG = "IntegrationTest"

    // Use 10.0.2.2 for emulators, host machine IP for physical devices
    private val BASE_URL: String
        get() {
            val url = if (isEmulator()) {
                "http://10.0.2.2:8000/"
            } else {
                // Physical device: use host machine's local network IP
                // Update this IP to match your development machine
                "http://10.0.0.17:8000/"
            }
            Log.d(TAG, "Using BASE_URL: $url (isEmulator=${isEmulator()})")
            return url
        }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.PRODUCT == "google_sdk"
                || Build.PRODUCT.startsWith("sdk")
                || Build.PRODUCT.endsWith("_sdk")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun generateUniqueUsername(): String {
        return "testuser_${UUID.randomUUID().toString().take(8)}"
    }

    fun generateUniqueEmail(): String {
        return "test_${UUID.randomUUID().toString().take(8)}@test.com"
    }

    fun createApiService(): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }

    fun createAuthenticatedApiService(token: String): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }

    suspend fun registerTestUser(
        apiService: ApiService,
        username: String = generateUniqueUsername(),
        email: String = generateUniqueEmail(),
        password: String = "testpassword123"
    ): RegisterResult {
        val response = apiService.register(RegisterRequest(username, email, password))
        return if (response.isSuccessful) {
            RegisterResult.Success(username, email, password)
        } else {
            RegisterResult.Failure(response.code(), response.errorBody()?.string())
        }
    }

    suspend fun registerAndLogin(
        apiService: ApiService,
        username: String = generateUniqueUsername(),
        email: String = generateUniqueEmail(),
        password: String = "testpassword123"
    ): LoginResult {
        val registerResult = registerTestUser(apiService, username, email, password)
        if (registerResult is RegisterResult.Failure) {
            return LoginResult.Failure(registerResult.code, "Registration failed: ${registerResult.error}")
        }

        val successResult = registerResult as RegisterResult.Success
        val loginResponse = apiService.login(LoginRequest(successResult.username, successResult.password))

        return if (loginResponse.isSuccessful) {
            val tokenResponse = loginResponse.body()!!
            LoginResult.Success(
                username = successResult.username,
                email = successResult.email,
                token = tokenResponse.accessToken,
                tokenResponse = tokenResponse
            )
        } else {
            LoginResult.Failure(loginResponse.code(), loginResponse.errorBody()?.string())
        }
    }

    sealed class RegisterResult {
        data class Success(
            val username: String,
            val email: String,
            val password: String
        ) : RegisterResult()

        data class Failure(
            val code: Int,
            val error: String?
        ) : RegisterResult()
    }

    sealed class LoginResult {
        data class Success(
            val username: String,
            val email: String,
            val token: String,
            val tokenResponse: TokenResponse
        ) : LoginResult()

        data class Failure(
            val code: Int,
            val error: String?
        ) : LoginResult()
    }

    fun copyAssetToFile(context: Context, assetName: String, cacheDir: File): File {
        val inputStream = context.assets.open(assetName)
        val tempFile = File.createTempFile(
            assetName.substringBeforeLast("."),
            ".${assetName.substringAfterLast(".")}",
            cacheDir
        )
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        return tempFile
    }

    fun createImageMultipart(file: File, partName: String = "image"): MultipartBody.Part {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
    }
}
