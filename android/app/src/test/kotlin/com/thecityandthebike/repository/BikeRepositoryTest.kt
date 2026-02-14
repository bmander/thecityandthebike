package com.thecityandthebike.repository

import com.thecityandthebike.data.api.ApiService
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.BikeDetailResponse
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.BikeRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class BikeRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var repository: BikeRepository

    @Before
    fun setup() {
        apiService = mockk()
        repository = BikeRepository(apiService)
    }

    // --- getBikeDetail ---

    @Test
    fun `getBikeDetail success returns bike detail`() = runTest {
        val detail = BikeDetailResponse(
            bikeQrId = "BIKE001",
            provider = "CityBike",
            submissionCount = 5
        )
        coEvery { apiService.getBikeDetail("BIKE001") } returns Response.success(detail)

        val result = repository.getBikeDetail("BIKE001")

        assertTrue(result is ApiResult.Success)
        assertEquals("BIKE001", (result as ApiResult.Success).data.bikeQrId)
        assertEquals(5, result.data.submissionCount)
    }

    @Test
    fun `getBikeDetail empty body returns Server error`() = runTest {
        coEvery { apiService.getBikeDetail("BIKE001") } returns Response.success(null)

        val result = repository.getBikeDetail("BIKE001")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals("Empty response", (error as AppError.Server).message)
    }

    @Test
    fun `getBikeDetail HTTP error returns Server error`() = runTest {
        coEvery { apiService.getBikeDetail("BIKE001") } returns Response.error(
            404, "Not found".toResponseBody()
        )

        val result = repository.getBikeDetail("BIKE001")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(404, (error as AppError.Server).code)
    }

    @Test
    fun `getBikeDetail IOException returns Network error`() = runTest {
        coEvery { apiService.getBikeDetail("BIKE001") } throws IOException("no internet")

        val result = repository.getBikeDetail("BIKE001")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Network)
    }

    // --- getBikeSubmissions ---

    @Test
    fun `getBikeSubmissions success returns paginated submissions`() = runTest {
        val submissions = CursorPaginatedSubmissions(
            items = listOf(
                SubmissionResponse(
                    submissionId = "sub1",
                    userId = "user1",
                    bikeQrId = "BIKE001"
                )
            ),
            nextCursor = null,
            hasMore = false
        )
        coEvery { apiService.getBikeSubmissions("BIKE001", limit = 20, cursor = null) } returns
                Response.success(submissions)

        val result = repository.getBikeSubmissions("BIKE001")

        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.items.size)
    }

    @Test
    fun `getBikeSubmissions empty body returns empty list`() = runTest {
        coEvery { apiService.getBikeSubmissions("BIKE001", limit = 20, cursor = null) } returns
                Response.success(null)

        val result = repository.getBikeSubmissions("BIKE001")

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertTrue(data.items.isEmpty())
    }

    @Test
    fun `getBikeSubmissions HTTP error returns Server error`() = runTest {
        coEvery { apiService.getBikeSubmissions("BIKE001", limit = 20, cursor = null) } returns
                Response.error(500, "Server error".toResponseBody())

        val result = repository.getBikeSubmissions("BIKE001")

        assertTrue(result is ApiResult.Error)
        val error = (result as ApiResult.Error).error
        assertTrue(error is AppError.Server)
        assertEquals(500, (error as AppError.Server).code)
    }

    @Test
    fun `getBikeSubmissions IOException returns Network error`() = runTest {
        coEvery { apiService.getBikeSubmissions("BIKE001", limit = 20, cursor = null) } throws
                IOException("timeout")

        val result = repository.getBikeSubmissions("BIKE001")

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).error is AppError.Network)
    }
}
