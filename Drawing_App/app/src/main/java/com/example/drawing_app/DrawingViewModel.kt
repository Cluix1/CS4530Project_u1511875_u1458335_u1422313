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

    private val _preview = MutableStateFlow<Line?>(null)
    val preview = _preview.asStateFlow()

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
        _lines.value += makeLine(start, end)
    }

    private fun makeLine(start: Offset, end: Offset): Line {
        val color = if (_selectedTool.value == DrawingTool.ERASER) Color.White else _selectedColor.value
        return Line(start, end, color, _brushSize.value.dp, _selectedTool.value)
    }

    fun startStroke(position: Offset) {
        _preview.value = makeLine(position, position)
    }

    fun dragStroke(position: Offset) {
        val current = _preview.value ?: return
        when (current.tool) {
            DrawingTool.PEN, DrawingTool.BRUSH, DrawingTool.ERASER -> {
                _lines.value += current.copy(end = position)
                _preview.value = current.copy(start = position, end = position)
            }
            else -> _preview.value = current.copy(end = position)
        }
    }

    fun finishStroke() {
        val current = _preview.value ?: return
        _lines.value += current
        _preview.value = null
    }

    fun cancelStroke() {
        _preview.value = null
    }

    fun clear() {
        _lines.value = emptyList()
        _preview.value = null
    }

    companion object {
        const val MIN_BRUSH_SIZE = 2f
        const val MAX_BRUSH_SIZE = 40f
        const val DEFAULT_BRUSH_SIZE = 8f
    }
}

/** A freehand segment or the start and end of a shape. */
data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 4.dp,
    val tool: DrawingTool = DrawingTool.PEN
)
