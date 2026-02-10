package com.thecityandthebike.util

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImagePreparer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun prepareImageFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        val file = uriToFile(context, uri) ?: return@withContext null
        try {
            stripMetadata(file)
        } catch (_: Exception) {
            // Best-effort; proceed even if stripping fails
        }
        file
    }
}
