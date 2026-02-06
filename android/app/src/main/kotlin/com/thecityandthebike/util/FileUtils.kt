package com.thecityandthebike.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

fun cropToSquare(file: File): File {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val size = minOf(bitmap.width, bitmap.height)
    val x = (bitmap.width - size) / 2
    val y = (bitmap.height - size) / 2
    val cropped = Bitmap.createBitmap(bitmap, x, y, size, size)
    file.outputStream().use { out ->
        cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    if (cropped !== bitmap) cropped.recycle()
    bitmap.recycle()
    return file
}
