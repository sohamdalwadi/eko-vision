package com.eko.vision.presentation.camera.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.eko.vision.core.model.DetectedObject
import com.eko.vision.core.model.OcrBlock
import com.eko.vision.core.model.SpatialRelation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 100% Native Jetpack Compose Canvas rendering:
 * - Sub-pixel polygon instance segmentation masks
 * - Multi-color entity bounding boxes & label badges
 * - Directional spatial relationship vectors with arrowhead markers
 * - OCR text highlight blocks
 * - Central AI viewfinder reticle & scanning brackets
 */
@Composable
fun BoundingBoxOverlay(
    objects: List<DetectedObject>,
    ocrBlocks: List<OcrBlock>,
    spatialRelations: List<SpatialRelation>,
    showObjects: Boolean,
    showMasks: Boolean,
    showRelations: Boolean,
    showOcr: Boolean,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Central AI Viewfinder Reticle Brackets
        if (isScanning) {
            drawReticle(canvasWidth, canvasHeight)
        }

        // Map objects by label/name for spatial relation connections
        val objectCenterMap = mutableMapOf<String, Offset>()

        // 2. Polygon Instance Segmentation Masks
        if (showMasks) {
            objects.forEach { obj ->
                val boxColor = Color(obj.colorHex)
                obj.mask.forEach { poly ->
                    if (poly.size >= 6) {
                        val path = Path()
                        val startX = (poly[0] / 1000f) * canvasWidth
                        val startY = (poly[1] / 1000f) * canvasHeight
                        path.moveTo(startX, startY)

                        for (i in 2 until poly.size step 2) {
                            val px = (poly[i] / 1000f) * canvasWidth
                            val py = (poly[i + 1] / 1000f) * canvasHeight
                            path.lineTo(px, py)
                        }
                        path.close()

                        // Translucent colored fill
                        drawPath(
                            path = path,
                            color = boxColor.copy(alpha = 0.22f)
                        )
                        // Boundary stroke
                        drawPath(
                            path = path,
                            color = boxColor.copy(alpha = 0.90f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // 3. Multi-Color Bounding Boxes with Labels & Attributes
        if (showObjects && objects.isNotEmpty()) {
            objects.forEach { obj ->
                val box = obj.box ?: return@forEach
                val left = box.xmin * canvasWidth
                val top = box.ymin * canvasHeight
                val width = box.width * canvasWidth
                val height = box.height * canvasHeight

                val boxColor = Color(obj.colorHex)

                // Track center for relation vectors
                val centerX = left + (width / 2f)
                val centerY = top + (height / 2f)
                objectCenterMap[obj.label.lowercase()] = Offset(centerX, centerY)

                // Draw bounding box outline
                drawRect(
                    color = boxColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Native Label Badge
                val scorePercent = (obj.confidence * 100).toInt()
                val mainLabel = "${obj.label} $scorePercent%"
                val attrSummary = if (obj.attributes.isNotEmpty()) " • ${obj.attributes.first()}" else ""
                val fullLabelText = mainLabel + attrSummary

                drawContext.canvas.nativeCanvas.apply {
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 32f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }

                    val bgPaint = Paint().apply {
                        color = android.graphics.Color.argb(220, 15, 18, 20)
                        isAntiAlias = true
                    }

                    val textWidth = textPaint.measureText(fullLabelText)
                    val pillHeight = 38f
                    val padding = 12f
                    val pillY = if (top > 180f) (top - 8f) else (top + pillHeight + 8f)

                    drawRoundRect(
                        left,
                        pillY - pillHeight + 6f,
                        left + textWidth + (padding * 2),
                        pillY + 6f,
                        8f,
                        8f,
                        bgPaint
                    )

                    drawText(fullLabelText, left + padding, pillY, textPaint)
                }
            }
        }

        // 4. Directional Spatial Relationship Vectors (Arrows & Badges)
        if (showRelations && spatialRelations.isNotEmpty()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)

            spatialRelations.forEach { rel ->
                val subjCenter = objectCenterMap[rel.subject.lowercase()]
                    ?: objectCenterMap.entries.firstOrNull { it.key.contains(rel.subject.lowercase()) }?.value
                val targetCenter = objectCenterMap[rel.target.lowercase()]
                    ?: objectCenterMap.entries.firstOrNull { it.key.contains(rel.target.lowercase()) }?.value

                if (subjCenter != null && targetCenter != null && subjCenter != targetCenter) {
                    // Draw dashed connecting vector
                    drawLine(
                        color = Color(0xFFE4E4E7),
                        start = subjCenter,
                        end = targetCenter,
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // Draw arrowhead at target
                    drawArrowhead(
                        from = subjCenter,
                        to = targetCenter,
                        color = Color(0xFFE4E4E7),
                        arrowSize = 18f
                    )

                    // Draw predicate badge at vector midpoint
                    val midX = (subjCenter.x + targetCenter.x) / 2f
                    val midY = (subjCenter.y + targetCenter.y) / 2f
                    val predicateText = rel.relation.replace('_', ' ')

                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 26f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                            isAntiAlias = true
                        }
                        val bgPaint = Paint().apply {
                            color = android.graphics.Color.argb(230, 24, 24, 27)
                            isAntiAlias = true
                        }

                        val tw = textPaint.measureText(predicateText)
                        val pad = 10f
                        drawRoundRect(
                            midX - (tw / 2f) - pad,
                            midY - 18f,
                            midX + (tw / 2f) + pad,
                            midY + 16f,
                            6f,
                            6f,
                            bgPaint
                        )
                        drawText(predicateText, midX - (tw / 2f), midY + 7f, textPaint)
                    }
                }
            }
        }

        // 5. OCR Text Highlight Regions
        if (showOcr && ocrBlocks.isNotEmpty()) {
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
                    val textPaint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 26f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val bgPaint = Paint().apply {
                        color = android.graphics.Color.argb(240, 255, 214, 0)
                        isAntiAlias = true
                    }
                    val textWidth = textPaint.measureText(block.text)
                    val pillY = if (top > 180f) (top - 6f) else (top + 34f)

                    drawRoundRect(
                        left,
                        pillY - 30f,
                        left + textWidth + 12f,
                        pillY + 6f,
                        6f,
                        6f,
                        bgPaint
                    )
                    drawText(block.text, left + 6f, pillY, textPaint)
                }
            }
        }
    }
}

private fun DrawScope.drawReticle(canvasWidth: Float, canvasHeight: Float) {
    val reticleWidth = canvasWidth * 0.74f
    val reticleHeight = canvasHeight * 0.48f
    val left = (canvasWidth - reticleWidth) / 2f
    val top = (canvasHeight - reticleHeight) / 2f
    val right = left + reticleWidth
    val bottom = top + reticleHeight
    val cornerLength = 32.dp.toPx()
    val strokeWidth = 2.dp.toPx()
    val reticleColor = Color(0x7700E5FF)

    drawLine(reticleColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
    drawLine(reticleColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)

    drawLine(reticleColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
    drawLine(reticleColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)

    drawLine(reticleColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
    drawLine(reticleColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)

    drawLine(reticleColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
    drawLine(reticleColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)

    val centerX = canvasWidth / 2f
    val centerY = canvasHeight / 2f
    val crosshairSize = 8.dp.toPx()
    drawLine(reticleColor, Offset(centerX - crosshairSize, centerY), Offset(centerX + crosshairSize, centerY), 1.5.dp.toPx())
    drawLine(reticleColor, Offset(centerX, centerY - crosshairSize), Offset(centerX, centerY + crosshairSize), 1.5.dp.toPx())
}

private fun DrawScope.drawArrowhead(from: Offset, to: Offset, color: Color, arrowSize: Float) {
    val angle = atan2(to.y - from.y, to.x - from.x)
    val arrowAngle = Math.PI / 6.0

    val p1 = Offset(
        (to.x - arrowSize * cos(angle - arrowAngle)).toFloat(),
        (to.y - arrowSize * sin(angle - arrowAngle)).toFloat()
    )
    val p2 = Offset(
        (to.x - arrowSize * cos(angle + arrowAngle)).toFloat(),
        (to.y - arrowSize * sin(angle + arrowAngle)).toFloat()
    )

    val path = Path().apply {
        moveTo(to.x, to.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path, color)
}
