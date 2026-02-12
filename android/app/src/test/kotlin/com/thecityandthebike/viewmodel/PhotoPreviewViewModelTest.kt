package com.thecityandthebike.viewmodel

import android.net.Uri
import com.thecityandthebike.ui.viewmodel.PhotoPreviewViewModel
import com.thecityandthebike.upload.UploadManager
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PhotoPreviewViewModelTest {

    private val uploadManager: UploadManager = mockk(relaxed = true)

    private fun createViewModel(): PhotoPreviewViewModel {
        return PhotoPreviewViewModel(uploadManager)
    }

    @Test
    fun `upload should delegate to uploadManager`() {
        val uri = mockk<Uri>()
        val qrId = "BIKE-001"

        val viewModel = createViewModel()
        viewModel.upload(uri, qrId)

        verify(exactly = 1) { uploadManager.uploadAndCreateSubmission(uri, qrId) }
    }
}
