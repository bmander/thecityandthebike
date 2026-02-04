package com.thecityandthebike.viewmodel

import android.net.Uri
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.data.repository.SubmissionResult
import com.thecityandthebike.ui.viewmodel.MainViewModel
import io.mockk.coEvery
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

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        submissionRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

        viewModel = MainViewModel(submissionRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.state.value.submissions.size)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `loadSubmissions failure should set error state`() = runTest {
        coEvery { submissionRepository.getSubmissions() } returns SubmissionResult.Error("Network error")

        viewModel = MainViewModel(submissionRepository)
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

        viewModel = MainViewModel(submissionRepository)
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

        viewModel = MainViewModel(submissionRepository)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        viewModel.clearError()
        assertNull(viewModel.state.value.error)
    }
}
