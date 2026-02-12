package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.LeaderboardResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getLeaderboard(period: String): ApiResult<LeaderboardResponse> {
        return try {
            val response = apiService.getLeaderboard(period)
            if (response.isSuccessful) {
                response.body()?.let { leaderboard ->
                    ApiResult.Success(leaderboard)
                } ?: ApiResult.Error(AppError.Server(response.code(), "Empty response"))
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to fetch leaderboard"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }
}
