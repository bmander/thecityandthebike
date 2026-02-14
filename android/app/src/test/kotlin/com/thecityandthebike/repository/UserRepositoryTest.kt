package com.thecityandthebike.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UserDetailResponse
import com.thecityandthebike.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class UserRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var repository: UserRepository

    @Before
    fun setup() {
        apiService = mockk()
        repository = UserRepository(apiService)
    }

    // --- getUserDetail ---

    @Test
    fun `getUserDetail success returns user detail`() = runTest {
        val detail = UserDetailResponse(
            userId = "u1",
            username = "testuser",
            submissionCount = 10
        )
        coEvery { apiService.getUserDetail("u1") } returns Response.success(detail)

        val result = repository.getUserDetail("u1")

        assertTrue(result is ApiResult.Success)
        assertEquals("testuser", (result as ApiResult.Success).data.username)
        assertEquals(10, result.data.submissionCount)
    }

    @Test
    fun `getUserDetail empty body returns Server error`() = runTest {
        coEvery { apiService.getUserDetail("u1") } returns Response.success(null)

        val result = repository.getUserDetail("u1")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals("Empty response", (error as AppError.Server).message)
    }

    @Test
    fun `getUserDetail HTTP error returns Server error`() = runTest {
        coEvery { apiService.getUserDetail("u1") } returns
                Response.error(404, "not found".toResponseBody())

        val result = repository.getUserDetail("u1")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(404, (error as AppError.Server).code)
    }

    @Test
    fun `getUserDetail IOException returns Network error`() = runTest {
        coEvery { apiService.getUserDetail("u1") } throws IOException("dns failure")

        val result = repository.getUserDetail("u1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Network)
    }

    // --- getUserSubmissions ---

    @Test
    fun `getUserSubmissions success returns paginated submissions`() = runTest {
        val submissions = CursorPaginatedSubmissions(
            items = listOf(
                SubmissionResponse(submissionId = "s1", userId = "u1", bikeQrId = "b1")
            ),
            nextCursor = null,
            hasMore = false
        )
        coEvery { apiService.getUserSubmissions("u1", limit = 20, cursor = null) } returns
                Response.success(submissions)

        val result = repository.getUserSubmissions("u1")

        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.items.size)
    }

    @Test
    fun `getUserSubmissions forwards cursor to ApiService`() = runTest {
        val cursor = "usercursor789"
        val submissions = CursorPaginatedSubmissions(
            items = emptyList(),
            nextCursor = null,
            hasMore = false
        )
        coEvery { apiService.getUserSubmissions("u1", limit = 20, cursor = cursor) } returns
                Response.success(submissions)

        repository.getUserSubmissions("u1", cursor = cursor)

        coVerify { apiService.getUserSubmissions("u1", limit = 20, cursor = cursor) }
    }

    @Test
    fun `getUserSubmissions empty body returns empty list`() = runTest {
        coEvery { apiService.getUserSubmissions("u1", limit = 20, cursor = null) } returns
                Response.success(null)

        val result = repository.getUserSubmissions("u1")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertTrue(data.items.isEmpty())
    }

    @Test
    fun `getUserSubmissions HTTP error returns Server error`() = runTest {
        coEvery { apiService.getUserSubmissions("u1", limit = 20, cursor = null) } returns
                Response.error(500, "server error".toResponseBody())

        val result = repository.getUserSubmissions("u1")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(500, (error as AppError.Server).code)
    }

    @Test
    fun `getUserSubmissions IOException returns Network error`() = runTest {
        coEvery { apiService.getUserSubmissions("u1", limit = 20, cursor = null) } throws
                IOException("timeout")

        val result = repository.getUserSubmissions("u1")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Network)
    }
}
