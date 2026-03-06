package com.thecityandthebike.upload

import android.content.Context
import android.net.Uri
import com.thecityandthebike.util.ImagePreparer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadQueue @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePreparer: ImagePreparer
) {
    private val queueDir = File(context.filesDir, "pending_uploads")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private val _pendingUploads = MutableStateFlow<List<PendingUpload>>(emptyList())
    val pendingUploads: StateFlow<List<PendingUpload>> = _pendingUploads.asStateFlow()

    init {
        queueDir.mkdirs()
        val uploads = scanDirectory()
        // Reset any UPLOADING items to QUEUED (app was killed mid-upload)
        val resetUploads = uploads.map { upload ->
            if (upload.status == PendingUploadStatus.UPLOADING) {
                val updated = upload.copy(status = PendingUploadStatus.QUEUED)
                writeMetadata(upload.id, updated)
                updated
            } else {
                upload
            }
        }
        _pendingUploads.value = resetUploads.sortedBy { it.createdAt }
    }

    suspend fun enqueue(
        uri: Uri,
        bikeQrId: String,
        side: String?,
        capturedDate: String
    ): PendingUpload? {
        val imageFile = imagePreparer.prepareImageFile(uri) ?: return null

        return mutex.withLock {
            val id = UUID.randomUUID().toString()
            val uploadDir = File(queueDir, id)
            uploadDir.mkdirs()

            val destFile = File(uploadDir, IMAGE_FILENAME)
            imageFile.copyTo(destFile, overwrite = true)
            imageFile.delete()

            val upload = PendingUpload(
                id = id,
                imageFileName = IMAGE_FILENAME,
                bikeQrId = bikeQrId,
                side = side,
                capturedDate = capturedDate,
                createdAt = System.currentTimeMillis()
            )
            writeMetadata(id, upload)
            _pendingUploads.value = (_pendingUploads.value + upload).sortedBy { it.createdAt }
            upload
        }
    }

    fun getImageFile(id: String): File {
        return File(File(queueDir, id), IMAGE_FILENAME)
    }

    suspend fun getAll(): List<PendingUpload> = mutex.withLock {
        _pendingUploads.value
    }

    suspend fun remove(id: String) = mutex.withLock {
        File(queueDir, id).deleteRecursively()
        _pendingUploads.value = _pendingUploads.value.filter { it.id != id }
    }

    suspend fun updateStatus(id: String, status: PendingUploadStatus) = mutex.withLock {
        val uploads = _pendingUploads.value.map { upload ->
            if (upload.id == id) {
                val updated = upload.copy(status = status)
                writeMetadata(id, updated)
                updated
            } else {
                upload
            }
        }
        _pendingUploads.value = uploads
    }

    private fun scanDirectory(): List<PendingUpload> {
        val dirs = queueDir.listFiles()?.filter { it.isDirectory } ?: return emptyList()
        return dirs.mapNotNull { dir ->
            val metadataFile = File(dir, "metadata.json")
            if (!metadataFile.exists()) {
                dir.deleteRecursively()
                return@mapNotNull null
            }
            try {
                json.decodeFromString<PendingUpload>(metadataFile.readText())
            } catch (_: Exception) {
                dir.deleteRecursively()
                null
            }
        }
    }

    companion object {
        private const val IMAGE_FILENAME = "image.jpg"
    }

    private fun writeMetadata(id: String, upload: PendingUpload) {
        val metadataFile = File(File(queueDir, id), "metadata.json")
        metadataFile.writeText(json.encodeToString(PendingUpload.serializer(), upload))
    }
}
