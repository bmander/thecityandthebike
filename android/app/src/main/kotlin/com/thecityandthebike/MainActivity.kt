package com.thecityandthebike

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val imageUris = remember { mutableStateListOf<Uri>() }
                    val currentPhotoUri = remember { mutableStateOf<Uri?>(null) }

                    // Create a temp file and get URI for camera
                    fun createImageUri(): Uri {
                        val imageDir = File(context.cacheDir, "images").apply { mkdirs() }
                        val imageFile = File(imageDir, "photo_${System.currentTimeMillis()}.jpg")
                        return FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            imageFile
                        )
                    }

                    // Camera launcher
                    val cameraLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.TakePicture()
                    ) { success ->
                        if (success) {
                            currentPhotoUri.value?.let { uri ->
                                imageUris.add(0, uri)
                            }
                        }
                    }

                    // Permission launcher
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) {
                            val uri = createImageUri()
                            currentPhotoUri.value = uri
                            cameraLauncher.launch(uri)
                        }
                    }

                    fun launchCamera() {
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                val uri = createImageUri()
                                currentPhotoUri.value = uri
                                cameraLauncher.launch(uri)
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Image grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(imageUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Captured image",
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .background(Color.LightGray),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Menu button (top-left)
                        IconButton(
                            onClick = { /* TODO: Open settings menu */ },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Add button (top-right)
                        FloatingActionButton(
                            onClick = { launchCamera() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add image"
                            )
                        }
                    }
                }
            }
        }
    }
}
