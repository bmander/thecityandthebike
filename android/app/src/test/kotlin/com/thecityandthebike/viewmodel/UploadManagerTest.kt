package com.thecityandthebike.viewmodel

import android.net.Uri
import com.thecityandthebike.data.model.ApiResult
import com.thecityandthebike.data.model.AppError
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UploadResponse
import com.thecityandthebike.data.repository.SubmissionRepository
import com.thecityandthebike.upload.UploadManager
import com.thecityandthebike.upload.UploadState
import com.thecityandthebike.util.ImagePreparer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UploadManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var submissionRepository: SubmissionRepository
    private lateinit var imagePreparer: ImagePreparer

    @Before
    fun setup() {
        submissionRepository = mockk()
        imagePreparer = mockk()
    }

    private fun createUploadManager(): UploadManager {
        return UploadManager(submissionRepository, imagePreparer, testScope)
    }

    @Test
    fun `upload success should transition through states`() = testScope.runTest {
        val uri = mockk<Uri>()
        val imageFile = mockk<File>(relaxed = true)
        coEvery { imagePreparer.prepareImageFile(uri) } returns imageFile

        val uploadResponse = UploadResponse(
            url = "http://example.com/uploaded.jpg",
            filename = "uploaded.jpg",
            thumbnailUrl = "http://example.com/uploaded_thumb.jpg"
        )
        coEvery { submissionRepository.uploadImage(imageFile) } returns ApiResult.Success(uploadResponse)

        val submissionResponse = SubmissionResponse(
            submissionId = "new-1",
            userId = "user1",
            bikeQrId = "bike1",
            imageUrl = "http://example.com/uploaded.jpg"
        )
        coEvery { submissionRepository.createSubmission(any()) } returns ApiResult.Success(submissionResponse)

        val uploadManager = createUploadManager()
        assertEquals(UploadState.Idle, uploadManager.state.value)

        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UploadState.Success, uploadManager.state.value)
    }

    @Test
    fun `upload failure should set error state`() = testScope.runTest {
        val uri = mockk<Uri>()
        val imageFile = mockk<File>(relaxed = true)
        coEvery { imagePreparer.prepareImageFile(uri) } returns imageFile
        coEvery { submissionRepository.uploadImage(imageFile) } returns ApiResult.Error(AppError.Server(500, "Upload failed"))

        val uploadManager = createUploadManager()
        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = uploadManager.state.value
        assertTrue(state is UploadState.Error)
        assertEquals("Server error. Please try again later.", (state as UploadState.Error).message)
    }

    @Test
    fun `submission creation failure should set error state`() = testScope.runTest {
        val uri = mockk<Uri>()
        val imageFile = mockk<File>(relaxed = true)
        coEvery { imagePreparer.prepareImageFile(uri) } returns imageFile

        val uploadResponse = UploadResponse(
            url = "http://example.com/uploaded.jpg",
            filename = "uploaded.jpg",
            thumbnailUrl = "http://example.com/uploaded_thumb.jpg"
        )
        coEvery { submissionRepository.uploadImage(imageFile) } returns ApiResult.Success(uploadResponse)
        coEvery { submissionRepository.createSubmission(any()) } returns ApiResult.Error(AppError.Server(500, "Create failed"))

        val uploadManager = createUploadManager()
        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = uploadManager.state.value
        assertTrue(state is UploadState.Error)
    }

    @Test
    fun `file conversion failure should set error and not attempt upload`() = testScope.runTest {
        val uri = mockk<Uri>()
        coEvery { imagePreparer.prepareImageFile(uri) } returns null

        val uploadManager = createUploadManager()
        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = uploadManager.state.value
        assertTrue(state is UploadState.Error)
        assertEquals("Could not read image file", (state as UploadState.Error).message)
        coVerify(exactly = 0) { submissionRepository.uploadImage(any()) }
    }

    @Test
    fun `clearError should reset to idle`() = testScope.runTest {
        val uri = mockk<Uri>()
        coEvery { imagePreparer.prepareImageFile(uri) } returns null

        val uploadManager = createUploadManager()
        uploadManager.uploadAndCreateSubmission(uri, "bike1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(uploadManager.state.value is UploadState.Error)

        uploadManager.clearError()
        assertEquals(UploadState.Idle, uploadManager.state.value)
    }
}
