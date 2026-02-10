package com.thecityandthebike.viewmodel

import android.net.Uri
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UploadResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.data.repository.SubmissionResult
import com.thecityandthebike.ui.viewmodel.MainViewModel
import com.thecityandthebike.util.ImagePreparer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
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
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var imagePreparer: ImagePreparer
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        submissionRepository = mockk()
        imagePreparer = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(submissionRepository, imagePreparer)
    }

    @Test
    fun `loadSubmissions success should update state with submissions`() = runTest {
        val submissions = listOf(
            SubmissionResponse(
                submissionId = "1",
                userId = "user1",
                bikeQrId = "bike1",
                imageUrlOriginal = "http://example.com/image1.jpg"
            ),
            SubmissionResponse(
                submissionId = "2",
                userId = "user1",
                bikeQrId = "bike2",
                imageUrlOriginal = "http://example.com/image2.jpg"
            )
        )
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Success(submissions)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `loadSubmissions failure should set error state`() = runTest {
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Error("Network error")

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.submissions.isEmpty())
        assertFalse(viewModel.state.value.isLoading)
        assertEquals("Network error", viewModel.state.value.error)
    }

    @Test
    fun `addLocalImage should prepend image to local images list`() = runTest {
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Success(emptyList())

        mockkStatic(Uri::class)
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addLocalImage(uri1)
        assertEquals(1, viewModel.state.value.localImages.size)
        assertEquals(uri1, viewModel.state.value.localImages[0])

        viewModel.addLocalImage(uri2)
        assertEquals(2, viewModel.state.value.localImages.size)
        assertEquals(uri2, viewModel.state.value.localImages[0])
        assertEquals(uri1, viewModel.state.value.localImages[1])
    }

    @Test
    fun `clearError should reset error state`() = runTest {
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Error("Error")

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `upload success should add submission to state`() = runTest {
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Success(emptyList())

        val uri = mockk<Uri>()
        val imageFile = mockk<File>(relaxed = true)
        coEvery { imagePreparer.prepareImageFile(uri) } returns imageFile

        val uploadResponse = UploadResponse(
            url = "http://example.com/uploaded.jpg",
            filename = "uploaded.jpg",
            thumbnailUrl = "http://example.com/uploaded_thumb.jpg"
        )
        coEvery { submissionRepository.uploadImage(imageFile) } returns SubmissionResult.Success(uploadResponse)

        val submissionResponse = SubmissionResponse(
            submissionId = "new-1",
            userId = "user1",
            bikeQrId = "bike1",
            imageUrlOriginal = "http://example.com/uploaded.jpg"
        )
        coEvery { submissionRepository.createSubmission(any()) } returns SubmissionResult.Success(submissionResponse)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addLocalImage(uri)
        viewModel.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isUploading)
        assertEquals(1, viewModel.state.value.submissions.size)
        assertEquals("new-1", viewModel.state.value.submissions[0].submissionId)
        assertFalse(viewModel.state.value.localImages.contains(uri))
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `upload failure should set error state`() = runTest {
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Success(emptyList())

        val uri = mockk<Uri>()
        val imageFile = mockk<File>(relaxed = true)
        coEvery { imagePreparer.prepareImageFile(uri) } returns imageFile

        coEvery { submissionRepository.uploadImage(imageFile) } returns SubmissionResult.Error("Upload failed")

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addLocalImage(uri)
        viewModel.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isUploading)
        assertEquals("Upload failed", viewModel.state.value.error)
        assertTrue(viewModel.state.value.submissions.isEmpty())
    }

    @Test
    fun `submission creation failure should set error state`() = runTest {
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Success(emptyList())

        val uri = mockk<Uri>()
        val imageFile = mockk<File>(relaxed = true)
        coEvery { imagePreparer.prepareImageFile(uri) } returns imageFile

        val uploadResponse = UploadResponse(
            url = "http://example.com/uploaded.jpg",
            filename = "uploaded.jpg",
            thumbnailUrl = "http://example.com/uploaded_thumb.jpg"
        )
        coEvery { submissionRepository.uploadImage(imageFile) } returns SubmissionResult.Success(uploadResponse)
        coEvery { submissionRepository.createSubmission(any()) } returns SubmissionResult.Error("Create failed")

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addLocalImage(uri)
        viewModel.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isUploading)
        assertEquals("Create failed", viewModel.state.value.error)
        assertTrue(viewModel.state.value.submissions.isEmpty())
    }

    @Test
    fun `file conversion failure should set error and not attempt upload`() = runTest {
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Success(emptyList())

        val uri = mockk<Uri>()
        coEvery { imagePreparer.prepareImageFile(uri) } returns null

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addLocalImage(uri)
        viewModel.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isUploading)
        assertEquals("Could not read image file", viewModel.state.value.error)
        coVerify(exactly = 0) { submissionRepository.uploadImage(any()) }
    }
}
