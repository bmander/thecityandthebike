package com.thecityandthebike.data.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.data.model.safeApiCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    suspend fun createTag(
        submissionId: String,
        imageFile: File,
        ring: List<List<Float>>? = null,
        ringWidth: Int? = null,
        ringHeight: Int? = null
    ): ApiResult<TagResponse> {
        return safeApiCall {
            val requestFile = imageFile.asRequestBody("image/png".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
            val textMediaType = "text/plain".toMediaTypeOrNull()
            val ringBody = ring?.let { Json.encodeToString(it).toRequestBody(textMediaType) }
            val ringWidthBody = ringWidth?.let { it.toString().toRequestBody(textMediaType) }
            val ringHeightBody = ringHeight?.let { it.toString().toRequestBody(textMediaType) }
            apiService.createTag(submissionId, body, ringBody, ringWidthBody, ringHeightBody)
        }
    }

    suspend fun deleteTag(tagId: String): ApiResult<MessageResponse> {
        return safeApiCall { apiService.deleteTag(tagId) }
    }
}
