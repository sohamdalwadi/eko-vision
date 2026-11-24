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

enum class CaptureState {
    FRAMING,    // Live 60 FPS CameraX viewfinder preview, 0% inference CPU
    ANALYZING,  // Still frame captured, frozen on screen, Mirage 22MB WASM forward pass in progress
    RESULT      // Inference complete, still frame + bounding boxes, masks, relations & OCR displayed
}

data class CameraUiState(
    val captureState: CaptureState = CaptureState.FRAMING,
    val isModelLoaded: Boolean = false,
    val capturedBitmap: Bitmap? = null,
    val perception: PerceptionResult = PerceptionResult(
        description = "Mirage 22MB engine ready. Aim camera at any scene and tap Scan."
    ),
    val lastLatencyMs: Long = 0L,
    val showBoundingBoxes: Boolean = true,
    val showMasks: Boolean = true,
    val showRelations: Boolean = true,
    val showOcr: Boolean = true,
    val engineStatus: String = "Initializing Mirage runtime..."
)

class CameraViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val engine: MirageVisionEngine = MirageVisionEngineImpl(application) { newPerception ->
        updatePerceptionResult(newPerception)
    }

    val headlessWebView: android.webkit.WebView?
        get() = engine.webView

    init {
        viewModelScope.launch {
            engine.isReady.collect { ready ->
                _uiState.update { it.copy(isModelLoaded = ready) }
            }
        }
        viewModelScope.launch {
            engine.statusMessage.collect { status ->
                _uiState.update { it.copy(engineStatus = status) }
            }
        }
    }

    /**
     * User tapped the shutter button or viewfinder to capture and analyze the current frame.
     */
    fun startScan(bitmap: Bitmap) {
        if (!_uiState.value.isModelLoaded) return
        if (_uiState.value.captureState == CaptureState.ANALYZING) return

        _uiState.update {
            it.copy(
                captureState = CaptureState.ANALYZING,
                capturedBitmap = bitmap
            )
        }

        viewModelScope.launch {
            val accepted = engine.analyzeFrame(bitmap)
            if (!accepted) {
                // If engine was unable to accept, return to framing
                _uiState.update {
                    it.copy(captureState = CaptureState.FRAMING, capturedBitmap = null)
                }
            }
        }
    }

    /**
     * User tapped "New Scan" / "Retake" to discard the current result and return to live framing.
     */
    fun resetToFraming() {
        _uiState.update {
            it.copy(
                captureState = CaptureState.FRAMING,
                capturedBitmap = null
            )
        }
    }

    private fun updatePerceptionResult(perception: PerceptionResult) {
        _uiState.update {
            it.copy(
                captureState = CaptureState.RESULT,
                perception = perception,
                lastLatencyMs = perception.latencyMs
            )
        }
    }

    fun toggleBoundingBoxes() {
        _uiState.update { it.copy(showBoundingBoxes = !it.showBoundingBoxes) }
    }

    fun toggleMasks() {
        _uiState.update { it.copy(showMasks = !it.showMasks) }
    }

    fun toggleRelations() {
        _uiState.update { it.copy(showRelations = !it.showRelations) }
    }

    fun toggleOcr() {
        _uiState.update { it.copy(showOcr = !it.showOcr) }
    }

    override fun onCleared() {
        super.onCleared()
        engine.close()
    }
}
