package com.example.drawing_app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests pen settings, clearing, and keeping existing strokes unchanged.
// Drawing, buttons, splash, rotation, and layouts still need manual testing.
class DrawingViewModelTest {
    @Test
    fun changingPenSettings_keepsExistingSegments() {
        val viewModel = DrawingViewModel()
        viewModel.setTool(DrawingTool.BRUSH)
        viewModel.setColor(Color.Red)
        viewModel.setBrushSize(12f)
        viewModel.addLine(Offset.Zero, Offset(10f, 10f))

        viewModel.setTool(DrawingTool.PEN)
        viewModel.setColor(Color.Blue)
        viewModel.setBrushSize(6f)
        viewModel.addLine(Offset(10f, 10f), Offset(20f, 20f))

        val lines = viewModel.lines.value
        assertEquals(2, lines.size)
        assertEquals(DrawingTool.BRUSH, lines[0].tool)
        assertEquals(Color.Red, lines[0].color)
        assertEquals(12f, lines[0].strokeWidth.value, 0f)
        assertEquals(DrawingTool.PEN, lines[1].tool)
        assertEquals(Color.Blue, lines[1].color)
        assertEquals(6f, lines[1].strokeWidth.value, 0f)
    }

    @Test
    fun clear_removesDrawingAndKeepsPenSettings() {
        val viewModel = DrawingViewModel()
        viewModel.setColor(Color.Red)
        viewModel.setBrushSize(12f)
        viewModel.setTool(DrawingTool.PEN)
        viewModel.addLine(Offset.Zero, Offset(10f, 10f))
        viewModel.addLine(Offset(10f, 10f), Offset(20f, 20f))

        viewModel.clear()

        assertTrue(viewModel.lines.value.isEmpty())
        assertEquals(Color.Red, viewModel.selectedColor.value)
        assertEquals(12f, viewModel.brushSize.value, 0f)
        assertEquals(DrawingTool.PEN, viewModel.selectedTool.value)

        // Drawing can start again immediately after clearing.
        viewModel.addLine(Offset.Zero, Offset(5f, 5f))
        assertEquals(1, viewModel.lines.value.size)
        assertEquals(Color.Red, viewModel.lines.value.first().color)
    }

    @Test
    fun setTool_updatesSelectedTool() {
        val viewModel = DrawingViewModel()
        viewModel.setTool(DrawingTool.BRUSH)
        assertEquals(DrawingTool.BRUSH, viewModel.selectedTool.value)
    }

    @Test
    fun setColor_updatesSelectedColor() {
        val viewModel = DrawingViewModel()
        viewModel.setColor(Color.Red)
        assertEquals(Color.Red, viewModel.selectedColor.value)
    }

    @Test
    fun setBrushSize_limitsValueToAllowedRange() {
        val viewModel = DrawingViewModel()
        viewModel.setBrushSize(100f)
        assertEquals(DrawingViewModel.MAX_BRUSH_SIZE, viewModel.brushSize.value)
    }
}
