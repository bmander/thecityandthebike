package com.thecityandthebike.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.ui.viewmodel.ImageDetailViewModel
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
class ImageDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var savedStateHandle: SavedStateHandle

    private val testSubmissionId = "test-submission-001"

    private val testSubmission = SubmissionResponse(
        submissionId = testSubmissionId,
        userId = "user1",
        bikeQrId = "BIKE-001",
        imageUrl = "https://example.com/image.jpg",
        username = "testuser"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        submissionRepository = mockk()
        tokenManager = mockk()
        savedStateHandle = SavedStateHandle(mapOf("submissionId" to testSubmissionId))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ImageDetailViewModel {
        return ImageDetailViewModel(submissionRepository, tokenManager, savedStateHandle)
    }

    @Test
    fun `loadSubmission success should update state with submission`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.submission)
        assertEquals(testSubmissionId, state.submission?.submissionId)
        assertNull(state.error)
    }

    @Test
    fun `loadSubmission failure should set error state`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Error(AppError.Server(404, "Not found"))
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.submission)
        assertEquals("Server error. Please try again later.", state.error)
    }

    @Test
    fun `loadSubmission network error should set error state`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.submission)
        assertEquals("Network error. Check your connection and try again.", state.error)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Error(AppError.Server(500, "Error"))
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `submissionId should come from SavedStateHandle`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        assertEquals(testSubmissionId, viewModel.submissionId)
    }

    @Test
    fun `isOwner should be true when logged-in user matches submission user`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isOwner)
    }

    @Test
    fun `isOwner should be false when logged-in user differs from submission user`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "other-user"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isOwner)
    }

    @Test
    fun `isOwner should be false when no user is logged in`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isOwner)
    }
}
