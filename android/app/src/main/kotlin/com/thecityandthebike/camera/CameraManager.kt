package com.thecityandthebike.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.thecityandthebike.util.createImageUri

class CameraState(
    private val context: Context,
    private val currentPhotoUri: MutableState<Uri?>,
    private val cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    private val permissionLauncher: ManagedActivityResultLauncher<String, Boolean>
) {
    fun launchCamera() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                val uri = createImageUri(context)
                currentPhotoUri.value = uri
                cameraLauncher.launch(uri)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

@Composable
fun rememberCameraState(
    context: Context,
    currentPhotoUri: MutableState<Uri?>,
    onPhotoTaken: (Uri) -> Unit
): CameraState {
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri.value?.let { uri ->
                onPhotoTaken(uri)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            currentPhotoUri.value = uri
            cameraLauncher.launch(uri)
        }
    }

    return remember(context, currentPhotoUri, cameraLauncher, permissionLauncher) {
        CameraState(context, currentPhotoUri, cameraLauncher, permissionLauncher)
    }
}
