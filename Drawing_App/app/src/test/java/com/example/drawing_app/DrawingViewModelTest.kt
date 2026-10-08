package com.example.drawing_app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingViewModelTest {
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
        viewModel.setTool(DrawingTool.CIRCLE)
        assertEquals(DrawingTool.CIRCLE, viewModel.selectedTool.value)
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
