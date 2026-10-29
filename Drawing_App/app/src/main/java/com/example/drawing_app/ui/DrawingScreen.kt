package com.example.drawing_app.ui

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
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
import androidx.navigation.NavHostController
import com.example.drawing_app.DrawingTool
import com.example.drawing_app.DrawingViewModel
import com.example.drawing_app.Line
import com.example.drawing_app.Drawing

@Composable
fun DrawingScreen(drawingViewModel: DrawingViewModel, nav: NavHostController, mainViewModel: MainViewModel) {
    val selectedTool = drawingViewModel.selectedTool.collectAsState().value
    val selectedColor = drawingViewModel.selectedColor.collectAsState().value
    val brushSize = drawingViewModel.brushSize.collectAsState().value
    val lines = drawingViewModel.lines.collectAsState().value
    val background by drawingViewModel.background.collectAsState()
    val backgroundImage = remember(background) { background?.asImageBitmap() }
    val isSaving by mainViewModel.isSaving.collectAsState()
    val isImporting by mainViewModel.isImporting.collectAsState()
    val isSharing by mainViewModel.isSharing.collectAsState()
    val saveMessage by mainViewModel.saveMessage.collectAsState()
    var showSaveOptions by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val busy = isSaving || isImporting || isSharing || isCapturing

    fun captureBitmap(onCaptured: (Bitmap) -> Unit) {
        if (busy) return
        isCapturing = true
        scope.launch {
            try {
                // Only the white drawing canvas is included in the bitmap.
                val imageBitmap = imageLayer.toImageBitmap()
                onCaptured(imageBitmap.asAndroidBitmap())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, "Could not capture drawing. Please try again.", Toast.LENGTH_SHORT).show()
            } finally {
                isCapturing = false
            }
        }
    }

    fun save(toPhotos: Boolean) {
        captureBitmap { mainViewModel.saveDrawing(it, toPhotos) }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            mainViewModel.importImage(it) { bitmap -> drawingViewModel.openDrawing(Drawing(backing = bitmap)) }
        }
    }

    val galleryPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save(true)
        else Toast.makeText(context, "Storage permission is needed to save to photos", Toast.LENGTH_SHORT).show()
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
            modifier = Modifier.align(Alignment.Start).testTag("galleryButton")
        ) { Text("← Gallery") }
        Row {
            Box {
                TextButton(onClick = { showSaveOptions = !showSaveOptions }, enabled = !busy) {
                    Text(if (isSaving || isCapturing) "Saving..." else "Save")
                }
                DropdownMenu(expanded = showSaveOptions, onDismissRequest = { showSaveOptions = false }) {
                    TextButton(onClick = {
                        showSaveOptions = false
                        save(false)
                    }, enabled = !busy) { Text("Save to Gallery") }
                    TextButton(onClick = {
                        showSaveOptions = false
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        ) {
                            galleryPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else save(true)
                    }, enabled = !busy) { Text("Save to Photos") }
                }
            }
            TextButton(onClick = { imagePicker.launch("image/*") }, enabled = !busy) {
                Text(if (isImporting) "Importing..." else "Import")
            }
            TextButton(onClick = {
                captureBitmap { bitmap ->
                    mainViewModel.prepareShare(bitmap) { uri ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share drawing"))
                    }
                }
            }, enabled = !busy) { Text(if (isSharing) "Sharing..." else "Share") }
        }
        DrawingToolbar(
            selectedTool = selectedTool,
            selectedColor = selectedColor,
            brushSize = brushSize,
            onToolSelected = drawingViewModel::setTool,
            onColorSelected = drawingViewModel::setColor,
            onBrushSizeChanged = drawingViewModel::setBrushSize,
            canClear = lines.isNotEmpty() || background != null,
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
            backgroundImage?.let { image ->
                val scale = minOf(size.width / image.width, size.height / image.height)
                var width = (image.width * scale).toInt()
                var height = (image.height * scale).toInt()
                if (width < 1) width = 1
                if (height < 1) height = 1
                drawImage(
                    image,
                    dstOffset = IntOffset(((size.width - width) / 2).toInt(), ((size.height - height) / 2).toInt()),
                    dstSize = IntSize(width, height)
                )
            }
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
