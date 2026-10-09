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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import com.example.drawing_app.DrawingTool
import com.example.drawing_app.DrawingViewModel
import com.example.drawing_app.Line

@Composable
fun DrawingScreen(drawingViewModel: DrawingViewModel) {
    val selectedTool = drawingViewModel.selectedTool.collectAsState().value
    val selectedColor = drawingViewModel.selectedColor.collectAsState().value
    val brushSize = drawingViewModel.brushSize.collectAsState().value
    val lines = drawingViewModel.lines.collectAsState().value

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
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        drawingViewModel.addLine(change.position - dragAmount, change.position)
                    }
                }
        ) {
            lines.forEach { drawSegment(it) }
        }
    }
}

private fun DrawScope.drawSegment(line: Line) {
    val width = line.strokeWidth.toPx()
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
