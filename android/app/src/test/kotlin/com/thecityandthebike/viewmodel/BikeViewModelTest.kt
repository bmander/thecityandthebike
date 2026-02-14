package com.thecityandthebike.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.BikeDetailResponse
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.BikeRepository
import com.thecityandthebike.ui.viewmodel.BikeViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BikeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var bikeRepository: BikeRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private val testBikeQrId = "TEST-BIKE-001"

    private val testBikeDetail = BikeDetailResponse(
        bikeQrId = testBikeQrId,
        provider = "citibike",
        bikeBrand = null,
        firstSeenAt = "2024-01-15T10:00:00Z",
        lastSeenAt = "2024-06-20T14:30:00Z",
        notes = null,
        submissionCount = 3
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bikeRepository = mockk()
        savedStateHandle = SavedStateHandle(mapOf("bikeQrId" to testBikeQrId))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BikeViewModel {
        return BikeViewModel(bikeRepository, savedStateHandle)
    }

    @Test
    fun `loadBike success should update state with detail and submissions`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = testBikeQrId),
            SubmissionResponse(submissionId = "2", userId = "user1", bikeQrId = testBikeQrId)
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Success(testBikeDetail)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.bikeDetail)
        assertEquals(testBikeQrId, state.bikeDetail?.bikeQrId)
        assertEquals(3, state.bikeDetail?.submissionCount)
        assertEquals(2, state.submissions.size)
        assertFalse(state.hasMorePages)
        assertNull(state.nextCursor)
        assertNull(state.error)
    }

    @Test
    fun `loadBike detail failure should set error state`() = runTest {
        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Error(AppError.Server(404, "Not found"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.bikeDetail)
        assertEquals("Not found", state.error)
    }

    @Test
    fun `loadBike submissions failure should set error but keep detail`() = runTest {
        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Success(testBikeDetail)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, any(), any()) } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.bikeDetail)
        assertTrue(state.submissions.isEmpty())
        assertEquals("Network error. Check your connection and try again.", state.error)
    }

    @Test
    fun `loadMoreSubmissions should append items`() = runTest {
        val firstPage = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = testBikeQrId),
            SubmissionResponse(submissionId = "2", userId = "user1", bikeQrId = testBikeQrId)
        )
        val firstPaginated = CursorPaginatedSubmissions(items = firstPage, nextCursor = "cursor-abc", hasMore = true)

        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Success(testBikeDetail)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, limit = any(), cursor = null) } returns ApiResult.Success(firstPaginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertTrue(viewModel.state.value.hasMorePages)
        assertEquals("cursor-abc", viewModel.state.value.nextCursor)

        val secondPage = listOf(
            SubmissionResponse(submissionId = "3", userId = "user1", bikeQrId = testBikeQrId),
            SubmissionResponse(submissionId = "4", userId = "user1", bikeQrId = testBikeQrId)
        )
        val secondPaginated = CursorPaginatedSubmissions(items = secondPage, nextCursor = null, hasMore = false)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, limit = any(), cursor = "cursor-abc") } returns ApiResult.Success(secondPaginated)

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.state.value.submissions.size)
        assertEquals("3", viewModel.state.value.submissions[2].submissionId)
        assertFalse(viewModel.state.value.hasMorePages)
    }

    @Test
    fun `loadMoreSubmissions should not load when no more pages`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = testBikeQrId)
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Success(testBikeDetail)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.hasMorePages)

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.submissions.size)
    }

    @Test
    fun `loadMoreSubmissions error should keep existing submissions`() = runTest {
        val firstPage = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = testBikeQrId),
            SubmissionResponse(submissionId = "2", userId = "user1", bikeQrId = testBikeQrId)
        )
        val firstPaginated = CursorPaginatedSubmissions(items = firstPage, nextCursor = "cursor-abc", hasMore = true)

        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Success(testBikeDetail)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, limit = any(), cursor = null) } returns ApiResult.Success(firstPaginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, limit = any(), cursor = "cursor-abc") } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertEquals("Network error. Check your connection and try again.", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoadingMore)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Error(AppError.Server(500, "Error"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `bikeQrId should come from SavedStateHandle`() = runTest {
        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Success(testBikeDetail)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))

        val viewModel = createViewModel()
        assertEquals(testBikeQrId, viewModel.bikeQrId)
    }

    @Test
    fun `removeSubmission should remove by id`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = testBikeQrId),
            SubmissionResponse(submissionId = "2", userId = "user1", bikeQrId = testBikeQrId),
            SubmissionResponse(submissionId = "3", userId = "user1", bikeQrId = testBikeQrId)
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Success(testBikeDetail)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.state.value.submissions.size)

        viewModel.removeSubmission("2")

        assertEquals(2, viewModel.state.value.submissions.size)
        assertFalse(viewModel.state.value.submissions.any { it.submissionId == "2" })
        assertTrue(viewModel.state.value.submissions.any { it.submissionId == "1" })
        assertTrue(viewModel.state.value.submissions.any { it.submissionId == "3" })
    }

    @Test
    fun `removeSubmission should be no-op when id not found`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = testBikeQrId)
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { bikeRepository.getBikeDetail(testBikeQrId) } returns ApiResult.Success(testBikeDetail)
        coEvery { bikeRepository.getBikeSubmissions(testBikeQrId, any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeSubmission("nonexistent")

        assertEquals(1, viewModel.state.value.submissions.size)
        assertEquals("1", viewModel.state.value.submissions[0].submissionId)
    }
}
