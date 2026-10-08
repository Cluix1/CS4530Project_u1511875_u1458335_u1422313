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
import androidx.compose.ui.input.pointer.pointerInput
import com.example.drawing_app.DrawingViewModel

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
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White)
                .pointerInput(selectedColor, brushSize) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        drawingViewModel.addLine(
                            start = change.position - dragAmount,
                            end = change.position
                        )
                    }
                }
        ) {
            lines.forEach { line ->
                drawLine(
                    color = line.color,
                    start = line.start,
                    end = line.end,
                    strokeWidth = line.strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
