package com.thecityandthebike.data.processing

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

data class ProcessedMaskResult(
    val ring: List<List<Float>>,
    val width: Int,
    val height: Int
)

@Singleton
class MaskProcessor @Inject constructor() {

    suspend fun process(imageFile: File): ProcessedMaskResult = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: throw IllegalArgumentException("Could not decode image")

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()

        processPixels(pixels, width, height)
    }

    internal fun processPixels(pixels: IntArray, width: Int, height: Int): ProcessedMaskResult {
        val binary = threshold(pixels, width, height)
        val contours = traceContours(binary, width, height)

        if (contours.isEmpty()) {
            throw IllegalArgumentException("No contours found in mask")
        }

        val largest = contours.maxBy { shoelaceArea(it) }
        val simplified = douglasPeucker(largest, 2.0)
        val ring = closeRing(simplified)

        return ProcessedMaskResult(
            ring = ring.map { listOf(it.first, it.second) },
            width = width,
            height = height
        )
    }

    internal fun threshold(pixels: IntArray, width: Int, height: Int): BooleanArray {
        val binary = BooleanArray(width * height)
        for (i in pixels.indices) {
            val blue = pixels[i] and 0xFF
            binary[i] = blue > 127
        }
        return binary
    }

    internal fun traceContours(
        binary: BooleanArray,
        width: Int,
        height: Int
    ): List<List<Pair<Float, Float>>> {
        val visited = BooleanArray(width * height)
        val contours = mutableListOf<List<Pair<Float, Float>>>()

        // Moore neighborhood: 8 directions clockwise from east
        // E, SE, S, SW, W, NW, N, NE
        val dx = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
        val dy = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!binary[y * width + x]) continue
                if (visited[y * width + x]) continue
                // Only start tracing on left-edge pixels (external contour entry)
                if (x > 0 && binary[y * width + (x - 1)]) continue

                val contour = traceMoore(binary, visited, width, height, x, y, dx, dy)
                if (contour.size >= 3) {
                    contours.add(contour)
                }
            }
        }

        return contours
    }

    private fun traceMoore(
        binary: BooleanArray,
        visited: BooleanArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        dx: IntArray,
        dy: IntArray
    ): List<Pair<Float, Float>> {
        val rawPoints = mutableListOf<Pair<Int, Int>>()
        rawPoints.add(Pair(startX, startY))
        visited[startY * width + startX] = true

        var cx = startX
        var cy = startY
        // We "arrived" at startX by scanning east (direction 0); the background pixel is to the west
        var dir = 0

        var steps = 0
        val maxSteps = width * height * 2

        while (steps < maxSteps) {
            steps++
            // Search the 8 neighbors starting from the backtrack direction
            var found = false
            val searchStart = (dir + 5) % 8 // backtrack direction + 1

            for (i in 0 until 8) {
                val d = (searchStart + i) % 8
                val nx = cx + dx[d]
                val ny = cy + dy[d]

                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue

                if (binary[ny * width + nx]) {
                    cx = nx
                    cy = ny
                    dir = d
                    visited[cy * width + cx] = true

                    if (cx == startX && cy == startY) {
                        // Back to start — contour complete
                        return compressContour(rawPoints)
                    }

                    rawPoints.add(Pair(cx, cy))
                    found = true
                    break
                }
            }

            if (!found) {
                // Isolated pixel or dead end
                break
            }
        }

        return compressContour(rawPoints)
    }

    /** Compress collinear sequences to just their endpoints (like CHAIN_APPROX_SIMPLE). */
    private fun compressContour(points: List<Pair<Int, Int>>): List<Pair<Float, Float>> {
        if (points.size <= 2) return points.map { Pair(it.first.toFloat(), it.second.toFloat()) }

        val result = mutableListOf<Pair<Float, Float>>()
        result.add(Pair(points[0].first.toFloat(), points[0].second.toFloat()))

        for (i in 1 until points.size - 1) {
            val prevDx = points[i].first - points[i - 1].first
            val prevDy = points[i].second - points[i - 1].second
            val nextDx = points[i + 1].first - points[i].first
            val nextDy = points[i + 1].second - points[i].second

            if (prevDx != nextDx || prevDy != nextDy) {
                result.add(Pair(points[i].first.toFloat(), points[i].second.toFloat()))
            }
        }

        result.add(Pair(points.last().first.toFloat(), points.last().second.toFloat()))
        return result
    }

    /** Shoelace formula for polygon area (absolute value). */
    internal fun shoelaceArea(points: List<Pair<Float, Float>>): Double {
        var area = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += points[i].first * points[j].second
            area -= points[j].first * points[i].second
        }
        return abs(area) / 2.0
    }

    /** Iterative (stack-based) Douglas-Peucker simplification. */
    internal fun douglasPeucker(
        points: List<Pair<Float, Float>>,
        tolerance: Double
    ): List<Pair<Float, Float>> {
        if (points.size <= 2) return points

        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.size - 1] = true

        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(Pair(0, points.size - 1))

        while (stack.isNotEmpty()) {
            val (start, end) = stack.removeLast()
            var maxDist = 0.0
            var maxIdx = start

            for (i in start + 1 until end) {
                val dist = perpendicularDistance(points[i], points[start], points[end])
                if (dist > maxDist) {
                    maxDist = dist
                    maxIdx = i
                }
            }

            if (maxDist > tolerance) {
                keep[maxIdx] = true
                if (maxIdx - start > 1) stack.addLast(Pair(start, maxIdx))
                if (end - maxIdx > 1) stack.addLast(Pair(maxIdx, end))
            }
        }

        return points.filterIndexed { index, _ -> keep[index] }
    }

    internal fun perpendicularDistance(
        point: Pair<Float, Float>,
        lineStart: Pair<Float, Float>,
        lineEnd: Pair<Float, Float>
    ): Double {
        val dx = lineEnd.first - lineStart.first
        val dy = lineEnd.second - lineStart.second
        val lenSq = (dx * dx + dy * dy).toDouble()

        if (lenSq == 0.0) {
            val px = (point.first - lineStart.first).toDouble()
            val py = (point.second - lineStart.second).toDouble()
            return sqrt(px * px + py * py)
        }

        val num = abs(
            dy * (point.first - lineStart.first) -
            dx * (point.second - lineStart.second)
        ).toDouble()

        return num / sqrt(lenSq)
    }

    private fun closeRing(points: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        if (points.isEmpty()) return points
        if (points.first() == points.last()) return points
        return points + points.first()
    }
}
