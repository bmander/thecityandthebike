package com.thecityandthebike.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.data.repository.TagRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.io.IOException

class TagRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var repository: TagRepository

    @Before
    fun setup() {
        apiService = mockk()
        repository = TagRepository(apiService)
    }

    private val testTag = TagResponse(
        tagId = "tag-1",
        submissionId = "sub-1",
        userId = "user-1",
        imageUrl = "/uploads/images/tag.png",
        createdAt = "2026-01-01T00:00:00Z"
    )

    // --- getTags ---

    @Test
    fun `getTags success returns list of tags`() = runTest {
        coEvery { apiService.getTags("sub-1") } returns Response.success(listOf(testTag))

        val result = repository.getTags("sub-1")

        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.size)
        assertEquals("tag-1", result.data[0].tagId)
    }

    @Test
    fun `getTags empty returns empty list`() = runTest {
        coEvery { apiService.getTags("sub-1") } returns Response.success(emptyList())

        val result = repository.getTags("sub-1")

        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    @Test
    fun `getTags HTTP error returns Server error`() = runTest {
        coEvery { apiService.getTags("sub-1") } returns
                Response.error(404, "not found".toResponseBody())

        val result = repository.getTags("sub-1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Server)
    }

    @Test
    fun `getTags IOException returns Network error`() = runTest {
        coEvery { apiService.getTags("sub-1") } throws IOException("network down")

        val result = repository.getTags("sub-1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Network)
    }

    // --- createTag ---

    @Test
    fun `createTag success returns tag response`() = runTest {
        coEvery { apiService.createTag("sub-1", any(), any(), any(), any()) } returns Response.success(testTag)

        val tempFile = File.createTempFile("test_tag", ".png")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01))
        tempFile.deleteOnExit()

        val result = repository.createTag("sub-1", tempFile)

        assertTrue(result is ApiResult.Success)
        assertEquals("tag-1", (result as ApiResult.Success).data.tagId)
    }

    @Test
    fun `createTag creates multipart with correct form field and content type`() = runTest {
        val partSlot = slot<MultipartBody.Part>()
        coEvery { apiService.createTag("sub-1", capture(partSlot), any(), any(), any()) } returns
                Response.success(testTag)

        val tempFile = File.createTempFile("test_tag", ".png")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01))
        tempFile.deleteOnExit()

        repository.createTag("sub-1", tempFile)

        val disposition = partSlot.captured.headers?.get("Content-Disposition") ?: ""
        assertTrue(disposition.contains("name=\"image\""))
        assertTrue(disposition.contains("filename=\"${tempFile.name}\""))
    }

    @Test
    fun `createTag HTTP 401 returns Auth error`() = runTest {
        coEvery { apiService.createTag("sub-1", any(), any(), any(), any()) } returns
                Response.error(401, "unauthorized".toResponseBody())

        val tempFile = File.createTempFile("test_tag", ".png")
        tempFile.writeBytes(byteArrayOf(0x00))
        tempFile.deleteOnExit()

        val result = repository.createTag("sub-1", tempFile)

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Auth)
    }

    @Test
    fun `createTag IOException returns Network error`() = runTest {
        coEvery { apiService.createTag("sub-1", any(), any(), any(), any()) } throws IOException("upload failed")

        val tempFile = File.createTempFile("test_tag", ".png")
        tempFile.writeBytes(byteArrayOf(0x00))
        tempFile.deleteOnExit()

        val result = repository.createTag("sub-1", tempFile)

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Network)
    }

    // --- deleteTag ---

    @Test
    fun `deleteTag success returns message`() = runTest {
        coEvery { apiService.deleteTag("tag-1") } returns
                Response.success(MessageResponse(msg = "Tag deleted"))

        val result = repository.deleteTag("tag-1")

        assertTrue(result is ApiResult.Success)
        assertEquals("Tag deleted", (result as ApiResult.Success).data.msg)
    }

    @Test
    fun `deleteTag HTTP 403 returns Auth error`() = runTest {
        coEvery { apiService.deleteTag("tag-1") } returns
                Response.error(403, "forbidden".toResponseBody())

        val result = repository.deleteTag("tag-1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Auth)
    }

    @Test
    fun `deleteTag HTTP 404 returns Server error`() = runTest {
        coEvery { apiService.deleteTag("tag-1") } returns
                Response.error(404, "not found".toResponseBody())

        val result = repository.deleteTag("tag-1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Server)
    }

    @Test
    fun `deleteTag IOException returns Network error`() = runTest {
        coEvery { apiService.deleteTag("tag-1") } throws IOException("network down")

        val result = repository.deleteTag("tag-1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Network)
    }

    // --- createTag with ring data ---

    @Test
    fun `createTag with ring data sends ring as multipart parts`() = runTest {
        coEvery { apiService.createTag("sub-1", any(), any(), any(), any()) } returns
                Response.success(testTag)

        val tempFile = File.createTempFile("test_tag", ".png")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01))
        tempFile.deleteOnExit()

        val ring = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f), listOf(0f, 0f))
        val result = repository.createTag("sub-1", tempFile, ring, 200, 200)

        assertTrue(result is ApiResult.Success)

        coVerify {
            apiService.createTag(
                "sub-1",
                any(),
                match { body ->
                    val content = okio.Buffer().also { body.writeTo(it) }.readUtf8()
                    content.contains("100")
                },
                match { body ->
                    val content = okio.Buffer().also { body.writeTo(it) }.readUtf8()
                    content == "200"
                },
                match { body ->
                    val content = okio.Buffer().also { body.writeTo(it) }.readUtf8()
                    content == "200"
                }
            )
        }
    }

    @Test
    fun `createTag without ring data sends null parts`() = runTest {
        coEvery { apiService.createTag("sub-1", any(), any(), any(), any()) } returns
                Response.success(testTag)

        val tempFile = File.createTempFile("test_tag", ".png")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01))
        tempFile.deleteOnExit()

        val result = repository.createTag("sub-1", tempFile)

        assertTrue(result is ApiResult.Success)

        coVerify {
            apiService.createTag("sub-1", any(), null, null, null)
        }
    }

}
