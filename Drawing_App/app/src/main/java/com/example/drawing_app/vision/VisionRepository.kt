/**
 * VisionRepository.kt
 *
 * Repository handling image scaling, encoding, API request execution, and result caching
 * for Cloud Vision analysis.
 *
 * Authors: Ryan Hoo (u1511875), Connor Kolodziej (u1458335), Luke Stansbury (u1422313)
 * Course: CS 4530
 */
package com.example.drawing_app.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Prepares images for the Vision API and remembers results so that reopening the analysis
 * screen does not send the same image again. Results are keyed by the drawing's file name,
 * which changes whenever the drawing is saved again.
 */
class VisionRepository(private val api: CloudVisionApi) {
    private val cache = ConcurrentHashMap<String, VisionAnalysis>()

    suspend fun analyze(bitmap: Bitmap, cacheKey: String, refresh: Boolean = false): VisionAnalysis {
        if (!refresh) cache[cacheKey]?.let { return it }
        val upload = withContext(Dispatchers.Default) { encodeForUpload(bitmap) }
        val result = withContext(Dispatchers.IO) { api.analyze(upload.jpeg, upload.width, upload.height) }
        if (cacheKey.isNotEmpty()) cache[cacheKey] = result
        return result
    }

    // Large photos are scaled down to keep uploads small. Bounding boxes are relative, so they still line up.
    private class Upload(val jpeg: ByteArray, val width: Int, val height: Int)

    private fun encodeForUpload(source: Bitmap): Upload {
        val longest = maxOf(source.width, source.height)
        val scaled = if (longest > MAX_UPLOAD_SIZE) {
            val factor = MAX_UPLOAD_SIZE.toFloat() / longest
            source.scale(
                (source.width * factor).toInt().coerceAtLeast(1),
                (source.height * factor).toInt().coerceAtLeast(1)
            )
        } else source
        // JPEG has no transparency, so transparent areas are drawn on white like the canvas.
        val opaque = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply {
            drawColor(Color.WHITE)
            drawBitmap(scaled, 0f, 0f, null)
        }
        val jpeg = ByteArrayOutputStream().use { output ->
            opaque.compress(Bitmap.CompressFormat.JPEG, 85, output)
            output.toByteArray()
        }
        return Upload(jpeg, opaque.width, opaque.height).also {
            if (scaled !== source) scaled.recycle()
            opaque.recycle()
        }
    }

    private companion object {
        const val MAX_UPLOAD_SIZE = 1024
    }
}
