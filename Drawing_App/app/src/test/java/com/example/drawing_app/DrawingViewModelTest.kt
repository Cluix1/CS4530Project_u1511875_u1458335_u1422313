package com.example.drawing_app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingViewModelTest {
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
