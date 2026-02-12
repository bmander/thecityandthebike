package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.BikeDetailResponse
import com.thecityandthebike.data.model.dto.PaginatedBikes
import com.thecityandthebike.data.model.dto.PaginatedSubmissions
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BikeRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getBikes(limit: Int = 20, offset: Int = 0): ApiResult<PaginatedBikes> {
        return try {
            val response = apiService.getBikes(limit = limit, offset = offset)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    ApiResult.Success(paginated)
                } ?: ApiResult.Success(PaginatedBikes(items = emptyList(), total = 0, limit = limit, offset = offset))
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to fetch bikes"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    suspend fun getBikeDetail(bikeQrId: String): ApiResult<BikeDetailResponse> {
        return try {
            val response = apiService.getBikeDetail(bikeQrId)
            if (response.isSuccessful) {
                response.body()?.let { detail ->
                    ApiResult.Success(detail)
                } ?: ApiResult.Error(AppError.Server(response.code(), "Empty response"))
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to fetch bike details"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    suspend fun getBikeSubmissions(
        bikeQrId: String,
        limit: Int = 20,
        offset: Int = 0
    ): ApiResult<PaginatedSubmissions> {
        return try {
            val response = apiService.getBikeSubmissions(bikeQrId, limit = limit, offset = offset)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    ApiResult.Success(paginated)
                } ?: ApiResult.Success(
                    PaginatedSubmissions(items = emptyList(), total = 0, limit = limit, offset = offset)
                )
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to fetch bike submissions"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }
}
