package com.thecityandthebike.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.thecityandthebike.R
import com.thecityandthebike.util.createImageFileAndUri
import com.thecityandthebike.util.cropToSquare
import com.thecityandthebike.util.stripMetadata
import java.util.concurrent.Executors

@Composable
fun PhotoCaptureScreen(
    side: String,
    onSideChanged: (String) -> Unit,
    onPhotoCaptured: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) permissionDenied = true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (permissionDenied) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Camera permission is required to take photos",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
        return
    }

    if (!hasCameraPermission) return

    var isCapturing by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()
            )
            .build()
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    LaunchedEffect(flashMode, camera) {
        camera?.cameraControl?.enableTorch(flashMode == ImageCapture.FLASH_MODE_ON)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Flash toggle + square camera preview
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.End
        ) {
            FlashToggleButton(
                flashMode = flashMode,
                onToggle = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                        else -> ImageCapture.FLASH_MODE_AUTO
                    }
                },
                modifier = Modifier.padding(end = 8.dp)
            )

            // Square camera preview with fender overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clipToBounds()
            ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("PhotoCaptureScreen", "Camera initialization failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview")
            )

            // Fender template overlay — loaded from drawable resource
            val fenderVector = ImageVector.vectorResource(R.drawable.fender_template)
            val fenderPaths = remember(fenderVector) {
                fenderVector.root.filterIsInstance<VectorPath>().map { vectorPath ->
                    PathParser().addPathNodes(vectorPath.pathData).toPath()
                }
            }
            Canvas(modifier = Modifier.fillMaxSize().testTag("template_overlay")) {
                val overlayHeight = size.height * 0.95f
                val scale = overlayHeight / fenderVector.viewportHeight
                val overlayWidth = fenderVector.viewportWidth * scale
                val offsetX = (size.width - overlayWidth) / 2
                val offsetY = (size.height - overlayHeight) / 2

                withTransform({
                    translate(left = offsetX, top = offsetY)
                    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                    if (side == "left") {
                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(fenderVector.viewportWidth / 2f, fenderVector.viewportHeight / 2f))
                    }
                }) {
                    fenderPaths.forEach { path ->
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.6f),
                            style = Stroke(width = 3.dp.toPx() / scale)
                        )
                    }
                }
            }
        }
        }

        // Side toggle buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val sideIcon = ImageVector.vectorResource(R.drawable.side_toggle_icon)
            Button(
                onClick = { onSideChanged("left") },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Left side" }
                    .testTag("side_left"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (side == "left") Color.White else Color.Gray
                ),
                contentPadding = PaddingValues(6.dp)
            ) {
                Image(
                    imageVector = sideIcon,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer(scaleX = -1f),
                    alpha = if (side == "left") 1f else 0.4f
                )
            }
            Button(
                onClick = { onSideChanged("right") },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Right side" }
                    .testTag("side_right"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (side == "right") Color.White else Color.Gray
                ),
                contentPadding = PaddingValues(6.dp)
            ) {
                Image(
                    imageVector = sideIcon,
                    contentDescription = null,
                    alpha = if (side == "right") 1f else 0.4f
                )
            }
        }

        // Capture button
        Button(
            onClick = {
                isCapturing = true
                val (file, uri) = createImageFileAndUri(context)
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imageCapture.takePicture(
                    outputOptions,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            try {
                                cropToSquare(file)
                            } catch (e: Exception) {
                                Log.e("PhotoCaptureScreen", "Square crop failed, using original", e)
                                try {
                                    stripMetadata(file)
                                } catch (stripEx: Exception) {
                                    Log.e("PhotoCaptureScreen", "Metadata strip also failed", stripEx)
                                }
                            }
                            ContextCompat.getMainExecutor(context).execute {
                                isCapturing = false
                                onPhotoCaptured(uri)
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("PhotoCaptureScreen", "Photo capture failed", exception)
                            ContextCompat.getMainExecutor(context).execute {
                                isCapturing = false
                            }
                        }
                    }
                )
            },
            enabled = !isCapturing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(72.dp)
                .semantics { contentDescription = "Capture photo" },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) { }

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

    }
}

@Composable
fun FlashToggleButton(
    flashMode: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
    ) {
        Icon(
            imageVector = when (flashMode) {
                ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                else -> Icons.Filled.FlashOff
            },
            contentDescription = when (flashMode) {
                ImageCapture.FLASH_MODE_AUTO -> "Flash auto"
                ImageCapture.FLASH_MODE_ON -> "Flash on"
                else -> "Flash off"
            },
            tint = Color.White
        )
    }
}
