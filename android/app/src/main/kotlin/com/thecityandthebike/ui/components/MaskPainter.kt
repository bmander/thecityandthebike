package com.thecityandthebike.ui.components

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
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
import androidx.compose.ui.graphics.drawscope.DrawScope
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

private const val STROKE_WIDTH = 40f
private val MASK_COLOR = Color(0x80000000)

data class StrokePoint(val x: Float, val y: Float)
data class StrokePath(val points: List<StrokePoint>)

class MaskPainterState {
    val strokes = mutableStateListOf<StrokePath>()
    var currentStroke by mutableStateOf<List<StrokePoint>>(emptyList())
    var canvasSize by mutableStateOf(IntSize.Zero)

    val hasStrokes: Boolean get() = strokes.isNotEmpty()

    /** All strokes including the one currently being drawn. */
    val allStrokes: List<StrokePath>
        get() = if (currentStroke.size >= 2) {
            strokes + StrokePath(currentStroke)
        } else {
            strokes.toList()
        }

    fun clear() {
        strokes.clear()
        currentStroke = emptyList()
    }

    /**
     * Export the mask as a black-and-white PNG: black background with white strokes
     * scaled to the target dimensions. No compositing with the original image.
     */
    suspend fun exportMask(context: Context, targetWidth: Int, targetHeight: Int): File? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (strokes.isEmpty() || canvasSize.width <= 0 || canvasSize.height <= 0) return@withContext null

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(android.graphics.Color.BLACK)

        val scaleX = targetWidth.toFloat() / canvasSize.width
        val scaleY = targetHeight.toFloat() / canvasSize.height

        val maskPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = STROKE_WIDTH * scaleX
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

        val tempFile = File(context.cacheDir, "mask_${System.currentTimeMillis()}.png")
        FileOutputStream(tempFile).use { out ->
            output.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        output.recycle()

        tempFile
    }

    /**
     * Export the composited image: original pixels where the mask was painted,
     * transparent elsewhere. Uses PorterDuff.SRC_IN compositing.
     */
    suspend fun exportComposited(context: Context, imageUri: Uri): File? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (strokes.isEmpty() || canvasSize.width <= 0 || canvasSize.height <= 0) return@withContext null

        val scheme = imageUri.scheme
        val inputStream = if (scheme == "http" || scheme == "https") {
            java.net.URL(imageUri.toString()).openStream()
        } else {
            context.contentResolver.openInputStream(imageUri)
        } ?: return@withContext null
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return@withContext null

        val outputWidth = originalBitmap.width
        val outputHeight = originalBitmap.height
        val output = createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val scaleX = outputWidth.toFloat() / canvasSize.width
        val scaleY = outputHeight.toFloat() / canvasSize.height

        val maskPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = STROKE_WIDTH * scaleX
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

        val imagePaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }

        val scaledOriginal = originalBitmap.scale(outputWidth, outputHeight, true)
        canvas.drawBitmap(scaledOriginal, 0f, 0f, imagePaint)

        if (scaledOriginal !== originalBitmap) {
            scaledOriginal.recycle()
        }
        originalBitmap.recycle()

        val tempFile = File(context.cacheDir, "tag_${System.currentTimeMillis()}.png")
        FileOutputStream(tempFile).use { out ->
            output.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        output.recycle()

        tempFile
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri ?: thumbnailUri)
                .crossfade(true)
                .build(),
            contentDescription = "Photo to tag",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        ScratchOffOverlay(state)
    }
}

@Composable
private fun ScratchOffOverlay(state: MaskPainterState) {
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
        drawRect(color = MASK_COLOR)

        for (stroke in state.allStrokes) {
            drawStroke(stroke, BlendMode.Clear)
        }
    }
}

private fun DrawScope.drawStroke(stroke: StrokePath, blendMode: BlendMode = BlendMode.SrcOver) {
    if (stroke.points.size < 2) return
    for (i in 1 until stroke.points.size) {
        drawLine(
            color = Color.Black,
            start = Offset(stroke.points[i - 1].x, stroke.points[i - 1].y),
            end = Offset(stroke.points[i].x, stroke.points[i].y),
            strokeWidth = STROKE_WIDTH,
            cap = StrokeCap.Round,
            blendMode = blendMode,
        )
    }
}

/**
 * Create a composited image from ring coordinates: draws a filled polygon from the ring,
 * then uses SRC_IN compositing with the original image to extract those pixels.
 */
suspend fun exportCompositedFromRing(
    context: Context,
    imageUri: Uri,
    ring: List<List<Float>>,
    maskWidth: Int,
    maskHeight: Int
): File? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    if (ring.size < 3) return@withContext null

    val scheme = imageUri.scheme
    val inputStream = if (scheme == "http" || scheme == "https") {
        java.net.URL(imageUri.toString()).openStream()
    } else {
        context.contentResolver.openInputStream(imageUri)
    } ?: return@withContext null
    val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    if (originalBitmap == null) return@withContext null

    val outputWidth = originalBitmap.width
    val outputHeight = originalBitmap.height
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val scaleX = outputWidth.toFloat() / maskWidth
    val scaleY = outputHeight.toFloat() / maskHeight

    val fillPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val path = Path()
    path.moveTo(ring[0][0] * scaleX, ring[0][1] * scaleY)
    for (i in 1 until ring.size) {
        path.lineTo(ring[i][0] * scaleX, ring[i][1] * scaleY)
    }
    path.close()
    canvas.drawPath(path, fillPaint)

    val imagePaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    val scaledOriginal = Bitmap.createScaledBitmap(originalBitmap, outputWidth, outputHeight, true)
    canvas.drawBitmap(scaledOriginal, 0f, 0f, imagePaint)

    if (scaledOriginal !== originalBitmap) {
        scaledOriginal.recycle()
    }
    originalBitmap.recycle()

    // Crop to the bounding box of the ring polygon
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE
    for (point in ring) {
        val px = point[0] * scaleX
        val py = point[1] * scaleY
        if (px < minX) minX = px
        if (py < minY) minY = py
        if (px > maxX) maxX = px
        if (py > maxY) maxY = py
    }
    val cropLeft = minX.toInt().coerceIn(0, outputWidth)
    val cropTop = minY.toInt().coerceIn(0, outputHeight)
    val cropRight = (maxX + 1).toInt().coerceIn(cropLeft, outputWidth)
    val cropBottom = (maxY + 1).toInt().coerceIn(cropTop, outputHeight)
    val cropWidth = cropRight - cropLeft
    val cropHeight = cropBottom - cropTop

    val cropped = if (cropWidth > 0 && cropHeight > 0) {
        Bitmap.createBitmap(output, cropLeft, cropTop, cropWidth, cropHeight)
    } else {
        output
    }

    val tempFile = File(context.cacheDir, "tag_${System.currentTimeMillis()}.png")
    FileOutputStream(tempFile).use { out ->
        cropped.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    if (cropped !== output) {
        cropped.recycle()
    }
    output.recycle()

    tempFile
}
