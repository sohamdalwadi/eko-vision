package com.eko.vision.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Normalized Bounding Box coordinates [0.0 - 1.0] relative to frame width and height.
 */
@Serializable
data class BoundingBox(
    @SerialName("ymin") val ymin: Float,
    @SerialName("xmin") val xmin: Float,
    @SerialName("ymax") val ymax: Float,
    @SerialName("xmax") val xmax: Float
) {
    val width: Float get() = (xmax - xmin).coerceAtLeast(0f)
    val height: Float get() = (ymax - ymin).coerceAtLeast(0f)
}

@Serializable
data class DetectedObject(
    @SerialName("label") val label: String,
    @SerialName("confidence") val confidence: Float = 1.0f,
    @SerialName("box") val box: BoundingBox? = null,
    @SerialName("attributes") val attributes: List<String> = emptyList()
)

@Serializable
data class OcrBlock(
    @SerialName("text") val text: String,
    @SerialName("confidence") val confidence: Float = 1.0f,
    @SerialName("box") val box: BoundingBox? = null
)

@Serializable
data class SpatialRelation(
    @SerialName("subject") val subject: String,
    @SerialName("relation") val relation: String,
    @SerialName("target") val target: String
)

/**
 * Single-pass perception result returned by Mirage VLM (22MB).
 */
@Serializable
data class PerceptionResult(
    @SerialName("description") val description: String = "",
    @SerialName("objects") val objects: List<DetectedObject> = emptyList(),
    @SerialName("ocr") val ocr: List<OcrBlock> = emptyList(),
    @SerialName("spatial_relations") val spatialRelations: List<SpatialRelation> = emptyList(),
    @SerialName("latency_ms") val latencyMs: Long = 0L,
    @SerialName("raw_json") val rawJson: String = ""
)
