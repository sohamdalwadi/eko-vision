package com.eko.vision.presentation.camera.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.eko.vision.core.model.DetectedObject
import com.eko.vision.core.model.OcrBlock

/**
 * Draws normalized bounding boxes and text pills directly over the camera feed.
 */
@Composable
fun BoundingBoxOverlay(
    objects: List<DetectedObject>,
    ocrBlocks: List<OcrBlock>,
    showObjects: Boolean,
    showOcr: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Draw Object Detection Bounding Boxes
        if (showObjects) {
            objects.forEach { obj ->
                val box = obj.box ?: return@forEach
                val left = box.xmin * canvasWidth
                val top = box.ymin * canvasHeight
                val width = box.width * canvasWidth
                val height = box.height * canvasHeight

                // Box Outline
                drawRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Label pill text
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 34f
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    val labelText = "${obj.label} ${(obj.confidence * 100).toInt()}%"
                    drawText(labelText, left + 8f, (top - 12f).coerceAtLeast(40f), paint)
                }
            }
        }

        // 2. Draw OCR Detected Text Regions
        if (showOcr) {
            ocrBlocks.forEach { block ->
                val box = block.box ?: return@forEach
                val left = box.xmin * canvasWidth
                val top = box.ymin * canvasHeight
                val width = box.width * canvasWidth
                val height = box.height * canvasHeight

                drawRect(
                    color = Color(0xFFFFD600),
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 2.dp.toPx())
                )

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.YELLOW
                        textSize = 28f
                        isAntiAlias = true
                    }
                    drawText(block.text, left + 6f, (top - 8f).coerceAtLeast(30f), paint)
                }
            }
        }
    }
}
