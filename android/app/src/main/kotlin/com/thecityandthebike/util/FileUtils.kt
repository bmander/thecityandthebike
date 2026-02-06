package com.thecityandthebike.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

fun createImageFileAndUri(context: Context): Pair<File, Uri> {
    val imageDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imageDir, "photo_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
    return imageFile to uri
}

fun uriToFile(contentResolver: ContentResolver, cacheDir: File, uri: Uri): File? {
    val inputStream = contentResolver.openInputStream(uri) ?: return null
    val file = File(cacheDir, "upload_${System.currentTimeMillis()}.jpg")
    inputStream.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return file
}

fun uriToFile(context: Context, uri: Uri): File? =
    uriToFile(context.contentResolver, context.cacheDir, uri)
