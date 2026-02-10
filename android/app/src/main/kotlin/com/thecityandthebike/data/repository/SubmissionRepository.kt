package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.dto.PaginatedSubmissions
import com.thecityandthebike.data.model.dto.SubmissionCreate
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class SubmissionResult<out T> {
    data class Success<T>(val data: T) : SubmissionResult<T>()
    data class Error(val message: String) : SubmissionResult<Nothing>()
}

@Singleton
class SubmissionRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getSubmissions(limit: Int = 20, offset: Int = 0): SubmissionResult<PaginatedSubmissions> {
        return try {
            val response = apiService.getSubmissions(limit = limit, offset = offset)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    SubmissionResult.Success(paginated)
                } ?: SubmissionResult.Success(PaginatedSubmissions(items = emptyList(), total = 0, limit = limit, offset = offset))
            } else {
                SubmissionResult.Error("Failed to fetch submissions")
            }
        } catch (e: Exception) {
            SubmissionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMySubmissions(limit: Int = 20, offset: Int = 0): SubmissionResult<PaginatedSubmissions> {
        return try {
            val response = apiService.getMySubmissions(limit = limit, offset = offset)
            if (response.isSuccessful) {
                response.body()?.let { paginated ->
                    SubmissionResult.Success(paginated)
                } ?: SubmissionResult.Success(PaginatedSubmissions(items = emptyList(), total = 0, limit = limit, offset = offset))
            } else {
                SubmissionResult.Error("Failed to fetch submissions")
            }
        } catch (e: Exception) {
            SubmissionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun createSubmission(submission: SubmissionCreate): SubmissionResult<SubmissionResponse> {
        return try {
            val response = apiService.createSubmission(submission)
            if (response.isSuccessful) {
                response.body()?.let { submissionResponse ->
                    SubmissionResult.Success(submissionResponse)
                } ?: SubmissionResult.Error("Empty response")
            } else {
                SubmissionResult.Error("Failed to create submission")
            }
        } catch (e: Exception) {
            SubmissionResult.Error(e.message ?: "Network error")
        }
    }

    suspend fun uploadImage(file: File): SubmissionResult<UploadResponse> {
        return try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", file.name, requestFile)
            val response = apiService.uploadImage(body)
            if (response.isSuccessful) {
                response.body()?.let { uploadResponse ->
                    SubmissionResult.Success(uploadResponse)
                } ?: SubmissionResult.Error("Empty response")
            } else {
                SubmissionResult.Error("Failed to upload image")
            }
        } catch (e: Exception) {
            SubmissionResult.Error(e.message ?: "Network error")
        }
    }
}
