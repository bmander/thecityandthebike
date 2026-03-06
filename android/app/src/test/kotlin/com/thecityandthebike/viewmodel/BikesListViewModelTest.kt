package com.thecityandthebike.viewmodel

import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.BikeListItem
import com.thecityandthebike.data.model.dto.PaginatedBikes
import com.thecityandthebike.data.model.dto.UserSummary
import com.thecityandthebike.data.repository.BikeRepository
import com.thecityandthebike.ui.viewmodel.BikesListViewModel
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
class BikesListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var bikeRepository: BikeRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bikeRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BikesListViewModel {
        return BikesListViewModel(bikeRepository)
    }

    @Test
    fun `loadBikes success should update state with bikes`() = runTest {
        val bikes = listOf(
            BikeListItem(bikeQrId = "BIKE-001", provider = "citibike", submissionCount = 5, owner = UserSummary(name = "alice", id = "u1")),
            BikeListItem(bikeQrId = "BIKE-002", provider = null, submissionCount = 3, owner = null)
        )
        val paginated = PaginatedBikes(items = bikes, total = 2, limit = 20, offset = 0)

        coEvery { bikeRepository.getBikes(any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.bikes.size)
        assertEquals("BIKE-001", state.bikes[0].bikeQrId)
        assertEquals("BIKE-002", state.bikes[1].bikeQrId)
        assertFalse(state.hasMorePages)
        assertNull(state.error)
    }

    @Test
    fun `loadBikes failure should set error state`() = runTest {
        coEvery { bikeRepository.getBikes(any(), any()) } returns ApiResult.Error(AppError.Server(500, "Error"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.bikes.isEmpty())
        assertEquals("Error", state.error)
    }

    @Test
    fun `loadBikes network error should set error state`() = runTest {
        coEvery { bikeRepository.getBikes(any(), any()) } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadMoreBikes should append items`() = runTest {
        val firstPage = listOf(
            BikeListItem(bikeQrId = "BIKE-001", provider = null, submissionCount = 5, owner = null),
            BikeListItem(bikeQrId = "BIKE-002", provider = null, submissionCount = 3, owner = null)
        )
        val firstPaginated = PaginatedBikes(items = firstPage, total = 4, limit = 2, offset = 0)

        coEvery { bikeRepository.getBikes(limit = any(), offset = 0) } returns ApiResult.Success(firstPaginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.bikes.size)
        assertTrue(viewModel.state.value.hasMorePages)

        val secondPage = listOf(
            BikeListItem(bikeQrId = "BIKE-003", provider = null, submissionCount = 2, owner = null),
            BikeListItem(bikeQrId = "BIKE-004", provider = null, submissionCount = 1, owner = null)
        )
        val secondPaginated = PaginatedBikes(items = secondPage, total = 4, limit = 2, offset = 2)
        coEvery { bikeRepository.getBikes(limit = any(), offset = 2) } returns ApiResult.Success(secondPaginated)

        viewModel.loadMoreBikes()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.state.value.bikes.size)
        assertEquals("BIKE-003", viewModel.state.value.bikes[2].bikeQrId)
        assertFalse(viewModel.state.value.hasMorePages)
    }

    @Test
    fun `loadMoreBikes should not load when no more pages`() = runTest {
        val bikes = listOf(
            BikeListItem(bikeQrId = "BIKE-001", provider = null, submissionCount = 1, owner = null)
        )
        val paginated = PaginatedBikes(items = bikes, total = 1, limit = 20, offset = 0)

        coEvery { bikeRepository.getBikes(any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.hasMorePages)

        viewModel.loadMoreBikes()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.bikes.size)
    }

    @Test
    fun `loadMoreBikes error should keep existing bikes`() = runTest {
        val firstPage = listOf(
            BikeListItem(bikeQrId = "BIKE-001", provider = null, submissionCount = 5, owner = null),
            BikeListItem(bikeQrId = "BIKE-002", provider = null, submissionCount = 3, owner = null)
        )
        val firstPaginated = PaginatedBikes(items = firstPage, total = 4, limit = 2, offset = 0)

        coEvery { bikeRepository.getBikes(limit = any(), offset = 0) } returns ApiResult.Success(firstPaginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { bikeRepository.getBikes(limit = any(), offset = 2) } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))

        viewModel.loadMoreBikes()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.bikes.size)
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoadingMore)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { bikeRepository.getBikes(any(), any()) } returns ApiResult.Error(AppError.Server(500, "Error"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }
}
