/**
 * VisionModels.kt
 *
 * Data structures representing bounding boxes, detected objects, labels, and analysis result summaries.
 *
 * Authors: Ryan Hoo (u1511875), Connor Kolodziej (u1458335), Luke Stansbury (u1422313)
 * Course: CS 4530
 */
package com.example.drawing_app.vision

/** A bounding box with edges given as fractions (0..1) of the image width and height. */
data class NormalizedBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

/**
 * Something found at a location in the image: an object or a face.
 * [confidence] is between 0 and 1, and [details] holds extra facts such as face expressions.
 */
data class DetectedObject(
    val label: String,
    val category: String,
    val confidence: Float,
    val box: NormalizedBox,
    val details: List<String> = emptyList()
)

/** A label that describes the whole image, such as "Cat" or "Sketch". */
data class ImageLabel(val name: String, val confidence: Float)

/** Everything the app shows on the analysis screen. */
data class VisionAnalysis(
    val objects: List<DetectedObject>,
    val labels: List<ImageLabel>,
    val detectedText: List<String>
)

/** A failure with a message that is safe to show to the user. */
class VisionException(message: String, cause: Throwable? = null) : Exception(message, cause)
