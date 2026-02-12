package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.data.model.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getTags(submissionId: String): ApiResult<List<TagResponse>> {
        return safeApiCall { apiService.getTags(submissionId) }
    }

    suspend fun createTag(submissionId: String, imageFile: File): ApiResult<TagResponse> {
        return safeApiCall {
            val requestFile = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            apiService.createTag(submissionId, body)
        }
    }

    suspend fun deleteTag(tagId: String): ApiResult<MessageResponse> {
        return safeApiCall { apiService.deleteTag(tagId) }
    }
}
