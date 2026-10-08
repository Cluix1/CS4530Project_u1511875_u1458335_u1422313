package com.example.drawing_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.drawing_app.DrawingTool
import com.example.drawing_app.DrawingViewModel

private val drawingColors = listOf(
    Color.Black, Color.Red, Color(0xFF1565C0), Color(0xFF2E7D32),
    Color(0xFFFF9800), Color(0xFF7B1FA2)
)

@Composable
fun DrawingToolbar(
    selectedTool: DrawingTool,
    selectedColor: Color,
    brushSize: Float,
    onToolSelected: (DrawingTool) -> Unit,
    onColorSelected: (Color) -> Unit,
    onBrushSizeChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Tools", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolButton("Pen", DrawingTool.PEN, selectedTool, onToolSelected)
            ToolButton("Brush", DrawingTool.BRUSH, selectedTool, onToolSelected)
            ToolButton("Eraser", DrawingTool.ERASER, selectedTool, onToolSelected)
            ToolButton("Line", DrawingTool.LINE, selectedTool, onToolSelected)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolButton("Rectangle", DrawingTool.RECTANGLE, selectedTool, onToolSelected)
            ToolButton("Circle", DrawingTool.CIRCLE, selectedTool, onToolSelected)
        }

        Text("Color", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            drawingColors.forEach { color ->
                ColorButton(color, color == selectedColor, onColorSelected)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size: ${brushSize.toInt()} px", modifier = Modifier.width(84.dp))
            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChanged,
                valueRange = DrawingViewModel.MIN_BRUSH_SIZE..DrawingViewModel.MAX_BRUSH_SIZE,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.padding(start = 12.dp).size(40.dp)
                    .background(selectedColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(brushSize.coerceAtMost(32f).dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    label: String,
    tool: DrawingTool,
    selectedTool: DrawingTool,
    onToolSelected: (DrawingTool) -> Unit
) {
    val selected = tool == selectedTool
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = label,
        color = textColor,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(background)
            .clickable { onToolSelected(tool) }.padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

@Composable
private fun ColorButton(color: Color, selected: Boolean, onColorSelected: (Color) -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape).background(color)
            .border(if (selected) 3.dp else 1.dp, borderColor, CircleShape)
            .clickable { onColorSelected(color) }
    )
}
