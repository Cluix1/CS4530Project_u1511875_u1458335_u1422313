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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.drawing_app.DrawingTool
import com.example.drawing_app.DrawingViewModel

private val drawingColors = listOf(
    "Black" to Color.Black, "Red" to Color.Red,
    "Blue" to Color(0xFF1565C0), "Green" to Color(0xFF2E7D32),
    "Orange" to Color(0xFFFF9800), "Purple" to Color(0xFF7B1FA2)
)

@Composable
fun DrawingToolbar(
    selectedTool: DrawingTool,
    selectedColor: Color,
    brushSize: Float,
    onToolSelected: (DrawingTool) -> Unit,
    onColorSelected: (Color) -> Unit,
    onBrushSizeChanged: (Float) -> Unit,
    canClear: Boolean,
    onClear: () -> Unit
) {
    var showShapes by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(onClick = { showShapes = true }, modifier = Modifier.testTag("shapePicker")) {
                    Text(if (selectedTool == DrawingTool.PEN) "Circle \u25be" else "Square \u25be")
                }
                DropdownMenu(expanded = showShapes, onDismissRequest = { showShapes = false }) {
                    DropdownMenuItem(text = { Text("Circle") }, onClick = {
                        onToolSelected(DrawingTool.PEN)
                        showShapes = false
                    })
                    DropdownMenuItem(text = { Text("Square") }, onClick = {
                        onToolSelected(DrawingTool.BRUSH)
                        showShapes = false
                    })
                }
            }
            Box {
                TextButton(onClick = { showColors = true }, modifier = Modifier.testTag("colorPicker")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(16.dp).background(selectedColor, CircleShape).border(1.dp, Color.Gray, CircleShape))
                        Text("Color \u25be")
                    }
                }
                DropdownMenu(expanded = showColors, onDismissRequest = { showColors = false }) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        drawingColors.forEach { (name, color) ->
                            ColorButton(name, color, color == selectedColor) {
                                onColorSelected(it)
                                showColors = false
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onClear, enabled = canClear) {
                Text("Clear")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size: ${brushSize.toInt()} dp", modifier = Modifier.width(84.dp))
            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChanged,
                valueRange = DrawingViewModel.MIN_BRUSH_SIZE..DrawingViewModel.MAX_BRUSH_SIZE,
                modifier = Modifier.weight(1f).testTag("brushSizeSlider")
            )
            Box(
                modifier = Modifier.padding(start = 12.dp).size(40.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(brushSize.dp)
                        .background(
                            selectedColor,
                            if (selectedTool == DrawingTool.BRUSH) RoundedCornerShape(0.dp) else CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ColorButton(name: String, color: Color, selected: Boolean, onColorSelected: (Color) -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(color)
            .border(if (selected) 3.dp else 1.dp, borderColor, CircleShape)
            .semantics { contentDescription = name }
            .clickable { onColorSelected(color) }
    )
}
