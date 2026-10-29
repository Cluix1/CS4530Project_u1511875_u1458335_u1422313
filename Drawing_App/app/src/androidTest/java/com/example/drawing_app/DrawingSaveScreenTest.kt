package com.example.drawing_app

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DrawingSaveScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun gallery_opensUpdatesStartsFreshAndDeletesSelectedDrawing() {
        val repository = (compose.activity.application as DrawingApplication).repository
        val original = android.graphics.Bitmap.createBitmap(80, 80, android.graphics.Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        val firstId = runBlocking { repository.add(Drawing(backing = original)).toInt() }
        val secondId = runBlocking { repository.add(Drawing(backing = original)).toInt() }
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodes(androidx.compose.ui.test.hasTestTag("drawing-$firstId")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("drawing-$firstId").performClick()
            val opened = compose.onNodeWithTag("drawingCanvas").captureToImage().asAndroidBitmap()
            assertEquals(Color.RED, opened.getPixel(opened.width / 2, opened.height / 2))

            compose.onNodeWithTag("drawingCanvas").performTouchInput {
                swipe(Offset(width * 0.25f, height * 0.5f), Offset(width * 0.75f, height * 0.5f))
            }
            compose.onNodeWithText("Save").performClick()
            compose.onNodeWithText("Save to Photoshop").performClick()
            compose.waitUntil(10_000) {
                runBlocking {
                    repository.allDrawings.first().first { it.id == firstId }.backing.let { saved ->
                        saved.getPixel(saved.width / 2, saved.height / 2) != Color.RED
                    }
                }
            }
            assertEquals(1, runBlocking { repository.allDrawings.first().count { it.id == firstId } })
            compose.onNodeWithTag("galleryButton").performClick()
            compose.onNodeWithText("New Drawing").performClick()
            val fresh = compose.onNodeWithTag("drawingCanvas").captureToImage().asAndroidBitmap()
            assertEquals(Color.WHITE, fresh.getPixel(fresh.width / 2, fresh.height / 2))
            compose.onNodeWithTag("galleryButton").performClick()
            compose.onNodeWithContentDescription("Delete drawing $firstId").performClick()
            compose.waitUntil(10_000) {
                runBlocking { repository.allDrawings.first().none { it.id == firstId } }
            }
            assertTrue(runBlocking { repository.allDrawings.first().any { it.id == secondId } })
        } finally {
            runBlocking {
                repository.allDrawings.first().filter { it.id == firstId || it.id == secondId }
                    .forEach { repository.remove(it) }
            }
        }
    }

    @Test
    fun saveButton_savesCanvasWithoutControls() {
        val repository = (compose.activity.application as DrawingApplication).repository
        val existingIds = runBlocking { repository.allDrawings.first().map { it.id }.toSet() }
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodes(androidx.compose.ui.test.hasText("New Drawing")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("New Drawing").performClick()
            compose.onNodeWithTag("drawingCanvas").performTouchInput {
                swipe(Offset(width * 0.25f, height * 0.5f), Offset(width * 0.75f, height * 0.5f))
            }
            compose.onNodeWithText("Save").performClick()
            compose.onNodeWithText("Save to Photoshop").performClick()
            compose.waitUntil(10_000) {
                runBlocking { repository.allDrawings.first().any { it.id !in existingIds } }
            }
            val saved = runBlocking { repository.allDrawings.first().first { it.id !in existingIds }.backing }
            assertEquals(Color.WHITE, saved.getPixel(0, 0))
            assertTrue((saved.width / 4..saved.width * 3 / 4).any {
                saved.getPixel(it, saved.height / 2) != Color.WHITE
            })
        } finally {
            runBlocking {
                repository.allDrawings.first().filter { it.id !in existingIds }.forEach { repository.remove(it) }
            }
        }
    }
}
