package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.UserDetailResponse
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getUserDetail(userId: String): ApiResult<UserDetailResponse> {
        return try {
            val response = apiService.getUserDetail(userId)
            if (response.isSuccessful) {
                response.body()?.let { detail ->
                    ApiResult.Success(detail)
                } ?: ApiResult.Error(AppError.Server(response.code(), "Empty response"))
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to fetch user details"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    suspend fun getUserSubmissions(
        userId: String,
        limit: Int = 20,
        cursor: String? = null
    ): ApiResult<CursorPaginatedSubmissions> {
        return try {
            val response = apiService.getUserSubmissions(userId, limit = limit, cursor = cursor)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    ApiResult.Success(paginated)
                } ?: ApiResult.Success(
                    CursorPaginatedSubmissions(items = emptyList())
                )
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to fetch user submissions"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }
}
