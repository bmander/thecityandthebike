package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.FlagStatusResponse
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UploadResponse
import com.thecityandthebike.data.model.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmissionRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getSubmissions(limit: Int = 20, cursor: String? = null): ApiResult<CursorPaginatedSubmissions> {
        return try {
            val response = apiService.getSubmissions(limit = limit, cursor = cursor)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    ApiResult.Success(paginated)
                } ?: ApiResult.Success(CursorPaginatedSubmissions(items = emptyList()))
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to fetch submissions"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    suspend fun getMySubmissions(limit: Int = 20, cursor: String? = null): ApiResult<CursorPaginatedSubmissions> {
        return try {
            val response = apiService.getMySubmissions(limit = limit, cursor = cursor)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    ApiResult.Success(paginated)
                } ?: ApiResult.Success(CursorPaginatedSubmissions(items = emptyList()))
            } else {
                ApiResult.Error(AppError.Server(response.code(), "Failed to fetch submissions"))
            }
        } catch (e: IOException) {
            ApiResult.Error(AppError.Network(e))
        } catch (e: Exception) {
            ApiResult.Error(AppError.Unknown(e))
        }
    }

    suspend fun createSubmission(
        imageFile: File, bikeQrId: String, capturedDate: String, userCaption: String? = null, side: String? = null
    ): ApiResult<SubmissionResponse> {
        return safeApiCall {
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            val bikeQrIdBody = bikeQrId.toRequestBody("text/plain".toMediaTypeOrNull())
            val capturedDateBody = capturedDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val userCaptionBody = userCaption?.toRequestBody("text/plain".toMediaTypeOrNull())
            val sideBody = side?.toRequestBody("text/plain".toMediaTypeOrNull())
            apiService.createSubmission(imagePart, bikeQrIdBody, capturedDateBody, userCaptionBody, sideBody)
        }
    }

    suspend fun getSubmission(submissionId: String): ApiResult<SubmissionResponse> {
        return safeApiCall { apiService.getSubmission(submissionId) }
    }

    suspend fun deleteSubmission(submissionId: String): ApiResult<MessageResponse> {
        return safeApiCall { apiService.deleteSubmission(submissionId) }
    }

    suspend fun uploadImage(file: File): ApiResult<UploadResponse> {
        return safeApiCall {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            apiService.uploadImage(body)
        }
    }

    suspend fun createFlag(submissionId: String): ApiResult<FlagStatusResponse> {
        return safeApiCall { apiService.createFlag(submissionId) }
    }

    suspend fun getFlagStatus(submissionId: String): ApiResult<FlagStatusResponse> {
        return safeApiCall { apiService.getFlagStatus(submissionId) }
    }

    suspend fun clearFlags(submissionId: String): ApiResult<FlagStatusResponse> {
        return safeApiCall { apiService.clearFlags(submissionId) }
    }
}
