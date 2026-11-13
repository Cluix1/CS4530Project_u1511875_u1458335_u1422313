package com.example.drawing_app.vision

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.io.encoding.Base64

internal val VisionJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** Builds the Ktor client used for Vision API requests. Tests pass a mock engine. */
fun createVisionHttpClient(engine: HttpClientEngine = OkHttp.create()): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(VisionJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 60_000
    }
}

/**
 * Sends images to the Google Cloud Vision API (images:annotate) and turns the reply into a [VisionAnalysis].
 * One request asks for object localization, labels, text and faces.
 */
class CloudVisionApi(private val client: HttpClient, private val apiKey: String) {

    /** [width] and [height] are the pixel size of [image]; face boxes come back in pixels. */
    suspend fun analyze(image: ByteArray, width: Int, height: Int): VisionAnalysis {
        if (apiKey.isBlank()) {
            throw VisionException("No Cloud Vision API key is set. Add CLOUD_VISION_API_KEY to local.properties and rebuild the app.")
        }
        val request = BatchAnnotateRequest(
            listOf(
                AnnotateImageRequest(
                    image = ImageContent(Base64.encode(image)),
                    features = listOf(
                        Feature("OBJECT_LOCALIZATION", 20),
                        Feature("LABEL_DETECTION", 10),
                        Feature("TEXT_DETECTION", 1),
                        Feature("FACE_DETECTION", 10)
                    )
                )
            )
        )

        val response = try {
            client.post(ENDPOINT) {
                // The key goes in a header rather than the URL so it does not appear in logs.
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        } catch (e: IOException) {
            throw VisionException("Could not reach the Vision API. Check your internet connection and try again.", e)
        }

        if (!response.status.isSuccess()) {
            val detail = try {
                VisionJson.decodeFromString<ErrorResponse>(response.bodyAsText()).error?.message
            } catch (e: Exception) {
                null
            }
            throw errorFor(response.status, detail)
        }

        val body = try {
            response.body<BatchAnnotateResponse>()
        } catch (e: SerializationException) {
            throw VisionException("The Vision API sent a response the app could not read.", e)
        } catch (e: IOException) {
            throw VisionException("The connection to the Vision API was interrupted. Please try again.", e)
        }
        val result = body.responses.firstOrNull()
            ?: throw VisionException("The Vision API did not return any results for this image.")
        result.error?.let { throw VisionException("The Vision API could not analyze this image: ${it.message ?: "unknown error"}") }
        return toAnalysis(result, width, height)
    }

    private fun errorFor(status: HttpStatusCode, detail: String?): VisionException {
        val message = when {
            status == HttpStatusCode.TooManyRequests ->
                "The Vision API is receiving too many requests. Wait a moment and try again."
            detail?.contains("API key", ignoreCase = true) == true || status == HttpStatusCode.Unauthorized ->
                "The Cloud Vision API key was rejected. Check CLOUD_VISION_API_KEY in local.properties."
            detail?.contains("billing", ignoreCase = true) == true ->
                "Billing is not enabled for this key's Google Cloud project. Link a billing account to use the Vision API (the free tier still requires one)."
            status == HttpStatusCode.Forbidden ->
                "This key cannot use the Cloud Vision API. Make sure the API is enabled for its project."
            status.value >= 500 -> "The Vision API is unavailable right now. Please try again later."
            else -> "The Vision API could not analyze this image (error ${status.value})."
        }
        return VisionException(message)
    }

    companion object {
        private const val ENDPOINT = "https://vision.googleapis.com/v1/images:annotate"

        private val LIKELIHOOD_RANK = mapOf("POSSIBLE" to 1, "LIKELY" to 2, "VERY_LIKELY" to 3)
        private val EXPRESSIONS = listOf("Joy", "Sorrow", "Anger", "Surprise")

        internal fun toAnalysis(result: AnnotateImageResponse, width: Int, height: Int): VisionAnalysis {
            val objects = result.localizedObjectAnnotations.mapNotNull { obj ->
                val box = boxFrom(obj.boundingPoly.normalizedVertices.map { it.x to it.y }) ?: return@mapNotNull null
                DetectedObject(obj.name.trim(), "Object", obj.score.toFloat().coerceIn(0f, 1f), box)
            }
            val faces = result.faceAnnotations.mapNotNull { face ->
                val box = pixelBox(face.fdBoundingPoly ?: face.boundingPoly, width, height) ?: return@mapNotNull null
                DetectedObject("Face", "Face", face.detectionConfidence.toFloat().coerceIn(0f, 1f), box, faceDetails(face))
            }
            return VisionAnalysis(
                objects = (objects + faces).filter { it.label.isNotEmpty() }.sortedByDescending { it.confidence },
                labels = result.labelAnnotations
                    .filter { it.description.isNotBlank() }
                    .map { ImageLabel(it.description.trim(), it.score.toFloat().coerceIn(0f, 1f)) },
                // The first text annotation holds all text in the image; the rest are single words.
                detectedText = result.textAnnotations.firstOrNull()?.description
                    ?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }
                    .orEmpty()
            )
        }

        private fun faceDetails(face: FaceAnnotation): List<String> {
            val likelihoods = listOf(face.joyLikelihood, face.sorrowLikelihood, face.angerLikelihood, face.surpriseLikelihood)
            return EXPRESSIONS.zip(likelihoods).mapNotNull { (name, likelihood) ->
                val rank = LIKELIHOOD_RANK[likelihood] ?: return@mapNotNull null
                "$name: ${listOf("possible", "likely", "very likely")[rank - 1]}"
            }
        }

        private fun pixelBox(poly: BoundingPoly?, width: Int, height: Int): NormalizedBox? {
            if (poly == null || width <= 0 || height <= 0) return null
            return boxFrom(poly.vertices.map { it.x / width to it.y / height })
        }

        // The API leaves out coordinates that are 0, so missing values default to 0.
        private fun boxFrom(points: List<Pair<Double, Double>>): NormalizedBox? {
            if (points.isEmpty()) return null
            val xs = points.map { it.first.coerceIn(0.0, 1.0).toFloat() }
            val ys = points.map { it.second.coerceIn(0.0, 1.0).toFloat() }
            val box = NormalizedBox(xs.min(), ys.min(), xs.max(), ys.max())
            return box.takeIf { it.right > it.left && it.bottom > it.top }
        }
    }
}

