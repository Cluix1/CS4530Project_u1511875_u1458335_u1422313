package com.example.drawing_app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

// Tests pen settings, clearing, and saving/canceling strokes and shapes.
// Drawing, buttons, splash, rotation, and layouts still need manual testing.
class DrawingViewModelTest {
    @Test
    fun shape_isOnlySavedWhenDragFinishes() {
        for (tool in listOf(DrawingTool.LINE, DrawingTool.RECTANGLE, DrawingTool.CIRCLE)) {
            val viewModel = DrawingViewModel()
            viewModel.setTool(tool)
            viewModel.startStroke(Offset(30f, 30f))
            viewModel.dragStroke(Offset(20f, 20f))
            viewModel.dragStroke(Offset(10f, 10f))

            assertTrue(viewModel.lines.value.isEmpty())
            assertEquals(Offset(10f, 10f), viewModel.preview.value?.end)
            viewModel.finishStroke()

            assertEquals(1, viewModel.lines.value.size)
            assertEquals(tool, viewModel.lines.value.first().tool)
            assertEquals(Offset(30f, 30f), viewModel.lines.value.first().start)
            assertNull(viewModel.preview.value)
        }
    }

    @Test
    fun cancelShape_discardsPreview() {
        val viewModel = DrawingViewModel()
        viewModel.setTool(DrawingTool.RECTANGLE)
        viewModel.startStroke(Offset.Zero)
        viewModel.dragStroke(Offset(10f, 10f))
        viewModel.cancelStroke()
        viewModel.finishStroke()

        assertTrue(viewModel.lines.value.isEmpty())
        assertNull(viewModel.preview.value)
    }

    @Test
    fun eraser_keepsEarlierStrokesAndSelectedColor() {
        val viewModel = DrawingViewModel()
        viewModel.setColor(Color.Red)
        viewModel.startStroke(Offset.Zero)
        viewModel.dragStroke(Offset(10f, 10f))
        viewModel.finishStroke()
        val original = viewModel.lines.value.toList()

        viewModel.setTool(DrawingTool.ERASER)
        viewModel.startStroke(Offset.Zero)
        viewModel.dragStroke(Offset(5f, 5f))
        viewModel.finishStroke()

        assertEquals(original, viewModel.lines.value.take(original.size))
        assertEquals(Color.White, viewModel.lines.value.last().color)
        assertEquals(Color.Red, viewModel.selectedColor.value)
    }

    @Test
    fun stroke_keepsSettingsFromStartOfDrag() {
        val viewModel = DrawingViewModel()
        viewModel.setTool(DrawingTool.BRUSH)
        viewModel.setColor(Color.Red)
        viewModel.startStroke(Offset.Zero)
        viewModel.setColor(Color.Blue)
        viewModel.setTool(DrawingTool.PEN)
        viewModel.dragStroke(Offset(10f, 10f))
        viewModel.finishStroke()

        assertTrue(viewModel.lines.value.all { it.tool == DrawingTool.BRUSH && it.color == Color.Red })
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
