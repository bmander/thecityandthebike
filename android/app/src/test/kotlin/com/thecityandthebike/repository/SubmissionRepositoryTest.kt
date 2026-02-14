package com.thecityandthebike.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UploadResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.just
import io.mockk.Runs
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.io.IOException

class SubmissionRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var repository: SubmissionRepository

    @Before
    fun setup() {
        apiService = mockk()
        repository = SubmissionRepository(apiService)
    }

    // --- getSubmissions ---

    @Test
    fun `getSubmissions success returns paginated submissions`() = runTest {
        val submissions = CursorPaginatedSubmissions(
            items = listOf(
                SubmissionResponse(submissionId = "s1", userId = "u1", bikeQrId = "b1")
            ),
            nextCursor = null,
            hasMore = false
        )
        coEvery { apiService.getSubmissions(limit = 20, cursor = null) } returns
                Response.success(submissions)

        val result = repository.getSubmissions()

        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.items.size)
    }

    @Test
    fun `getSubmissions empty body returns empty list`() = runTest {
        coEvery { apiService.getSubmissions(limit = 20, cursor = null) } returns
                Response.success(null)

        val result = repository.getSubmissions()

        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.items.isEmpty())
    }

    @Test
    fun `getSubmissions HTTP error returns Server error`() = runTest {
        coEvery { apiService.getSubmissions(limit = 20, cursor = null) } returns
                Response.error(500, "error".toResponseBody())

        val result = repository.getSubmissions()

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Server)
    }

    @Test
    fun `getSubmissions forwards cursor to ApiService`() = runTest {
        val cursor = "abc123cursor"
        val submissions = CursorPaginatedSubmissions(
            items = emptyList(),
            nextCursor = null,
            hasMore = false
        )
        coEvery { apiService.getSubmissions(limit = 20, cursor = cursor) } returns
                Response.success(submissions)

        repository.getSubmissions(cursor = cursor)

        coVerify { apiService.getSubmissions(limit = 20, cursor = cursor) }
    }

    // --- getMySubmissions ---

    @Test
    fun `getMySubmissions success returns paginated submissions`() = runTest {
        val submissions = CursorPaginatedSubmissions(
            items = listOf(
                SubmissionResponse(submissionId = "s1", userId = "u1", bikeQrId = "b1")
            ),
            nextCursor = null,
            hasMore = false
        )
        coEvery { apiService.getMySubmissions(limit = 20, cursor = null) } returns
                Response.success(submissions)

        val result = repository.getMySubmissions()

        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.items.size)
    }

    @Test
    fun `getMySubmissions empty body returns empty list`() = runTest {
        coEvery { apiService.getMySubmissions(limit = 20, cursor = null) } returns
                Response.success(null)

        val result = repository.getMySubmissions()

        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.items.isEmpty())
    }

    @Test
    fun `getMySubmissions HTTP error returns Server error`() = runTest {
        coEvery { apiService.getMySubmissions(limit = 20, cursor = null) } returns
                Response.error(403, "forbidden".toResponseBody())

        val result = repository.getMySubmissions()

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Server)
    }

    @Test
    fun `getMySubmissions forwards cursor to ApiService`() = runTest {
        val cursor = "mycursor456"
        val submissions = CursorPaginatedSubmissions(
            items = emptyList(),
            nextCursor = null,
            hasMore = false
        )
        coEvery { apiService.getMySubmissions(limit = 20, cursor = cursor) } returns
                Response.success(submissions)

        repository.getMySubmissions(cursor = cursor)

        coVerify { apiService.getMySubmissions(limit = 20, cursor = cursor) }
    }

    // --- createSubmission ---

    @Test
    fun `createSubmission success returns submission`() = runTest {
        val tempFile = File.createTempFile("test_image", ".jpg")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02))
        tempFile.deleteOnExit()

        val response = SubmissionResponse(
            submissionId = "s1",
            userId = "u1",
            bikeQrId = "BIKE001"
        )
        coEvery { apiService.createSubmission(any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.createSubmission(tempFile, "BIKE001", "2025-01-01")

        assertTrue(result is ApiResult.Success)
        assertEquals("s1", (result as ApiResult.Success).data.submissionId)
    }

    @Test
    fun `createSubmission HTTP error returns error`() = runTest {
        val tempFile = File.createTempFile("test_image", ".jpg")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02))
        tempFile.deleteOnExit()

        coEvery { apiService.createSubmission(any(), any(), any(), any()) } returns
                Response.error(422, "validation error".toResponseBody())

        val result = repository.createSubmission(tempFile, "BIKE001", "2025-01-01")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Validation)
    }

    // --- getSubmission ---

    @Test
    fun `getSubmission success returns submission`() = runTest {
        val response = SubmissionResponse(submissionId = "s1", userId = "u1", bikeQrId = "b1")
        coEvery { apiService.getSubmission("s1") } returns Response.success(response)

        val result = repository.getSubmission("s1")

        assertTrue(result is ApiResult.Success)
        assertEquals("s1", (result as ApiResult.Success).data.submissionId)
    }

    @Test
    fun `getSubmission not found returns Server error`() = runTest {
        coEvery { apiService.getSubmission("s1") } returns
                Response.error(404, "not found".toResponseBody())

        val result = repository.getSubmission("s1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Server)
    }

    // --- deleteSubmission ---

    @Test
    fun `deleteSubmission success returns message`() = runTest {
        coEvery { apiService.deleteSubmission("s1") } returns
                Response.success(MessageResponse(msg = "Deleted"))

        val result = repository.deleteSubmission("s1")

        assertTrue(result is ApiResult.Success)
        assertEquals("Deleted", (result as ApiResult.Success).data.msg)
    }

    @Test
    fun `deleteSubmission unauthorized returns Auth error`() = runTest {
        coEvery { apiService.deleteSubmission("s1") } returns
                Response.error(401, "unauthorized".toResponseBody())

        val result = repository.deleteSubmission("s1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Auth)
    }

    // --- uploadImage ---

    @Test
    fun `uploadImage success returns upload response`() = runTest {
        val tempFile = File.createTempFile("test_image", ".jpg")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02))
        tempFile.deleteOnExit()

        val uploadResponse = UploadResponse(
            url = "https://example.com/uploaded.jpg",
            filename = "uploaded.jpg",
            thumbnailUrl = "https://example.com/thumb.jpg"
        )
        coEvery { apiService.uploadImage(any()) } returns Response.success(uploadResponse)

        val result = repository.uploadImage(tempFile)

        assertTrue(result is ApiResult.Success)
        assertEquals("https://example.com/uploaded.jpg", (result as ApiResult.Success).data.url)
    }

    @Test
    fun `uploadImage creates multipart with correct form field name`() = runTest {
        val tempFile = File.createTempFile("test_image", ".jpg")
        tempFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02))
        tempFile.deleteOnExit()

        val partSlot = slot<MultipartBody.Part>()
        coEvery { apiService.uploadImage(capture(partSlot)) } returns
                Response.success(UploadResponse(url = "url", filename = "f.jpg"))

        repository.uploadImage(tempFile)

        val capturedPart = partSlot.captured
        val disposition = capturedPart.headers?.get("Content-Disposition") ?: ""
        assertTrue(disposition.contains("name=\"image\""))
        assertTrue(disposition.contains("filename=\"${tempFile.name}\""))
    }

    @Test
    fun `uploadImage IOException returns Network error`() = runTest {
        val tempFile = File.createTempFile("test_image", ".jpg")
        tempFile.writeBytes(byteArrayOf(0x00))
        tempFile.deleteOnExit()

        coEvery { apiService.uploadImage(any()) } throws IOException("upload failed")

        val result = repository.uploadImage(tempFile)

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Network)
    }
}