// Cloud Vision REST request and response shapes.

@Serializable
internal data class BatchAnnotateRequest(val requests: List<AnnotateImageRequest>)

@Serializable
internal data class AnnotateImageRequest(val image: ImageContent, val features: List<Feature>)

@Serializable
internal data class ImageContent(val content: String)

@Serializable
internal data class Feature(val type: String, val maxResults: Int)

@Serializable
internal data class BatchAnnotateResponse(val responses: List<AnnotateImageResponse> = emptyList())

@Serializable
internal data class AnnotateImageResponse(
    val localizedObjectAnnotations: List<LocalizedObject> = emptyList(),
    val labelAnnotations: List<EntityAnnotation> = emptyList(),
    val textAnnotations: List<EntityAnnotation> = emptyList(),
    val faceAnnotations: List<FaceAnnotation> = emptyList(),
    val error: Status? = null
)

@Serializable
internal data class LocalizedObject(val name: String = "", val score: Double = 0.0, val boundingPoly: BoundingPoly = BoundingPoly())

@Serializable
internal data class EntityAnnotation(val description: String = "", val score: Double = 0.0)

@Serializable
internal data class FaceAnnotation(
    val boundingPoly: BoundingPoly? = null,
    val fdBoundingPoly: BoundingPoly? = null,
    val detectionConfidence: Double = 0.0,
    val joyLikelihood: String = "",
    val sorrowLikelihood: String = "",
    val angerLikelihood: String = "",
    val surpriseLikelihood: String = ""
)

@Serializable
internal data class BoundingPoly(
    val vertices: List<Vertex> = emptyList(),
    val normalizedVertices: List<Vertex> = emptyList()
)

@Serializable
internal data class Vertex(val x: Double = 0.0, val y: Double = 0.0)

@Serializable
internal data class Status(val code: Int? = null, val message: String? = null)

@Serializable
internal data class ErrorResponse(val error: Status? = null)
