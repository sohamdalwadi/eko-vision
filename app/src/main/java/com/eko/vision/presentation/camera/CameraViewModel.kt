package com.eko.vision.presentation.camera

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eko.vision.core.engine.MirageVisionEngine
import com.eko.vision.core.engine.MirageVisionEngineImpl
import com.eko.vision.core.model.PerceptionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CameraUiState(
    val isModelLoaded: Boolean = false,
    val isFrozen: Boolean = false,
    val perception: PerceptionResult = PerceptionResult(),
    val fps: Float = 0.0f,
    val averageLatencyMs: Long = 0L,
    val showBoundingBoxes: Boolean = true,
    val showOcr: Boolean = true,
    val activeMode: VisionMode = VisionMode.LIVE_PERCEPTION,
    val errorMessage: String? = null
)

enum class VisionMode {
    LIVE_PERCEPTION,
    DOCUMENT_SCAN,
    ASSISTIVE_VOICE
}

class CameraViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val engine: MirageVisionEngine = MirageVisionEngineImpl(application)
    
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var frameCount = 0
    private var lastFpsCalculationTime = System.currentTimeMillis()

    init {
        loadModel()
    }

    private fun loadModel() {
        viewModelScope.launch {
            val result = engine.initialize()
            if (result.isSuccess) {
                _uiState.update { it.copy(isModelLoaded = true) }
            } else {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to load Mirage weights: ${result.exceptionOrNull()?.message}") 
                }
            }
        }
    }

    fun onFrameReceived(bitmap: Bitmap) {
        if (_uiState.value.isFrozen || !_uiState.value.isModelLoaded) return

        viewModelScope.launch {
            val result = engine.analyzeFrame(bitmap)
            result.onSuccess { perception ->
                updateFpsAndLatency(perception.latencyMs)
                _uiState.update { 
                    it.copy(perception = perception, averageLatencyMs = perception.latencyMs) 
                }
            }
        }
    }

    private fun updateFpsAndLatency(latencyMs: Long) {
        frameCount++
        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsCalculationTime
        if (elapsed >= 1000) {
            val currentFps = (frameCount * 1000f) / elapsed
            _uiState.update { it.copy(fps = currentFps) }
            frameCount = 0
            lastFpsCalculationTime = now
        }
    }

    fun toggleFreeze() {
        _uiState.update { it.copy(isFrozen = !it.isFrozen) }
    }

    fun toggleBoundingBoxes() {
        _uiState.update { it.copy(showBoundingBoxes = !it.showBoundingBoxes) }
    }

    fun toggleOcr() {
        _uiState.update { it.copy(showOcr = !it.showOcr) }
    }

    fun setVisionMode(mode: VisionMode) {
        _uiState.update { it.copy(activeMode = mode) }
    }

    override fun onCleared() {
        super.onCleared()
        engine.close()
    }
}
