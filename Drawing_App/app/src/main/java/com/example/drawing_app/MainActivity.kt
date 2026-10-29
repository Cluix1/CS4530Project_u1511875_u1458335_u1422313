package com.example.drawing_app

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.drawing_app.ui.DrawingScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class Drawing(
    val id: Int = 0,
    val backing: Bitmap
)

class MainViewModelFactory(private val repository: DrawingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(repository) as T
    }
}

class MainViewModel(private val repository: DrawingRepository) : ViewModel() {
    private var selectedDrawingId: Int? = null
    private var editorSession = 0
    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage = _saveMessage.asStateFlow()
    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()
    private val _isSharing = MutableStateFlow(false)
    val isSharing = _isSharing.asStateFlow()

    fun importImage(uri: Uri, onImported: (Bitmap) -> Unit) {
        if (_isImporting.value) return
        _isImporting.value = true
        val session = editorSession
        viewModelScope.launch {
            try {
                val bitmap = repository.importImage(uri)
                if (session == editorSession) {
                    selectDrawing(null)
                    onImported(bitmap)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveMessage.value = "Could not import image. Please choose another image."
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun prepareShare(bitmap: Bitmap, onReady: (Uri) -> Unit) {
        if (_isSharing.value) return
        _isSharing.value = true
        viewModelScope.launch {
            try {
                onReady(repository.prepareShare(bitmap))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveMessage.value = "Could not share drawing. Please try again."
            } finally {
                _isSharing.value = false
            }
        }
    }

    fun saveDrawing(bitmap: Bitmap, toPhotos: Boolean) {
        if (isSaving.value) return
        _isSaving.value = true
        _saveMessage.value = null
        val drawingId = selectedDrawingId
        val session = editorSession
        viewModelScope.launch {
            try {
                if (toPhotos) repository.saveToPhotos(bitmap)
                else {
                    val drawing = Drawing(id = drawingId ?: 0, backing = bitmap)
                    if (drawingId == null) {
                        val newId = repository.add(drawing).toInt()
                        if (editorSession == session) selectedDrawingId = newId
                    } else repository.update(drawing)
                }
                _saveMessage.value = if (toPhotos) "Saved to photos" else "Saved to gallery"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveMessage.value = "Could not save drawing. Please try again."
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveMessage() { _saveMessage.value = null }

    // Drawings come from the repository.
    val drawingsReadOnly: StateFlow<List<Drawing>> = repository.allDrawings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeDrawing(d: Drawing) {
        viewModelScope.launch {
            try {
                repository.remove(d)
                if (selectedDrawingId == d.id) selectDrawing(null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _saveMessage.value = "Could not delete drawing. Please try again."
            }
        }
    }

    fun selectDrawing(d: Drawing?) {
        selectedDrawingId = d?.id
        editorSession++
    }

}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val drawingViewModel: DrawingViewModel = viewModel()
            val mainViewModel: MainViewModel = viewModel(factory = MainViewModelFactory((application as DrawingApplication).repository))
            val nav = rememberNavController()
            AppNavHost(mainViewModel, drawingViewModel, nav)
        }
    }
}

@Composable
fun AppNavHost(mainVM: MainViewModel, drawVM: DrawingViewModel, navController: NavHostController) {
    NavHost(navController = navController, startDestination = "SplashRoute") {
        composable("SplashRoute"){
            SplashScreen(navController)
        }
        composable("MainRoute") {
            MainScreen(mainVM, navController, drawVM)
        }
        composable("DrawRoute"){
            DrawingScreen(drawVM, navController, mainVM)
        }
    }
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
        nav.navigate("MainRoute") {
            popUpTo("SplashRoute") { inclusive = true }
        }
    }
}

@Composable
fun MainScreen(mainViewModel: MainViewModel, nav: NavHostController, drawingViewModel: DrawingViewModel) {
    val drawings by mainViewModel.drawingsReadOnly.collectAsState()
    val message by mainViewModel.saveMessage.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            mainViewModel.clearSaveMessage()
        }
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Button(
                onClick = {
                    mainViewModel.selectDrawing(null)
                    drawingViewModel.newDrawing()
                    nav.navigate("DrawRoute")
                },
                modifier = Modifier.padding(16.dp)
            ) { Text("New Drawing") }
        }

        if (drawings.isEmpty()) {
            Text("No drawings yet", Modifier.padding(32.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(drawings, key = { _, d -> d.id }) { index, drawing ->
                    val imageBitmap = remember(drawing) { drawing.backing.asImageBitmap() }
                    Box {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Drawing ${index + 1}",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .testTag("drawing-${drawing.id}")
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                                .clickable {
                                    mainViewModel.selectDrawing(drawing)
                                    drawingViewModel.openDrawing(drawing)
                                    nav.navigate("DrawRoute")
                                }
                        )
                        IconButton(
                            onClick = { mainViewModel.removeDrawing(drawing) },
                            modifier = Modifier.align(Alignment.TopEnd)
                                .background(Color.White.copy(alpha = 0.9f), androidx.compose.foundation.shape.CircleShape)
                                .semantics { contentDescription = "Delete drawing ${drawing.id}" }
                        ) {
                            Text("\u00d7", color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}
