package com.thecityandthebike.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.thecityandthebike.data.local.TokenManager
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.MessageResponse
import com.thecityandthebike.data.model.dto.ProcessedMaskResponse
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.data.repository.TagRepository
import com.thecityandthebike.ui.viewmodel.ImageDetailViewModel
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ImageDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var tagRepository: TagRepository
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

    private val testTag = TagResponse(
        tagId = "tag-1",
        submissionId = testSubmissionId,
        userId = "user1",
        imageUrl = "/uploads/images/tag.png",
        createdAt = "2026-01-01T00:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        submissionRepository = mockk()
        tagRepository = mockk()
        tokenManager = mockk()
        savedStateHandle = SavedStateHandle(mapOf("submissionId" to testSubmissionId))
        // Default: tags load empty
        coEvery { tagRepository.getTags(testSubmissionId) } returns ApiResult.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ImageDetailViewModel {
        return ImageDetailViewModel(submissionRepository, tagRepository, tokenManager, savedStateHandle)
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

    @Test
    fun `deleteSubmission success should set isDeleted true`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"
        coEvery { submissionRepository.deleteSubmission(testSubmissionId) } returns ApiResult.Success(
            com.thecityandthebike.data.model.dto.MessageResponse(msg = "Deleted")
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteSubmission()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isDeleted)
        assertFalse(state.isDeleting)
        assertNull(state.error)
    }

    @Test
    fun `deleteSubmission failure should set error`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"
        coEvery { submissionRepository.deleteSubmission(testSubmissionId) } returns ApiResult.Error(AppError.Server(500, "Error"))

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteSubmission()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isDeleted)
        assertFalse(state.isDeleting)
        assertEquals("Failed to delete submission", state.error)
    }

    @Test
    fun `deleteSubmission should set isDeleting while in progress`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"

        val deleteDeferred = kotlinx.coroutines.CompletableDeferred<ApiResult<com.thecityandthebike.data.model.dto.MessageResponse>>()
        coEvery { submissionRepository.deleteSubmission(testSubmissionId) } coAnswers {
            deleteDeferred.await()
        }

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteSubmission()
        testDispatcher.scheduler.advanceUntilIdle()

        // Coroutine is suspended waiting on deferred — isDeleting should be true
        assertTrue(viewModel.state.value.isDeleting)

        deleteDeferred.complete(ApiResult.Success(com.thecityandthebike.data.model.dto.MessageResponse(msg = "Deleted")))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isDeleting)
    }

    // --- Tag tests ---

    @Test
    fun `loadTags success populates tags in state`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.getTags(testSubmissionId) } returns ApiResult.Success(listOf(testTag))
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.tags.size)
        assertEquals("tag-1", viewModel.state.value.tags[0].tagId)
    }

    @Test
    fun `loadTags failure does not set error`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.getTags(testSubmissionId) } returns ApiResult.Error(AppError.Server(500, "Error"))
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.tags.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `enterTagMode sets isTagMode true`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.enterTagMode()

        assertTrue(viewModel.state.value.isTagMode)
    }

    @Test
    fun `exitTagMode sets isTagMode false`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.enterTagMode()
        assertTrue(viewModel.state.value.isTagMode)

        viewModel.exitTagMode()
        assertFalse(viewModel.state.value.isTagMode)
    }

    @Test
    fun `createTag success adds tag to list and exits tag mode`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.createTag(testSubmissionId, any(), any(), any(), any()) } returns ApiResult.Success(testTag)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.enterTagMode()

        val tempFile = File.createTempFile("test", ".png")
        tempFile.deleteOnExit()
        viewModel.createTag(tempFile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isTagMode)
        assertFalse(viewModel.state.value.isCreatingTag)
        assertEquals(1, viewModel.state.value.tags.size)
        assertEquals("tag-1", viewModel.state.value.tags[0].tagId)
    }

    @Test
    fun `createTag failure sets error and stays in tag mode`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.createTag(testSubmissionId, any(), any(), any(), any()) } returns ApiResult.Error(AppError.Server(500, "Error"))
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.enterTagMode()

        val tempFile = File.createTempFile("test", ".png")
        tempFile.deleteOnExit()
        viewModel.createTag(tempFile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isCreatingTag)
        assertEquals("Failed to create tag", viewModel.state.value.error)
    }

    @Test
    fun `createTag sets isCreatingTag while in progress`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"

        val deferred = kotlinx.coroutines.CompletableDeferred<ApiResult<TagResponse>>()
        coEvery { tagRepository.createTag(testSubmissionId, any(), any(), any(), any()) } coAnswers { deferred.await() }

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val tempFile = File.createTempFile("test", ".png")
        tempFile.deleteOnExit()
        viewModel.createTag(tempFile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isCreatingTag)

        deferred.complete(ApiResult.Success(testTag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isCreatingTag)
    }

    @Test
    fun `deleteTag success removes tag from list`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.getTags(testSubmissionId) } returns ApiResult.Success(listOf(testTag))
        coEvery { tagRepository.deleteTag("tag-1") } returns ApiResult.Success(MessageResponse(msg = "Tag deleted"))
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.tags.size)

        viewModel.deleteTag("tag-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.tags.isEmpty())
        assertFalse(viewModel.state.value.isDeletingTag)
    }

    @Test
    fun `deleteTag failure sets error and keeps tag`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.getTags(testSubmissionId) } returns ApiResult.Success(listOf(testTag))
        coEvery { tagRepository.deleteTag("tag-1") } returns ApiResult.Error(AppError.Auth(403, "forbidden"))
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTag("tag-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.tags.size)
        assertEquals("Failed to delete tag", viewModel.state.value.error)
    }

    @Test
    fun `isLoggedIn returns true when token manager has user id`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()

        assertTrue(viewModel.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false when token manager has no user id`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns null

        val viewModel = createViewModel()

        assertFalse(viewModel.isLoggedIn())
    }

    @Test
    fun `isTagOwner returns true when current user matches tag user`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()

        assertTrue(viewModel.isTagOwner(testTag))
    }

    @Test
    fun `isTagOwner returns false when current user differs from tag user`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "other-user"

        val viewModel = createViewModel()

        assertFalse(viewModel.isTagOwner(testTag))
    }

    // --- processMask tests ---

    private val testRing = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f), listOf(0f, 0f))
    private val testProcessedMaskResponse = ProcessedMaskResponse(
        ring = testRing,
        width = 200,
        height = 200
    )

    @Test
    fun `processMask success sets ring data in state`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.processMask(testSubmissionId, any()) } returns ApiResult.Success(testProcessedMaskResponse)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val tempFile = File.createTempFile("mask", ".png")
        tempFile.deleteOnExit()
        viewModel.processMask(tempFile)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProcessingMask)
        assertEquals(testRing, state.processedRing)
        assertEquals(200, state.processedMaskWidth)
        assertEquals(200, state.processedMaskHeight)
    }

    @Test
    fun `processMask failure sets error and clears processing flag`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.processMask(testSubmissionId, any()) } returns ApiResult.Error(AppError.Server(422, "No contours"))
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val tempFile = File.createTempFile("mask", ".png")
        tempFile.deleteOnExit()
        viewModel.processMask(tempFile)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isProcessingMask)
        assertNull(state.processedRing)
        assertEquals("Failed to process mask", state.error)
    }

    @Test
    fun `processMask sets isProcessingMask while in progress`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        every { tokenManager.getUserId() } returns "user1"

        val deferred = kotlinx.coroutines.CompletableDeferred<ApiResult<ProcessedMaskResponse>>()
        coEvery { tagRepository.processMask(testSubmissionId, any()) } coAnswers { deferred.await() }

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val tempFile = File.createTempFile("mask", ".png")
        tempFile.deleteOnExit()
        viewModel.processMask(tempFile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.isProcessingMask)

        deferred.complete(ApiResult.Success(testProcessedMaskResponse))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isProcessingMask)
    }

    // --- goBackToDrawing tests ---

    @Test
    fun `goBackToDrawing clears processed ring data`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.processMask(testSubmissionId, any()) } returns ApiResult.Success(testProcessedMaskResponse)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // First process a mask to populate ring data
        val tempFile = File.createTempFile("mask", ".png")
        tempFile.deleteOnExit()
        viewModel.processMask(tempFile)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.processedRing)

        // Go back to drawing
        viewModel.goBackToDrawing()

        val state = viewModel.state.value
        assertNull(state.processedRing)
        assertEquals(0, state.processedMaskWidth)
        assertEquals(0, state.processedMaskHeight)
    }

    // --- exitTagMode clears ring data ---

    @Test
    fun `exitTagMode clears processed ring data`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.processMask(testSubmissionId, any()) } returns ApiResult.Success(testProcessedMaskResponse)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.enterTagMode()

        val tempFile = File.createTempFile("mask", ".png")
        tempFile.deleteOnExit()
        viewModel.processMask(tempFile)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.processedRing)

        viewModel.exitTagMode()

        val state = viewModel.state.value
        assertFalse(state.isTagMode)
        assertNull(state.processedRing)
        assertEquals(0, state.processedMaskWidth)
        assertEquals(0, state.processedMaskHeight)
    }

    // --- createTag passes ring data ---

    @Test
    fun `createTag passes processed ring data to repository`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.processMask(testSubmissionId, any()) } returns ApiResult.Success(testProcessedMaskResponse)
        coEvery { tagRepository.createTag(testSubmissionId, any(), any(), any(), any()) } returns ApiResult.Success(testTag)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Process mask first to populate ring state
        val maskFile = File.createTempFile("mask", ".png")
        maskFile.deleteOnExit()
        viewModel.processMask(maskFile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Now create the tag
        val tagFile = File.createTempFile("tag", ".png")
        tagFile.deleteOnExit()
        viewModel.createTag(tagFile)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            tagRepository.createTag(
                testSubmissionId,
                any(),
                testRing,
                200,
                200
            )
        }
    }

    @Test
    fun `createTag passes null ring when no mask was processed`() = runTest {
        coEvery { submissionRepository.getSubmission(testSubmissionId) } returns ApiResult.Success(testSubmission)
        coEvery { tagRepository.createTag(testSubmissionId, any(), any(), any(), any()) } returns ApiResult.Success(testTag)
        every { tokenManager.getUserId() } returns "user1"

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val tagFile = File.createTempFile("tag", ".png")
        tagFile.deleteOnExit()
        viewModel.createTag(tagFile)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            tagRepository.createTag(
                testSubmissionId,
                any(),
                null,
                null,
                null
            )
        }
    }
}
