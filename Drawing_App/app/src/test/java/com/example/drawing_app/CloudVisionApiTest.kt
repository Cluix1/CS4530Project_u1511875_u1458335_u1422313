package com.example.drawing_app

import com.example.drawing_app.vision.CloudVisionApi
import com.example.drawing_app.vision.VisionException
import com.example.drawing_app.vision.createVisionHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

// Checks the Cloud Vision request, response parsing, and error handling without using the network.
class CloudVisionApiTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val sampleResponse = """
        {"responses":[{
          "localizedObjectAnnotations":[
            {"mid":"/m/01yrx","name":"Cat","score":0.62,"boundingPoly":{"normalizedVertices":[
              {"x":0.2,"y":0.1},{"x":0.6,"y":0.1},{"x":0.6,"y":0.5},{"x":0.2,"y":0.5}]}},
            {"mid":"/m/07j7r","name":"Tree","score":0.91,"boundingPoly":{"normalizedVertices":[
              {},{"x":0.25},{"x":0.25,"y":1.0},{"y":1.0}]}}
          ],
          "labelAnnotations":[{"description":"Cat","score":0.97},{"description":"Plant","score":0.8}],
          "textAnnotations":[{"description":"HELLO\nWORLD\n"},{"description":"HELLO"}],
          "faceAnnotations":[{
            "boundingPoly":{"vertices":[{"x":10,"y":20},{"x":60,"y":20},{"x":60,"y":120},{"x":10,"y":120}]},
            "fdBoundingPoly":{"vertices":[{"x":20,"y":40},{"x":50,"y":40},{"x":50,"y":100},{"x":20,"y":100}]},
            "detectionConfidence":0.75,"joyLikelihood":"VERY_LIKELY","sorrowLikelihood":"VERY_UNLIKELY",
            "angerLikelihood":"UNLIKELY","surpriseLikelihood":"POSSIBLE"}]
        }]}
    """.trimIndent()

    @Test
    fun analyze_sendsKeyInHeaderAndParsesResults() = runBlocking {
        var sentKey: String? = null
        var sentUrl = ""
        var sentBody = ""
        val engine = MockEngine { request ->
            sentKey = request.headers["x-goog-api-key"]
            sentUrl = request.url.toString()
            sentBody = String(request.body.toByteArray())
            respond(sampleResponse, HttpStatusCode.OK, jsonHeaders)
        }
        val api = CloudVisionApi(createVisionHttpClient(engine), "test-key")

        val result = api.analyze(byteArrayOf(1, 2, 3), width = 200, height = 400)

        assertEquals("test-key", sentKey)
        assertFalse("API key must not be in the URL", sentUrl.contains("test-key"))
        assertEquals("https://vision.googleapis.com/v1/images:annotate", sentUrl)
        assertTrue(sentBody.contains("\"content\":\"AQID\"")) // base64 of the bytes 1, 2, 3
        assertTrue(sentBody.contains("OBJECT_LOCALIZATION"))
        assertTrue(sentBody.contains("FACE_DETECTION"))

        // Sorted by confidence: Tree, Face, Cat.
        assertEquals(listOf("Tree", "Face", "Cat"), result.objects.map { it.label })
        val tree = result.objects[0]
        assertEquals(0f, tree.box.left, 0f) // missing coordinates mean 0
        assertEquals(0.25f, tree.box.right, 0.0001f)
        assertEquals(1f, tree.box.bottom, 0.0001f)
        val cat = result.objects[2]
        assertEquals("Object", cat.category)
        assertEquals(0.62f, cat.confidence, 0.0001f)
        assertEquals(0.2f, cat.box.left, 0.0001f)
        assertEquals(0.1f, cat.box.top, 0.0001f)

        val face = result.objects[1]
        assertEquals("Face", face.category)
        // Uses the tighter fdBoundingPoly, converted from pixels on a 200x400 image.
        assertEquals(0.1f, face.box.left, 0.0001f)
        assertEquals(0.1f, face.box.top, 0.0001f)
        assertEquals(0.25f, face.box.right, 0.0001f)
        assertEquals(0.25f, face.box.bottom, 0.0001f)
        assertEquals(listOf("Joy: very likely", "Surprise: possible"), face.details)

        assertEquals(listOf("Cat", "Plant"), result.labels.map { it.name })
        assertEquals(listOf("HELLO", "WORLD"), result.detectedText)
    }

    @Test
    fun analyze_withoutKey_failsBeforeAnyRequest() = runBlocking {
        var called = false
        val engine = MockEngine { called = true; respond("", HttpStatusCode.OK) }
        val api = CloudVisionApi(createVisionHttpClient(engine), "")
        val error = expectVisionError { api.analyze(byteArrayOf(1), 1, 1) }
        assertTrue(error.message!!.contains("CLOUD_VISION_API_KEY"))
        assertFalse(called)
    }

    @Test
    fun analyze_mapsHttpErrorsToFriendlyMessages() = runBlocking {
        val cases = listOf(
            Triple(HttpStatusCode.BadRequest, """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key."}}""", "rejected"),
            Triple(HttpStatusCode.Forbidden, """{"error":{"code":403,"message":"This API method requires billing to be enabled."}}""", "billing"),
            Triple(HttpStatusCode.Forbidden, """{"error":{"code":403,"message":"Cloud Vision API has not been used in project 1 before or it is disabled."}}""", "enabled"),
            Triple(HttpStatusCode.TooManyRequests, """{"error":{"code":429,"message":"Quota exceeded"}}""", "too many requests"),
            Triple(HttpStatusCode.ServiceUnavailable, "not json", "unavailable")
        )
        for ((status, body, expected) in cases) {
            val engine = MockEngine { respond(body, status, jsonHeaders) }
            val api = CloudVisionApi(createVisionHttpClient(engine), "key")
            val error = expectVisionError { api.analyze(byteArrayOf(1), 1, 1) }
            assertTrue("$status -> ${error.message}", error.message!!.contains(expected, ignoreCase = true))
        }
    }

    @Test
    fun analyze_perImageError_isReported() = runBlocking {
        val engine = MockEngine {
            respond("""{"responses":[{"error":{"code":3,"message":"Bad image data."}}]}""", HttpStatusCode.OK, jsonHeaders)
        }
        val api = CloudVisionApi(createVisionHttpClient(engine), "key")
        val error = expectVisionError { api.analyze(byteArrayOf(1), 1, 1) }
        assertTrue(error.message!!.contains("Bad image data."))
    }

    @Test
    fun analyze_networkFailure_becomesVisionException() = runBlocking {
        val engine = MockEngine { throw IOException("offline") }
        val api = CloudVisionApi(createVisionHttpClient(engine), "key")
        val error = expectVisionError { api.analyze(byteArrayOf(1), 1, 1) }
        assertTrue(error.message!!.contains("internet", ignoreCase = true))
    }

    @Test
    fun analyze_emptyResults_givesEmptyAnalysis() = runBlocking {
        val engine = MockEngine { respond("""{"responses":[{}]}""", HttpStatusCode.OK, jsonHeaders) }
        val api = CloudVisionApi(createVisionHttpClient(engine), "key")
        val result = api.analyze(byteArrayOf(1), 1, 1)
        assertTrue(result.objects.isEmpty() && result.labels.isEmpty() && result.detectedText.isEmpty())
    }

    private suspend fun expectVisionError(block: suspend () -> Unit): VisionException {
        try {
            block()
        } catch (e: VisionException) {
            return e
        }
        fail("Expected a VisionException")
        throw AssertionError()
    }
}
