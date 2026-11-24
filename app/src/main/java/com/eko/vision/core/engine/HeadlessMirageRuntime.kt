package com.eko.vision.core.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class HeadlessMirageRuntime(
    private val context: Context,
    private val onPerceptionJson: (json: String, latencyMs: Double) -> Unit
) {
    private var webView: WebView? = null
    private val isReady = AtomicBoolean(false)
    private val isProcessing = AtomicBoolean(false)
    private var lastDispatchedTime = 0L

    private val _engineStatus = MutableStateFlow("Initializing Mirage engine...")
    val engineStatus: StateFlow<String> = _engineStatus.asStateFlow()

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _isInferring = MutableStateFlow(false)
    val isInferring: StateFlow<Boolean> = _isInferring.asStateFlow()

    init {
        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        webView = WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }
            }

            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                    Log.d("MirageJS", "[${consoleMessage.messageLevel()}] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})")
                    return true
                }
            }

            addJavascriptInterface(NativeBridge(), "NativeBridge")
            loadUrl("https://appassets.androidplatform.net/assets/headless_engine.html")
            onResume()
            resumeTimers()
        }
    }

    fun getWebView(): WebView? = webView

    /**
     * Dispatches a single camera frame for Mirage VLM evaluation.
     * Non-blocking single-flight execution: drops frame if engine is busy.
     */
    fun analyzeBitmap(bitmap: Bitmap): Boolean {
        if (!isReady.get()) return false

        val now = System.currentTimeMillis()
        // Safety watchdog: auto-recover if a previous frame hung for > 15 seconds
        if (isProcessing.get() && now - lastDispatchedTime > 15000L) {
            Log.w(TAG, "Watchdog: previous frame timed out, auto-resetting inference lock.")
            isProcessing.set(false)
            _isInferring.value = false
        }

        if (!isProcessing.compareAndSet(false, true)) {
            return false // Drop frame to maintain 60 FPS CameraX preview without queue backlog
        }
        _isInferring.value = true

        try {
            lastDispatchedTime = now
            val targetSize = 256
            val scaledBitmap = if (bitmap.width == targetSize && bitmap.height == targetSize) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
            }

            val rgbBytes = bitmapToRgbByteArray(scaledBitmap, targetSize, targetSize)
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            val base64 = Base64.encodeToString(rgbBytes, Base64.NO_WRAP)

            webView?.post {
                webView?.evaluateJavascript(
                    "window.runMirageInference('$base64', $targetSize, $targetSize);"
                ) { evalResult ->
                    Log.d(TAG, "evaluateJavascript callback: $evalResult")
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Frame dispatch failed", e)
            isProcessing.set(false)
            _isInferring.value = false
            return false
        }
    }

    private fun bitmapToRgbByteArray(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val rgb = ByteArray(width * height * 3)
        var idx = 0
        for (p in pixels) {
            rgb[idx++] = ((p shr 16) and 0xFF).toByte() // R
            rgb[idx++] = ((p shr 8) and 0xFF).toByte()  // G
            rgb[idx++] = (p and 0xFF).toByte()         // B
        }
        return rgb
    }

    fun close() {
        webView?.post {
            webView?.destroy()
            webView = null
        }
    }

    private inner class NativeBridge {
        @JavascriptInterface
        fun onEngineReady() {
            Log.i(TAG, "Headless Mirage engine initialized and ready.")
            isReady.set(true)
            _isModelReady.value = true
            _engineStatus.value = "Mirage 22MB VLM Active"
        }

        @JavascriptInterface
        fun onStatusUpdate(status: String) {
            Log.d(TAG, "Engine Status: $status")
            _engineStatus.value = status
        }

        @JavascriptInterface
        fun onFrameStarted() {
            _isInferring.value = true
            Log.d(TAG, "Inference in progress...")
        }

        @JavascriptInterface
        fun onInferenceComplete(jsonString: String, latencyMs: Double) {
            isProcessing.set(false)
            _isInferring.value = false
            Log.i(TAG, "Inference completed in ${latencyMs.toLong()}ms")
            onPerceptionJson(jsonString, latencyMs)
        }

        @JavascriptInterface
        fun onInferenceError(errorMsg: String) {
            isProcessing.set(false)
            _isInferring.value = false
            Log.e(TAG, "Mirage error: $errorMsg")
            _engineStatus.value = "Notice: $errorMsg"
        }
    }

    companion object {
        private const val TAG = "HeadlessMirageRuntime"
    }
}
