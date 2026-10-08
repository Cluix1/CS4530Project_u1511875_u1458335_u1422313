package com.example.drawing_app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds editor settings so they are retained when the device rotates. */
class DrawingViewModel : ViewModel() {
    private val _lines = MutableStateFlow(listOf<Line>())
    val lines = _lines.asStateFlow()

    private val _selectedTool = MutableStateFlow(DrawingTool.PEN)
    val selectedTool = _selectedTool.asStateFlow()

    private val _selectedColor = MutableStateFlow(Color.Black)
    val selectedColor = _selectedColor.asStateFlow()

    private val _brushSize = MutableStateFlow(DEFAULT_BRUSH_SIZE)
    val brushSize = _brushSize.asStateFlow()

    fun setTool(tool: DrawingTool) { _selectedTool.value = tool }
    fun setColor(color: Color) { _selectedColor.value = color }
    fun setBrushSize(size: Float) {
        _brushSize.value = size.coerceIn(MIN_BRUSH_SIZE, MAX_BRUSH_SIZE)
    }

    fun addLine(start: Offset, end: Offset) {
        _lines.value += Line(start, end, _selectedColor.value, _brushSize.value.dp)
    }

    fun clear() {
        _lines.value = emptyList()
    }

    companion object {
        const val MIN_BRUSH_SIZE = 2f
        const val MAX_BRUSH_SIZE = 40f
        const val DEFAULT_BRUSH_SIZE = 8f
    }
}

/** One short segment in a freehand stroke. */
data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 4.dp
)
