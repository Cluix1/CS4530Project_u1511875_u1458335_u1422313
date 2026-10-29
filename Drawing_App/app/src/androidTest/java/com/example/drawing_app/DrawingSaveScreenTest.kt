package com.example.drawing_app

import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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
