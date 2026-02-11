package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.PaginatedSubmissions
import com.thecityandthebike.data.model.dto.UserDetailResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getUserDetail(userId: String): SubmissionResult<UserDetailResponse> {
        return try {
            val response = apiService.getUserDetail(userId)
            if (response.isSuccessful) {
                response.body()?.let { detail ->
                    SubmissionResult.Success(detail)
                } ?: SubmissionResult.Error("Empty response")
            } else {
                SubmissionResult.Error("Failed to fetch user details")
            }
        } catch (e: Exception) {
            SubmissionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getUserSubmissions(
        userId: String,
        limit: Int = 20,
        offset: Int = 0
    ): SubmissionResult<PaginatedSubmissions> {
        return try {
            val response = apiService.getUserSubmissions(userId, limit = limit, offset = offset)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    SubmissionResult.Success(paginated)
                } ?: SubmissionResult.Success(
                    PaginatedSubmissions(items = emptyList(), total = 0, limit = limit, offset = offset)
                )
            } else {
                SubmissionResult.Error("Failed to fetch user submissions")
            }
        } catch (e: Exception) {
            SubmissionResult.Error(e.message ?: "Network error")
        }
    }
}
