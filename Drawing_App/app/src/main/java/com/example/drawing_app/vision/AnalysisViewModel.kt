/**
 * AnalysisViewModel.kt
 *
 * ViewModel for managing the execution state of image analysis requests and holding
 * vision analysis output state.
 *
 * Authors: Ryan Hoo (u1511875), Connor Kolodziej (u1458335), Luke Stansbury (u1422313)
 * Course: CS 4530
 */
package com.example.drawing_app.vision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.drawing_app.Drawing
import com.example.drawing_app.DrawingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AnalysisUiState {
    data object Loading : AnalysisUiState
    data class Success(val analysis: VisionAnalysis) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

/** Loads one gallery drawing and asks the Vision API what is in it. */
class AnalysisViewModel(
    private val drawingId: Int,
    private val drawings: DrawingRepository,
    private val vision: VisionRepository
) : ViewModel() {
    private val _drawing = MutableStateFlow<Drawing?>(null)
    val drawing = _drawing.asStateFlow()

    private val _state = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Loading)
    val state = _state.asStateFlow()

    // Index of the object highlighted in the list and on the image.
    private val _selected = MutableStateFlow<Int?>(null)
    val selected = _selected.asStateFlow()

    private var job: Job? = null

    init {
        analyze(refresh = false)
    }

    fun retry() = analyze(refresh = true)

    fun select(index: Int?) {
        _selected.value = if (_selected.value == index) null else index
    }

    private fun analyze(refresh: Boolean) {
        job?.cancel()
        _state.value = AnalysisUiState.Loading
        _selected.value = null
        job = viewModelScope.launch {
            try {
                val drawing = _drawing.value ?: drawings.getDrawing(drawingId)
                if (drawing == null) {
                    _state.value = AnalysisUiState.Error("This drawing is no longer in the gallery.")
                    return@launch
                }
                _drawing.value = drawing
                _state.value = AnalysisUiState.Success(vision.analyze(drawing.backing, drawing.fileName, refresh))
            } catch (e: CancellationException) {
                throw e
            } catch (e: VisionException) {
                _state.value = AnalysisUiState.Error(e.message ?: "Analysis failed. Please try again.")
            } catch (e: Exception) {
                _state.value = AnalysisUiState.Error("Analysis failed. Please try again.")
            }
        }
    }

    companion object {
        fun factory(drawingId: Int, drawings: DrawingRepository, vision: VisionRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { AnalysisViewModel(drawingId, drawings, vision) } }
    }
}
