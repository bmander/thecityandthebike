package com.thecityandthebike.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.thecityandthebike.upload.UploadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PhotoPreviewViewModel @Inject constructor(
    private val uploadManager: UploadManager
) : ViewModel() {

    fun upload(uri: Uri, qrId: String, side: String? = null) {
        uploadManager.uploadAndCreateSubmission(uri, qrId, side)
    }
}
