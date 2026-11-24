package com.eko.vision.core.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CameraFrameAnalyzer safely samples CameraX frames, throttles frame rate
 * to avoid thermal throttling, and rotates the bitmap to portrait.
 */
class CameraFrameAnalyzer(
    private val onFrameAvailable: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)
    private var lastAnalyzedTimestamp = 0L
    private val minIntervalMs = 120L // ~8 FPS optimal for mobile NPU/CPU balance

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        // 1. Throttle frame rate & drop frame if previous analysis is still ongoing
        if (currentTimestamp - lastAnalyzedTimestamp < minIntervalMs || isProcessing.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        try {
            isProcessing.set(true)
            lastAnalyzedTimestamp = currentTimestamp

            // Convert to Bitmap & apply sensor rotation
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toBitmap()
            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            onFrameAvailable(rotatedBitmap)
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            isProcessing.set(false)
            imageProxy.close()
        }
    }
}
