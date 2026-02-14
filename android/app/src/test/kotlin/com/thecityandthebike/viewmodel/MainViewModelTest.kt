package com.thecityandthebike.viewmodel

import android.net.Uri
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.CursorPaginatedSubmissions
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.ui.viewmodel.MainViewModel
import com.thecityandthebike.upload.UploadManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.thecityandthebike.upload.UploadState
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var uploadManager: UploadManager
    private val uploadStateFlow = MutableStateFlow<UploadState>(UploadState.Idle)
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        submissionRepository = mockk()
        uploadManager = mockk()
        every { uploadManager.state } returns uploadStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(submissionRepository, uploadManager)
    }

    @Test
    fun `loadSubmissions success should update state with submissions`() = runTest {
        val submissions = listOf(
            SubmissionResponse(
                submissionId = "1",
                userId = "user1",
                bikeQrId = "bike1",
                imageUrl = "http://example.com/image1.jpg"
            ),
            SubmissionResponse(
                submissionId = "2",
                userId = "user1",
                bikeQrId = "bike2",
                imageUrl = "http://example.com/image2.jpg"
            )
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertNull(viewModel.state.value.nextCursor)
        assertFalse(viewModel.state.value.hasMorePages)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `loadSubmissions failure should set error state`() = runTest {
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.submissions.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Network error. Check your connection and try again.", viewModel.state.value.error)
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Error(AppError.Server(500, "Error"))

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `loadMoreSubmissions should append items to existing list`() = runTest {
        val firstPage = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = "bike1"),
            SubmissionResponse(submissionId = "2", userId = "user1", bikeQrId = "bike2")
        )
        val firstPaginated = CursorPaginatedSubmissions(items = firstPage, nextCursor = "cursor-abc", hasMore = true)
        coEvery { submissionRepository.getSubmissions(limit = any(), cursor = null) } returns ApiResult.Success(firstPaginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertTrue(viewModel.state.value.hasMorePages)
        assertEquals("cursor-abc", viewModel.state.value.nextCursor)

        val secondPage = listOf(
            SubmissionResponse(submissionId = "3", userId = "user1", bikeQrId = "bike3"),
            SubmissionResponse(submissionId = "4", userId = "user1", bikeQrId = "bike4")
        )
        val secondPaginated = CursorPaginatedSubmissions(items = secondPage, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(limit = any(), cursor = "cursor-abc") } returns ApiResult.Success(secondPaginated)

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.state.value.submissions.size)
        assertEquals("3", viewModel.state.value.submissions[2].submissionId)
        assertEquals("4", viewModel.state.value.submissions[3].submissionId)
        assertFalse(viewModel.state.value.hasMorePages)
        assertFalse(viewModel.state.value.isLoadingMore)
    }

    @Test
    fun `loadMoreSubmissions should not load when already loading`() = runTest {
        val paginated = CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val fullPage = CursorPaginatedSubmissions(
            items = listOf(SubmissionResponse(submissionId = "1", userId = "u", bikeQrId = "b")),
            nextCursor = null, hasMore = false
        )
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(fullPage)

        viewModel.loadSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.hasMorePages)

        // This should be a no-op since hasMorePages is false
        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.submissions.size)
    }

    @Test
    fun `loadMoreSubmissions error should set error and keep existing submissions`() = runTest {
        val firstPage = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = "bike1"),
            SubmissionResponse(submissionId = "2", userId = "user1", bikeQrId = "bike2")
        )
        val firstPaginated = CursorPaginatedSubmissions(items = firstPage, nextCursor = "cursor-abc", hasMore = true)
        coEvery { submissionRepository.getSubmissions(limit = any(), cursor = null) } returns ApiResult.Success(firstPaginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertTrue(viewModel.state.value.hasMorePages)

        coEvery { submissionRepository.getSubmissions(limit = any(), cursor = "cursor-abc") } returns ApiResult.Error(AppError.Network(java.io.IOException("Network error")))

        viewModel.loadMoreSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertEquals("Network error. Check your connection and try again.", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoadingMore)
    }

    @Test
    fun `refreshSubmissions success should replace submissions list`() = runTest {
        val initialPage = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = "bike1")
        )
        val initialPaginated = CursorPaginatedSubmissions(items = initialPage, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(limit = any(), cursor = null) } returns ApiResult.Success(initialPaginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.submissions.size)
        assertEquals("1", viewModel.state.value.submissions[0].submissionId)

        val refreshedPage = listOf(
            SubmissionResponse(submissionId = "new-1", userId = "user1", bikeQrId = "bike1"),
            SubmissionResponse(submissionId = "new-2", userId = "user1", bikeQrId = "bike2")
        )
        val refreshedPaginated = CursorPaginatedSubmissions(items = refreshedPage, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(limit = any(), cursor = null) } returns ApiResult.Success(refreshedPaginated)

        viewModel.refreshSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertEquals("new-1", viewModel.state.value.submissions[0].submissionId)
        assertEquals("new-2", viewModel.state.value.submissions[1].submissionId)
        assertFalse(viewModel.state.value.isRefreshing)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `refreshSubmissions failure should set error and keep existing submissions`() = runTest {
        val initialPage = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = "bike1")
        )
        val initialPaginated = CursorPaginatedSubmissions(items = initialPage, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(limit = any(), cursor = null) } returns ApiResult.Success(initialPaginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.submissions.size)

        coEvery { submissionRepository.getSubmissions(limit = any(), cursor = null) } returns ApiResult.Error(AppError.Server(500, "Refresh failed"))

        viewModel.refreshSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.submissions.size)
        assertEquals("1", viewModel.state.value.submissions[0].submissionId)
        assertEquals("Refresh failed", viewModel.state.value.error)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `upload success should trigger refresh`() = runTest {
        val paginated = CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val refreshedPage = listOf(
            SubmissionResponse(submissionId = "new-1", userId = "user1", bikeQrId = "bike1")
        )
        val refreshedPaginated = CursorPaginatedSubmissions(items = refreshedPage, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(refreshedPaginated)

        uploadStateFlow.value = UploadState.Success
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.state.value.submissions.size)
        assertEquals("new-1", viewModel.state.value.submissions[0].submissionId)
    }

    @Test
    fun `upload error should set error state`() = runTest {
        val paginated = CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        uploadStateFlow.value = UploadState.Error("Upload failed")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Upload failed", viewModel.state.value.error)
    }

    @Test
    fun `removeSubmission should remove by id`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = "bike1"),
            SubmissionResponse(submissionId = "2", userId = "user1", bikeQrId = "bike2"),
            SubmissionResponse(submissionId = "3", userId = "user1", bikeQrId = "bike3")
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.state.value.submissions.size)

        viewModel.removeSubmission("2")

        assertEquals(2, viewModel.state.value.submissions.size)
        assertFalse(viewModel.state.value.submissions.any { it.submissionId == "2" })
    }

    @Test
    fun `removeSubmission should be no-op when id not found`() = runTest {
        val submissions = listOf(
            SubmissionResponse(submissionId = "1", userId = "user1", bikeQrId = "bike1"),
            SubmissionResponse(submissionId = "2", userId = "user1", bikeQrId = "bike2")
        )
        val paginated = CursorPaginatedSubmissions(items = submissions, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.removeSubmission("nonexistent")

        assertEquals(2, viewModel.state.value.submissions.size)
    }

    @Test
    fun `uploading state should set pendingUploadUri`() = runTest {
        val paginated = CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.pendingUploadUri)

        val uri = mockk<Uri>()
        uploadStateFlow.value = UploadState.Uploading(uri)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(uri, viewModel.state.value.pendingUploadUri)
    }

    @Test
    fun `upload success should clear pendingUploadUri after refresh`() = runTest {
        val paginated = CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val uri = mockk<Uri>()
        uploadStateFlow.value = UploadState.Uploading(uri)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(uri, viewModel.state.value.pendingUploadUri)

        val refreshedPage = listOf(
            SubmissionResponse(submissionId = "new-1", userId = "user1", bikeQrId = "bike1")
        )
        val refreshedPaginated = CursorPaginatedSubmissions(items = refreshedPage, nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(refreshedPaginated)

        uploadStateFlow.value = UploadState.Success
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.pendingUploadUri)
        assertEquals(1, viewModel.state.value.submissions.size)
    }

    @Test
    fun `upload error should clear pendingUploadUri`() = runTest {
        val paginated = CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val uri = mockk<Uri>()
        uploadStateFlow.value = UploadState.Uploading(uri)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(uri, viewModel.state.value.pendingUploadUri)

        uploadStateFlow.value = UploadState.Error("Upload failed")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.pendingUploadUri)
        assertEquals("Upload failed", viewModel.state.value.error)
    }

    @Test
    fun `pull to refresh should not clear pendingUploadUri`() = runTest {
        val paginated = CursorPaginatedSubmissions(items = emptyList(), nextCursor = null, hasMore = false)
        coEvery { submissionRepository.getSubmissions(any(), any()) } returns ApiResult.Success(paginated)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val uri = mockk<Uri>()
        uploadStateFlow.value = UploadState.Uploading(uri)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(uri, viewModel.state.value.pendingUploadUri)

        // Pull-to-refresh should keep pendingUploadUri
        viewModel.refreshSubmissions()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(uri, viewModel.state.value.pendingUploadUri)
    }
}
