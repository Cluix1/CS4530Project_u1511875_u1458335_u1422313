package com.example.drawing_app

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.example.drawing_app.ui.DrawingToolbar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DrawingToolbarTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pickersAndSlider_updateDrawingSettingsAndClearStrokes() {
        val viewModel = DrawingViewModel()
        compose.setContent {
            DrawingToolbar(
                selectedTool = viewModel.selectedTool.collectAsState().value,
                selectedColor = viewModel.selectedColor.collectAsState().value,
                brushSize = viewModel.brushSize.collectAsState().value,
                onToolSelected = viewModel::setTool,
                onColorSelected = viewModel::setColor,
                onBrushSizeChanged = viewModel::setBrushSize,
                canClear = viewModel.lines.collectAsState().value.isNotEmpty(),
                onClear = viewModel::clear
            )
        }
        compose.onNodeWithText("Clear").assertIsNotEnabled()
        compose.onNodeWithTag("shapePicker").performClick()
        compose.onNodeWithText("Square").performClick()
        compose.onNodeWithTag("colorPicker").performClick()
        compose.onNodeWithContentDescription("Red").performClick()
        compose.onNodeWithTag("brushSizeSlider").performSemanticsAction(SemanticsActions.SetProgress) { it(24f) }
        compose.runOnIdle {
            viewModel.addLine(Offset.Zero, Offset(10f, 10f))
            val line = viewModel.lines.value.first()
            assertEquals(DrawingTool.BRUSH, line.tool)
            assertEquals(Color.Red, line.color)
            assertEquals(24f, line.strokeWidth.value, 0f)
        }
        compose.onNodeWithText("Clear").performClick()
        compose.onNodeWithText("Clear").assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue(viewModel.lines.value.isEmpty())
            assertEquals(Color.Red, viewModel.selectedColor.value)
        }
    }
}
