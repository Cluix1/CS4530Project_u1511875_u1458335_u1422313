package com.example.drawing_app

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.example.drawing_app.ui.DrawingScreen
import com.example.drawing_app.ui.DrawingToolbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
    val visible = MutableStateFlow(true)

    suspend fun closeScreen() {
        delay(3000)
        visible.value = false
    }
}

@Entity(tableName = "drawings")
data class Drawing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val backing : Bitmap
)

class MainViewModelFactory(private val repository: DrawingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(repository) as T
    }
}

class MainViewModel(private val repository: DrawingRepository) : ViewModel() {

    //Model (comes from the database through the repository)
    val drawingsReadOnly: StateFlow<List<Drawing>> = repository.allDrawings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Methods to modify the Model
    fun addDrawing(d: Drawing) {
        viewModelScope.launch { repository.add(d) }
    }

    fun removeDrawing(d: Drawing) {
        viewModelScope.launch { repository.remove(d) }
    }

    fun selectDrawing(d: Drawing){

    }

}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val splashViewModel: SplashViewModel = viewModel()
            val drawingViewModel: DrawingViewModel = viewModel()
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory((application as DrawingApplication).repository))
            val nav = rememberNavController()
            AppNavHost(mainViewModel, drawingViewModel, nav)
            DrawingApp(splashViewModel, drawingViewModel, mainViewModel, nav)
        }
    }
}

@Composable
fun AppNavHost(mainVM: MainViewModel, drawVM: DrawingViewModel, navController: NavHostController, ) {
    NavHost(navController = navController, startDestination = "SplashRoute") {
        composable("SplashRoute"){
            SplashScreen(navController)
        }
        composable("MainRoute") {
            MainScreen(mainVM, navController)
        }
        composable("DrawRoute"){
            DrawingScreen(drawVM, navController)
        }
    }
}

@Composable
private fun DrawingApp(
    splashViewModel: SplashViewModel,
    drawingViewModel: DrawingViewModel,
    mainViewModel: MainViewModel,
    nav: NavHostController
) {

}

@Composable
fun SplashScreen(nav: NavHostController){
    AnimatedVisibility(true, Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.crossed),
            contentDescription = "Drawing app splash screen"
        )
    }
    LaunchedEffect(Unit) {
        delay(3000)
        nav.navigate("MainRoute")
    }
}

@Composable
fun MainScreen(mainViewModel: MainViewModel, nav: NavHostController) {
    val drawings by mainViewModel.drawingsReadOnly.collectAsState()

    Column(
        Modifier.fillMaxSize().safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { nav.navigate("DrawRoute") },   // your drawing screen route
            modifier = Modifier.padding(16.dp)
        ) { Text("New Drawing") }

//        if (drawings.isEmpty()) {
//            Text("No drawings yet", Modifier.padding(32.dp))
//        } else {
//            LazyVerticalGrid(
//                columns = GridCells.Adaptive(minSize = 120.dp),
//                contentPadding = PaddingValues(8.dp),
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp),
//                modifier = Modifier.fillMaxSize()
//            ) {
//                itemsIndexed(drawings) { index, bitmap ->
//                    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
//                    Image(
//                        bitmap = imageBitmap,
//                        contentDescription = "Drawing ${index + 1}",
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier
//                            .aspectRatio(1f)
//                            .clip(RoundedCornerShape(8.dp))
//                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
//                            .clickable { nav.navigate(DrawRoute(index)) } // open existing drawing
//                    )
//                }
//            }
//        }
    }
}