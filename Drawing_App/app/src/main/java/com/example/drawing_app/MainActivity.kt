package com.example.drawing_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drawing_app.ui.DrawingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class SplashViewModel : ViewModel() {
    val visible = MutableStateFlow(true)

    suspend fun closeScreen() {
        delay(3000)
        visible.value = false
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val splashViewModel: SplashViewModel = viewModel()
            val drawingViewModel: DrawingViewModel = viewModel()
            DrawingApp(splashViewModel, drawingViewModel)
        }
    }
}

@Composable
private fun DrawingApp(
    splashViewModel: SplashViewModel,
    drawingViewModel: DrawingViewModel
) {
    val showSplash = splashViewModel.visible.collectAsState().value
    AnimatedVisibility(showSplash, Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.crossed),
            contentDescription = "Drawing app splash screen"
        )
    }
    AnimatedVisibility(!showSplash, Modifier.fillMaxSize()) {
        DrawingScreen(drawingViewModel)
    }
    LaunchedEffect(Unit) { splashViewModel.closeScreen() }
}
