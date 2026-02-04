package com.thecityandthebike.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thecityandthebike.camera.rememberCameraState
import com.thecityandthebike.ui.components.CameraFAB
import com.thecityandthebike.ui.components.ImageGrid
import com.thecityandthebike.ui.components.MenuButton

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val imageUris = remember { mutableStateListOf<Uri>() }
    val currentPhotoUri = remember { mutableStateOf<Uri?>(null) }

    val cameraState = rememberCameraState(
        context = context,
        currentPhotoUri = currentPhotoUri,
        onPhotoTaken = { uri -> imageUris.add(0, uri) }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ImageGrid(
            imageUris = imageUris,
            modifier = Modifier.fillMaxSize()
        )

        MenuButton(
            onClick = { /* TODO: Open settings menu */ },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        CameraFAB(
            onClick = { cameraState.launchCamera() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}
