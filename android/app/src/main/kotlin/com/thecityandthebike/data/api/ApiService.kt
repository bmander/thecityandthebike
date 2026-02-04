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

    // User endpoints
    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @GET("users/me/submissions")
    suspend fun getMySubmissions(): Response<List<SubmissionResponse>>

    // Submission endpoints
    @GET("submissions")
    suspend fun getSubmissions(): Response<List<SubmissionResponse>>

    @POST("submissions")
    suspend fun createSubmission(@Body submission: SubmissionCreate): Response<SubmissionResponse>

    // Upload endpoint
    @Multipart
    @POST("uploads/images")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadResponse>
}
