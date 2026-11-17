/**
 * AnalysisScreen.kt
 *
 * UI screen displaying Google Cloud Vision API analysis results, rendering detected objects,
 * labels, faces, and text overlays on top of the drawing.
 *
 * Authors: Ryan Hoo (u1511875), Connor Kolodziej (u1458335), Luke Stansbury (u1422313)
 * Course: CS 4530
 */
package com.example.drawing_app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drawing_app.Drawing
import com.example.drawing_app.vision.AnalysisUiState
import com.example.drawing_app.vision.AnalysisViewModel
import com.example.drawing_app.vision.DetectedObject
import com.example.drawing_app.vision.VisionAnalysis
import kotlin.math.roundToInt

// Distinct colors so each box matches its row in the list.
private val BoxColors = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFD81B60), Color(0xFF6D4C41)
)

private fun boxColor(index: Int) = BoxColors[index % BoxColors.size]

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel,
    onBack: () -> Unit,
    onEdit: (Drawing) -> Unit
) {
    val drawing by viewModel.drawing.collectAsState()
    val state by viewModel.state.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val image = remember(drawing) { drawing?.backing?.asImageBitmap() }
    val objects = (state as? AnalysisUiState.Success)?.analysis?.objects.orEmpty()

    Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("analysisGalleryButton")) { Text("Gallery") }
            TextButton(onClick = viewModel::retry, enabled = state !is AnalysisUiState.Loading) {
                Text(if (state is AnalysisUiState.Loading) "Analyzing..." else "Re-analyze")
            }
            TextButton(onClick = { drawing?.let(onEdit) }, enabled = drawing != null) { Text("Edit") }
        }
        HorizontalDivider()

        Box(
            Modifier.weight(1f).fillMaxWidth().background(Color.White).testTag("analysisImage"),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                AnnotatedImage(image, objects, selected)
            } else if (state !is AnalysisUiState.Error) {
                CircularProgressIndicator()
            }
        }
        HorizontalDivider()

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val current = state) {
                AnalysisUiState.Loading -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Analyzing image...")
                }
                is AnalysisUiState.Error -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        current.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("analysisError")
                    )
                    Spacer(Modifier.height(12.dp))
                    if (drawing != null) Button(onClick = viewModel::retry) { Text("Try again") }
                }
                is AnalysisUiState.Success -> ResultsList(current.analysis, selected, viewModel::select)
            }
        }
    }
}

/** Draws the image scaled to fit, with a labeled box over each detected object. */
@Composable
private fun AnnotatedImage(image: ImageBitmap, objects: List<DetectedObject>, selected: Int?) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Canvas(
        Modifier.fillMaxSize().semantics {
            contentDescription = "Analyzed image with ${objects.size} detected objects"
        }
    ) {
        val scale = minOf(size.width / image.width, size.height / image.height)
        val width = (image.width * scale).roundToInt().coerceAtLeast(1)
        val height = (image.height * scale).roundToInt().coerceAtLeast(1)
        val left = (size.width - width) / 2f
        val top = (size.height - height) / 2f
        drawImage(
            image,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(width, height)
        )

        // The selected box is drawn last so it sits on top of the others.
        objects.indices.sortedBy { it == selected }.forEach { index ->
            val obj = objects[index]
            val isSelected = selected == index
            // When one object is selected, the others fade so it stands out.
            val alpha = if (selected == null || isSelected) 1f else 0.3f
            val color = boxColor(index).copy(alpha = alpha)
            val topLeft = Offset(left + obj.box.left * width, top + obj.box.top * height)
            val boxSize = Size((obj.box.right - obj.box.left) * width, (obj.box.bottom - obj.box.top) * height)
            drawRect(color, topLeft, boxSize, style = Stroke(width = (if (isSelected) 4 else 2).dp.toPx()))

            val label = textMeasurer.measure("${obj.label} ${percent(obj.confidence)}", labelStyle)
            val padding = 4.dp.toPx()
            val labelWidth = label.size.width + padding * 2
            val labelHeight = label.size.height.toFloat()
            // Put the label above the box, or just inside it when the box touches the top edge.
            val labelX = topLeft.x.coerceIn(0f, (size.width - labelWidth).coerceAtLeast(0f))
            val labelY = if (topLeft.y - labelHeight >= 0f) topLeft.y - labelHeight else topLeft.y
            drawRect(color, Offset(labelX, labelY), Size(labelWidth, labelHeight))
            drawText(label, topLeft = Offset(labelX + padding, labelY), alpha = alpha)
        }
    }
}

@Composable
private fun ResultsList(analysis: VisionAnalysis, selected: Int?, onSelect: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("analysisResults")) {
        if (analysis.labels.isNotEmpty()) {
            item {
                Text(
                    "Image labels",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                )
                FlowRow(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    analysis.labels.forEach { label ->
                        Text(
                            "${label.name} ${percent(label.confidence)}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        item {
            Text(
                if (analysis.objects.isEmpty()) "No objects detected" else "Detected objects (${analysis.objects.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        itemsIndexed(analysis.objects) { index, obj ->
            ObjectRow(index, obj, selected == index) { onSelect(index) }
        }
        if (analysis.detectedText.isNotEmpty()) {
            item {
                Text(
                    "Text found",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            itemsIndexed(analysis.detectedText) { _, line ->
                Text("“$line”", modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ObjectRow(index: Int, obj: DetectedObject, isSelected: Boolean, onClick: () -> Unit) {
    val color = boxColor(index)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (isSelected) color else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("detectedObject-$index"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(14.dp).background(color, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(obj.label.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge)
            val subtitle = (listOf(obj.category) + obj.details).filter { it.isNotEmpty() }.joinToString(" \u00b7 ")
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(Modifier.width(88.dp), horizontalAlignment = Alignment.End) {
            Text(percent(obj.confidence), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { obj.confidence },
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun percent(confidence: Float) = "${(confidence * 100).roundToInt()}%"
