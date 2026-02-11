package com.thecityandthebike.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.thecityandthebike.data.model.dto.PaginatedSubmissions
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UserDetailResponse
import com.thecityandthebike.data.repository.SubmissionResult
import com.thecityandthebike.data.repository.UserRepository
import com.thecityandthebike.ui.viewmodel.UserViewModel
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
class UserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var savedStateHandle: SavedStateHandle

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
        savedStateHandle = SavedStateHandle(mapOf("userId" to testUserId))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): UserViewModel {
        return UserViewModel(userRepository, savedStateHandle)
    }

    @Test
    fun `loadUser success should update state with detail and submissions`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001"),
            SubmissionResponse(submissionId = "2", userId = testUserId, bikeQrId = "BIKE-002")
        )
        val paginated = PaginatedSubmissions(items = submissions, total = 2, limit = 20, offset = 0)

        coEvery { userRepository.getUserDetail(testUserId) } returns SubmissionResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns SubmissionResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.userDetail)
        assertEquals("testuser", state.userDetail?.username)
        assertEquals(3, state.userDetail?.submissionCount)
        assertEquals(2, state.submissions.size)
        assertFalse(state.hasMorePages)
        assertNull(state.error)
    }

    @Test
    fun `loadUser detail failure should set error state`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns SubmissionResult.Error("Not found")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.userDetail)
        assertEquals("Not found", state.error)
    }

    @Test
    fun `loadUser submissions failure should set error but keep detail`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns SubmissionResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns SubmissionResult.Error("Network error")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.userDetail)
        assertTrue(state.submissions.isEmpty())
        assertEquals("Network error", state.error)
    }

    @Test
    fun `loadMoreSubmissions should append items`() = runTest {
        val firstPage = listOf(
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001"),
            SubmissionResponse(submissionId = "2", userId = testUserId, bikeQrId = "BIKE-002")
        )
        val firstPaginated = PaginatedSubmissions(items = firstPage, total = 4, limit = 2, offset = 0)

        coEvery { userRepository.getUserDetail(testUserId) } returns SubmissionResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), offset = 0) } returns SubmissionResult.Success(firstPaginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertTrue(viewModel.state.value.hasMorePages)

        val secondPage = listOf(
            SubmissionResponse(submissionId = "3", userId = testUserId, bikeQrId = "BIKE-003"),
            SubmissionResponse(submissionId = "4", userId = testUserId, bikeQrId = "BIKE-004")
        )
        val secondPaginated = PaginatedSubmissions(items = secondPage, total = 4, limit = 2, offset = 2)
        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), offset = 2) } returns SubmissionResult.Success(secondPaginated)

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.state.value.submissions.size)
        assertEquals("3", viewModel.state.value.submissions[2].submissionId)
        assertFalse(viewModel.state.value.hasMorePages)
    }

    @Test
    fun `loadMoreSubmissions should not load when no more pages`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001")
        )
        val paginated = PaginatedSubmissions(items = submissions, total = 1, limit = 20, offset = 0)

        coEvery { userRepository.getUserDetail(testUserId) } returns SubmissionResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns SubmissionResult.Success(paginated)

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
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001"),
            SubmissionResponse(submissionId = "2", userId = testUserId, bikeQrId = "BIKE-002")
        )
        val firstPaginated = PaginatedSubmissions(items = firstPage, total = 4, limit = 2, offset = 0)

        coEvery { userRepository.getUserDetail(testUserId) } returns SubmissionResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), offset = 0) } returns SubmissionResult.Success(firstPaginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), offset = 2) } returns SubmissionResult.Error("Network error")

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertEquals("Network error", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoadingMore)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns SubmissionResult.Error("Error")

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `userId should come from SavedStateHandle`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns SubmissionResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            SubmissionResult.Success(PaginatedSubmissions(items = emptyList(), total = 0, limit = 20, offset = 0))

        val viewModel = createViewModel()
        assertEquals(testUserId, viewModel.userId)
    }
}
