package com.example.drawing_app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.drawing_app.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import com.example.drawing_app.DrawingTool
import com.example.drawing_app.DrawingViewModel
import com.example.drawing_app.Line

@Composable
fun DrawingScreen(drawingViewModel: DrawingViewModel, nav: NavHostController, mainViewModel: MainViewModel) {
    val selectedTool = drawingViewModel.selectedTool.collectAsState().value
    val selectedColor = drawingViewModel.selectedColor.collectAsState().value
    val brushSize = drawingViewModel.brushSize.collectAsState().value
    val lines = drawingViewModel.lines.collectAsState().value
    val isSaving by mainViewModel.isSaving.collectAsState()
    val saveMessage by mainViewModel.saveMessage.collectAsState()
    var showSaveOptions by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    fun save(toGallery: Boolean) {
        if (isSaving || isCapturing) return
        isCapturing = true
        scope.launch {
            try {
                // Only the white drawing canvas is included in the bitmap.
                val imageBitmap = imageLayer.toImageBitmap()
                mainViewModel.saveDrawing(imageBitmap.asAndroidBitmap(), toGallery)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "Could not capture drawing. Please try again.", Toast.LENGTH_SHORT).show()
            } finally {
                isCapturing = false
            }
        }
    }

    val galleryPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save(true)
        else Toast.makeText(context, "Gallery permission is needed to save", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            mainViewModel.clearSaveMessage()
        }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = {
                nav.navigate("MainRoute") {
                    popUpTo("MainRoute") { inclusive = true }
                    launchSingleTop = true
                }
            },
            modifier = Modifier.align(Alignment.Start)
        ) { Text("← Gallery") }
        Row {
            TextButton(onClick = { showSaveOptions = !showSaveOptions }, enabled = !isSaving && !isCapturing) {
                Text(if (isSaving || isCapturing) "Saving..." else "Save")
            }
            if (showSaveOptions) {
                TextButton(onClick = { save(false) }, enabled = !isSaving && !isCapturing) {
                    Text("Save to Photoshop")
                }
                TextButton(onClick = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    ) {
                        galleryPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else save(true)
                }, enabled = !isSaving && !isCapturing) { Text("Save to Gallery") }
            }
        }
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
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("drawingCanvas").clipToBounds()
                .drawWithContent {
                    imageLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(imageLayer)
                }.background(Color.White)
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
