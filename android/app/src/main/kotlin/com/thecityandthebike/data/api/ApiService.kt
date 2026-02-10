package com.thecityandthebike.data.api

import com.thecityandthebike.data.model.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<MessageResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest): Response<MessageResponse>

    // User endpoints
    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @GET("users/me/submissions")
    suspend fun getMySubmissions(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<PaginatedSubmissions>

    // Submission endpoints
    @GET("submissions")
    suspend fun getSubmissions(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<PaginatedSubmissions>

    @POST("submissions")
    suspend fun createSubmission(@Body submission: SubmissionCreate): Response<SubmissionResponse>

    // Upload endpoint
    @Multipart
    @POST("uploads/images")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadResponse>
}
