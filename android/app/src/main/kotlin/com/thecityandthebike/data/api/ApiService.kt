package com.thecityandthebike.data.api

import com.thecityandthebike.data.model.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
        @Query("cursor") cursor: String? = null
    ): Response<CursorPaginatedSubmissions>

    // Submission endpoints
    @GET("submissions")
    suspend fun getSubmissions(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CursorPaginatedSubmissions>

    @Multipart
    @POST("submissions")
    suspend fun createSubmission(
        @Part image: MultipartBody.Part,
        @Part("bike_qr_id") bikeQrId: RequestBody,
        @Part("captured_date") capturedDate: RequestBody,
        @Part("user_caption") userCaption: RequestBody?
    ): Response<SubmissionResponse>

    @GET("submissions/{submissionId}")
    suspend fun getSubmission(@Path("submissionId") submissionId: String): Response<SubmissionResponse>

    @DELETE("submissions/{submissionId}")
    suspend fun deleteSubmission(@Path("submissionId") submissionId: String): Response<MessageResponse>

    // User detail endpoints
    @GET("users/{userId}")
    suspend fun getUserDetail(@Path("userId") userId: String): Response<UserDetailResponse>

    @GET("users/{userId}/submissions")
    suspend fun getUserSubmissions(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CursorPaginatedSubmissions>

    // Bike endpoints
    @GET("bikes")
    suspend fun getBikes(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<PaginatedBikes>

    @GET("bikes/{bikeQrId}")
    suspend fun getBikeDetail(@Path("bikeQrId") bikeQrId: String): Response<BikeDetailResponse>

    @GET("bikes/{bikeQrId}/submissions")
    suspend fun getBikeSubmissions(
        @Path("bikeQrId") bikeQrId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<CursorPaginatedSubmissions>

    // Leaderboard endpoint
    @GET("leaderboard")
    suspend fun getLeaderboard(@Query("period") period: String = "weekly"): Response<LeaderboardResponse>

    // Upload endpoint
    @Multipart
    @POST("uploads/images")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadResponse>

    // Tag endpoints
    @GET("submissions/{submissionId}/tags")
    suspend fun getTags(@Path("submissionId") submissionId: String): Response<List<TagResponse>>

    @Multipart
    @POST("submissions/{submissionId}/tags")
    suspend fun createTag(
        @Path("submissionId") submissionId: String,
        @Part image: MultipartBody.Part,
        @Part("ring") ring: okhttp3.RequestBody? = null,
        @Part("ring_width") ringWidth: okhttp3.RequestBody? = null,
        @Part("ring_height") ringHeight: okhttp3.RequestBody? = null
    ): Response<TagResponse>

    @Multipart
    @POST("submissions/{submissionId}/process-mask")
    suspend fun processMask(
        @Path("submissionId") submissionId: String,
        @Part image: MultipartBody.Part
    ): Response<ProcessedMaskResponse>

    @DELETE("tags/{tagId}")
    suspend fun deleteTag(@Path("tagId") tagId: String): Response<MessageResponse>
}
