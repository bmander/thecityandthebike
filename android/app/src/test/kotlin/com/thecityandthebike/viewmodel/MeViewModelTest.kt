package com.thecityandthebike.viewmodel

import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UserDetailResponse
import com.thecityandthebike.data.repository.UserRepository
import com.thecityandthebike.ui.viewmodel.MeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
class MeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var tokenManager: TokenManager

    private val testUserId = "550e8400-e29b-41d4-a716-446655440000"

    private val testUserDetail = UserDetailResponse(
        userId = testUserId,
        username = "testuser",
        submissionCount = 3,
        firstSeenAt = "2024-01-15T10:00:00Z",
        lastSeenAt = "2024-06-20T14:30:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        tokenManager = mockk()
        every { tokenManager.getUserId() } returns testUserId
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MeViewModel {
        return MeViewModel(userRepository, tokenManager)
    }

    @Test
    fun `loadUser success should update state with detail and submissions`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001"),
            SubmissionResponse(submissionId = "2", userId = testUserId, bikeQrId = "BIKE-002")
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.userDetail)
        assertEquals("testuser", state.userDetail?.username)
        assertEquals(3, state.userDetail?.submissionCount)
        assertEquals(2, state.submissions.size)
        assertFalse(state.hasMorePages)
        assertNull(state.nextCursor)
        assertNull(state.error)
    }

    @Test
    fun `loadUser detail failure should set error state`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Error(AppError.Server(404, "Not found"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.userDetail)
        assertEquals("Not found", state.error)
    }

    @Test
    fun `loadUser submissions failure should set error but keep detail`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.userDetail)
        assertTrue(state.submissions.isEmpty())
        assertEquals("Network error. Check your connection and try again.", state.error)
    }

    @Test
    fun `null userId should not load anything`() = runTest {
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.userDetail)
        assertTrue(state.submissions.isEmpty())
        assertNull(state.error)

        coVerify(exactly = 0) { userRepository.getUserDetail(any()) }
    }

    @Test
    fun `loadMoreSubmissions should append items`() = runTest {
        val firstPage = listOf(
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001"),
            SubmissionResponse(submissionId = "2", userId = testUserId, bikeQrId = "BIKE-002")
        )
        val firstPaginated = CursorPaginatedSubmissions(items = firstPage, nextCursor = "cursor-abc", hasMore = true)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), cursor = null) } returns ApiResult.Success(firstPaginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertTrue(viewModel.state.value.hasMorePages)

        val secondPage = listOf(
            SubmissionResponse(submissionId = "3", userId = testUserId, bikeQrId = "BIKE-003")
        )
        val secondPaginated = CursorPaginatedSubmissions(items = secondPage, nextCursor = null, hasMore = false)
        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), cursor = "cursor-abc") } returns ApiResult.Success(secondPaginated)

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.state.value.submissions.size)
        assertEquals("3", viewModel.state.value.submissions[2].submissionId)
        assertFalse(viewModel.state.value.hasMorePages)
    }

    @Test
    fun `loadMoreSubmissions should not load when no more pages`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001")
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.submissions.size)
    }

    @Test
    fun `loadMoreSubmissions with null userId should be no-op`() = runTest {
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { userRepository.getUserSubmissions(any(), any(), any()) }
    }

    @Test
    fun `removeSubmission should remove by id`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001"),
            SubmissionResponse(submissionId = "2", userId = testUserId, bikeQrId = "BIKE-002"),
            SubmissionResponse(submissionId = "3", userId = testUserId, bikeQrId = "BIKE-003")
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeSubmission("2")

        assertEquals(2, viewModel.state.value.submissions.size)
        assertFalse(viewModel.state.value.submissions.any { it.submissionId == "2" })
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Error(AppError.Server(500, "Error"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }
}
