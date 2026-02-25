package com.thecityandthebike.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.BanResponse
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UserDetailResponse
import com.thecityandthebike.data.repository.UserRepository
import com.thecityandthebike.ui.viewmodel.UserViewModel
import io.mockk.coEvery
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
class UserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var userRepository: UserRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var savedStateHandle: SavedStateHandle

    private val testUserId = "550e8400-e29b-41d4-a716-446655440000"

    private val testUserDetail = UserDetailResponse(
        userId = testUserId,
        username = "testuser",
        submissionCount = 3,
        firstSeenAt = "2024-01-15T10:00:00Z",
        lastSeenAt = "2024-06-20T14:30:00Z",
        ownedBikeCount = 0,
        leaderboardRanks = emptyList()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        tokenManager = mockk(relaxed = true)
        every { tokenManager.isAdmin() } returns false
        savedStateHandle = SavedStateHandle(mapOf("userId" to testUserId))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): UserViewModel {
        return UserViewModel(userRepository, tokenManager, savedStateHandle)
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
        assertEquals("cursor-abc", viewModel.state.value.nextCursor)

        val secondPage = listOf(
            SubmissionResponse(submissionId = "3", userId = testUserId, bikeQrId = "BIKE-003"),
            SubmissionResponse(submissionId = "4", userId = testUserId, bikeQrId = "BIKE-004")
        )
        val secondPaginated = CursorPaginatedSubmissions(items = secondPage, nextCursor = null, hasMore = false)
        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), cursor = "cursor-abc") } returns ApiResult.Success(secondPaginated)

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
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns ApiResult.Success(paginated)

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
        val firstPaginated = CursorPaginatedSubmissions(items = firstPage, nextCursor = "cursor-abc", hasMore = true)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), cursor = null) } returns ApiResult.Success(firstPaginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { userRepository.getUserSubmissions(testUserId, limit = any(), cursor = "cursor-abc") } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertEquals("Network error. Check your connection and try again.", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoadingMore)
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

    @Test
    fun `userId should come from SavedStateHandle`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))

        val viewModel = createViewModel()
        assertEquals(testUserId, viewModel.userId)
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
            SubmissionResponse(submissionId = "1", userId = testUserId, bikeQrId = "BIKE-001")
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns ApiResult.Success(paginated)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeSubmission("nonexistent")

        assertEquals(1, viewModel.state.value.submissions.size)
        assertEquals("1", viewModel.state.value.submissions[0].submissionId)
    }

    // --- currentUserIsAdmin ---

    @Test
    fun `currentUserIsAdmin should be true when TokenManager returns true`() = runTest {
        every { tokenManager.isAdmin() } returns true

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.currentUserIsAdmin)
    }

    @Test
    fun `currentUserIsAdmin should be false when TokenManager returns false`() = runTest {
        every { tokenManager.isAdmin() } returns false

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.currentUserIsAdmin)
    }

    // --- banUser ---

    @Test
    fun `banUser success should update isBanned to true`() = runTest {
        val detail = testUserDetail.copy(isBanned = false)
        val banResponse = BanResponse(msg = "User banned", isBanned = true)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(detail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))
        coEvery { userRepository.banUser(testUserId) } returns ApiResult.Success(banResponse)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.userDetail!!.isBanned)

        viewModel.banUser()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isBanning)
        assertTrue(state.userDetail!!.isBanned)
        assertNull(state.error)
    }

    @Test
    fun `banUser failure should set error and clear isBanning`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))
        coEvery { userRepository.banUser(testUserId) } returns
            ApiResult.Error(AppError.Server(500, "Internal server error"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.banUser()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isBanning)
        assertEquals("Internal server error", state.error)
    }

    @Test
    fun `banUser network error should set error`() = runTest {
        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(testUserDetail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))
        coEvery { userRepository.banUser(testUserId) } returns
            ApiResult.Error(AppError.Network(java.io.IOException("timeout")))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.banUser()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isBanning)
        assertEquals("Network error. Check your connection and try again.", state.error)
    }

    // --- unbanUser ---

    @Test
    fun `unbanUser success should update isBanned to false`() = runTest {
        val detail = testUserDetail.copy(isBanned = true)
        val unbanResponse = BanResponse(msg = "User unbanned", isBanned = false)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(detail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))
        coEvery { userRepository.unbanUser(testUserId) } returns ApiResult.Success(unbanResponse)

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.userDetail!!.isBanned)

        viewModel.unbanUser()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isBanning)
        assertFalse(state.userDetail!!.isBanned)
        assertNull(state.error)
    }

    @Test
    fun `unbanUser failure should set error and clear isBanning`() = runTest {
        val detail = testUserDetail.copy(isBanned = true)

        coEvery { userRepository.getUserDetail(testUserId) } returns ApiResult.Success(detail)
        coEvery { userRepository.getUserSubmissions(testUserId, any(), any()) } returns
            ApiResult.Success(CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false))
        coEvery { userRepository.unbanUser(testUserId) } returns
            ApiResult.Error(AppError.Server(404, "User not found"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.unbanUser()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isBanning)
        assertEquals("User not found", state.error)
    }
}
