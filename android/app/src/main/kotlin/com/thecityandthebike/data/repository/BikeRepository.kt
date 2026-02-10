package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.BikeDetailResponse
import com.thecityandthebike.data.model.dto.PaginatedSubmissions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BikeRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getBikeDetail(bikeQrId: String): SubmissionResult<BikeDetailResponse> {
        return try {
            val response = apiService.getBikeDetail(bikeQrId)
            if (response.isSuccessful) {
                response.body()?.let { detail ->
                    SubmissionResult.Success(detail)
                } ?: SubmissionResult.Error("Empty response")
            } else {
                SubmissionResult.Error("Failed to fetch bike details")
            }
        } catch (e: Exception) {
            SubmissionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getBikeSubmissions(
        bikeQrId: String,
        limit: Int = 20,
        offset: Int = 0
    ): SubmissionResult<PaginatedSubmissions> {
        return try {
            val response = apiService.getBikeSubmissions(bikeQrId, limit = limit, offset = offset)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    SubmissionResult.Success(paginated)
                } ?: SubmissionResult.Success(
                    PaginatedSubmissions(items = emptyList(), total = 0, limit = limit, offset = offset)
                )
            } else {
                SubmissionResult.Error("Failed to fetch bike submissions")
            }
        } catch (e: Exception) {
            SubmissionResult.Error(e.message ?: "Network error")
        }
    }
}
