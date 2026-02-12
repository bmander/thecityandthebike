package com.thecityandthebike.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream

data class StrokePoint(val x: Float, val y: Float)
data class StrokePath(val points: List<StrokePoint>)

class MaskPainterState {
    val strokes = mutableStateListOf<StrokePath>()
    var currentStroke by mutableStateOf<List<StrokePoint>>(emptyList())
    var canvasSize by mutableStateOf(IntSize.Zero)

    val hasStrokes: Boolean get() = strokes.isNotEmpty()

    fun clear() {
        strokes.clear()
        currentStroke = emptyList()
    }
}

@Composable
fun rememberMaskPainterState(): MaskPainterState {
    return remember { MaskPainterState() }
}

@Composable
fun MaskPainter(
    imageUri: Uri?,
    thumbnailUri: Uri?,
    state: MaskPainterState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { state.canvasSize = it }
    ) {
        // Show original image underneath
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri ?: thumbnailUri)
                .crossfade(true)
                .build(),
            contentDescription = "Photo to tag",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Scratch-off mask overlay: black semi-transparent mask with strokes cut out
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            state.currentStroke = listOf(StrokePoint(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            state.currentStroke = state.currentStroke + StrokePoint(
                                change.position.x,
                                change.position.y
                            )
                        },
                        onDragEnd = {
                            if (state.currentStroke.isNotEmpty()) {
                                state.strokes.add(StrokePath(state.currentStroke.toList()))
                                state.currentStroke = emptyList()
                            }
                        },
                        onDragCancel = {
                            state.currentStroke = emptyList()
                        }
                    )
                }
        ) {
            val strokeWidth = 40f

            // Draw the semi-transparent black mask over the entire image
            drawRect(color = Color(0x80000000))

            // Scratch off the mask where the user has drawn (BlendMode.Clear
            // punches transparent holes through the offscreen layer)
            for (stroke in state.strokes) {
                if (stroke.points.size >= 2) {
                    for (i in 1 until stroke.points.size) {
                        drawLine(
                            color = Color.Black,
                            start = Offset(stroke.points[i - 1].x, stroke.points[i - 1].y),
                            end = Offset(stroke.points[i].x, stroke.points[i].y),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                            blendMode = BlendMode.Clear
                        )
                    }
                }
            }

            // Scratch off current stroke in progress
            if (state.currentStroke.size >= 2) {
                for (i in 1 until state.currentStroke.size) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(state.currentStroke[i - 1].x, state.currentStroke[i - 1].y),
                        end = Offset(state.currentStroke[i].x, state.currentStroke[i].y),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                        blendMode = BlendMode.Clear
                    )
                }
            }
        }
    }
}

/**
 * Export the composited image: original pixels where the mask was painted,
 * transparent elsewhere. Uses PorterDuff.SRC_IN compositing.
 */
fun exportComposited(
    context: Context,
    imageUri: Uri,
    strokes: List<StrokePath>,
    canvasWidth: Int,
    canvasHeight: Int
): File? {
    if (strokes.isEmpty() || canvasWidth <= 0 || canvasHeight <= 0) return null

    // Load original image
    val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    if (originalBitmap == null) return null

    // Create output bitmap at the original image size
    val outputWidth = originalBitmap.width
    val outputHeight = originalBitmap.height
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    // Scale factor from canvas coordinates to image coordinates
    val scaleX = outputWidth.toFloat() / canvasWidth
    val scaleY = outputHeight.toFloat() / canvasHeight

    // Draw the mask (white strokes on transparent background)
    val maskPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 40f * scaleX  // Scale stroke width proportionally
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    for (stroke in strokes) {
        if (stroke.points.size >= 2) {
            val path = Path()
            path.moveTo(stroke.points[0].x * scaleX, stroke.points[0].y * scaleY)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].x * scaleX, stroke.points[i].y * scaleY)
            }
            canvas.drawPath(path, maskPaint)
        }
    }

    // Apply PorterDuff.SRC_IN: keep original image pixels only where mask is opaque
    val imagePaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    // Scale the original bitmap to output size if needed
    val scaledOriginal = Bitmap.createScaledBitmap(originalBitmap, outputWidth, outputHeight, true)
    canvas.drawBitmap(scaledOriginal, 0f, 0f, imagePaint)

    if (scaledOriginal !== originalBitmap) {
        scaledOriginal.recycle()
    }
    originalBitmap.recycle()

    // Save to temp file
    val tempFile = File(context.cacheDir, "tag_${System.currentTimeMillis()}.png")
    FileOutputStream(tempFile).use { out ->
        output.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    output.recycle()

    return tempFile
}
