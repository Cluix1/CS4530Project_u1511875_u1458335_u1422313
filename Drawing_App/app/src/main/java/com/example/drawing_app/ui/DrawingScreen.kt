package com.example.drawing_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import com.example.drawing_app.DrawingTool
import com.example.drawing_app.DrawingViewModel
import com.example.drawing_app.Line
import kotlin.math.abs
import kotlin.math.min

@Composable
fun DrawingScreen(drawingViewModel: DrawingViewModel) {
    val selectedTool = drawingViewModel.selectedTool.collectAsState().value
    val selectedColor = drawingViewModel.selectedColor.collectAsState().value
    val brushSize = drawingViewModel.brushSize.collectAsState().value
    val lines = drawingViewModel.lines.collectAsState().value
    val preview = drawingViewModel.preview.collectAsState().value

    Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        DrawingToolbar(
            selectedTool = selectedTool,
            selectedColor = selectedColor,
            brushSize = brushSize,
            onToolSelected = drawingViewModel::setTool,
            onColorSelected = drawingViewModel::setColor,
            onBrushSizeChanged = drawingViewModel::setBrushSize,
            canClear = lines.isNotEmpty(),
            onClear = drawingViewModel::clear
        )
        HorizontalDivider()
        Canvas(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds().background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = drawingViewModel::startStroke,
                        onDragEnd = drawingViewModel::finishStroke,
                        onDragCancel = drawingViewModel::cancelStroke,
                        onDrag = { change, _ ->
                            change.consume()
                            drawingViewModel.dragStroke(change.position)
                        }
                    )
                }
        ) {
            lines.forEach { drawSegment(it) }
            preview?.let { drawSegment(it) }
        }
    }
}

private fun DrawScope.drawSegment(line: Line) {
    val width = line.strokeWidth.toPx()
    when (line.tool) {
        DrawingTool.RECTANGLE -> drawRect(
            color = line.color,
            topLeft = Offset(min(line.start.x, line.end.x), min(line.start.y, line.end.y)),
            size = Size(abs(line.end.x - line.start.x), abs(line.end.y - line.start.y)),
            style = Stroke(width)
        )
        // Drag from the center out to the edge of the circle.
        DrawingTool.CIRCLE -> drawCircle(
            color = line.color,
            center = line.start,
            radius = (line.end - line.start).getDistance(),
            style = Stroke(width)
        )
        else -> {
            val squareBrush = line.tool == DrawingTool.BRUSH
            if (line.start == line.end) {
                if (squareBrush) {
                    drawRect(line.color, line.start - Offset(width / 2, width / 2), Size(width, width))
                } else {
                    drawCircle(line.color, width / 2, line.start)
                }
            } else {
                drawLine(
                    color = line.color,
                    start = line.start,
                    end = line.end,
                    strokeWidth = width,
                    cap = if (squareBrush) StrokeCap.Square else StrokeCap.Round
                )
            }
        }
    }
}
