package com.eko.vision.core.engine

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebView
import com.eko.vision.core.model.MirageOutputParser
import com.eko.vision.core.model.PerceptionResult
import kotlinx.coroutines.flow.StateFlow

interface MirageVisionEngine {
    val isReady: StateFlow<Boolean>
    val isInferring: StateFlow<Boolean>
    val statusMessage: StateFlow<String>
    val webView: WebView?
    fun analyzeFrame(bitmap: Bitmap): Boolean
    fun close()
}

/**
 * MirageVisionEngineImpl coordinates on-device Mirage VLM execution.
 * Feeds live camera bitmaps to the headless WebAssembly runtime and parses real-time perception JSON.
 */
class MirageVisionEngineImpl(
    context: Context,
    private val onResultReceived: (PerceptionResult) -> Unit
) : MirageVisionEngine {

    private val runtime = HeadlessMirageRuntime(context) { jsonStr, latencyMs ->
        val latencyLong = latencyMs.toLong().coerceAtLeast(1L)
        val perception = MirageOutputParser.parse(rawJson = jsonStr, latencyMs = latencyLong)
        onResultReceived(perception)
    }

    override val isReady: StateFlow<Boolean>
        get() = runtime.isModelReady

    override val isInferring: StateFlow<Boolean>
        get() = runtime.isInferring

    override val statusMessage: StateFlow<String>
        get() = runtime.engineStatus

    override val webView: WebView?
        get() = runtime.getWebView()

    override fun analyzeFrame(bitmap: Bitmap): Boolean {
        return runtime.analyzeBitmap(bitmap)
    }

    override fun close() {
        runtime.close()
    }
}
