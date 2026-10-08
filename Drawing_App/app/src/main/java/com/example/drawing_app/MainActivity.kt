package com.example.drawing_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

// toolbar , tests

// Model: one straight segment of a stroke
data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 4.dp
)

//viewModel
class DrawingViewModel : ViewModel()
{
    //Model
    val lines = MutableStateFlow(listOf<Line>())

    var visible = MutableStateFlow(true)

    // Methods to modify the Model
    fun addLine(line: Line){
        lines.value += line
    }

    fun clear(){
        lines.value = emptyList()
    }

    suspend fun closeScreen(){
        delay(3000)
        visible.value = false
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() //allows you to see time, signal, bottom bar, etc
        val myToast = Toast.makeText(this,"start", Toast.LENGTH_SHORT)
        myToast.show()
        setContent {
            val myVMObj: DrawingViewModel = viewModel()
            SScreen(myVMObj)
            DrawingCanvas(myVMObj)
        }
    }

    override fun onResume() {
        super.onResume()
        val myToast = Toast.makeText(this,"res", Toast.LENGTH_SHORT)
        myToast.show()
    }
}

@Composable
fun SScreen(myVM: DrawingViewModel){
    AnimatedVisibility(myVM.visible.collectAsState().value, Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.crossed),
            contentDescription = "Splash Screen PNG"
        )
    }
    LaunchedEffect(Unit) {myVM.closeScreen()} //passive coroutine scope
}

//View
@Composable
fun DrawingCanvas(myVM: DrawingViewModel) {
    AnimatedVisibility(!myVM.visible.collectAsState().value) {
        //Observe my lines
        val observableLines = myVM.lines.collectAsState().value

        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .background(Color.White)
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // each drag event becomes a short segment from the previous point to the current one
                            myVM.addLine(
                                Line(
                                    start = change.position - dragAmount,
                                    end = change.position
                                )
                            )
                        }
                    }
            ) {
                observableLines.forEach { line ->
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
}