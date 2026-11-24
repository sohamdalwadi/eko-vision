package com.eko.vision.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

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
    @SerialName("attributes") val attributes: List<String> = emptyList(),
    @SerialName("box") val box: BoundingBox? = null,
    @SerialName("mask") val mask: List<List<Int>> = emptyList(),
    @SerialName("color_hex") val colorHex: Long = 0xFF00E5FF
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
 * Single-pass rich perception result matching the official Mirage VLM showcase.
 */
@Serializable
data class PerceptionResult(
    @SerialName("description") val description: String = "",
    @SerialName("reasoning_trace") val reasoningTrace: String = "",
    @SerialName("objects") val objects: List<DetectedObject> = emptyList(),
    @SerialName("ocr") val ocr: List<OcrBlock> = emptyList(),
    @SerialName("spatial_relations") val spatialRelations: List<SpatialRelation> = emptyList(),
    @SerialName("latency_ms") val latencyMs: Long = 0L,
    @SerialName("raw_json") val rawJson: String = ""
)

/**
 * Parser that translates raw JSON output from Cactus Compute's Mirage VLM into structured perception.
 */
object MirageOutputParser {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true 
    }

    private val colorPalette = listOf(
        0xFF84CC16, // Lime
        0xFFF59E0B, // Amber
        0xFF8B5CF6, // Violet
        0xFFF43F5E, // Rose
        0xFF06B6D4, // Cyan
        0xFF10B981, // Emerald
        0xFFF97316  // Orange
    )

    fun colorForName(name: String): Long {
        if (name.isEmpty()) return colorPalette[0]
        var hash = 0
        for (ch in name) {
            hash = (hash * 31 + ch.code)
        }
        val index = Math.abs(hash) % colorPalette.size
        return colorPalette[index]
    }

    fun parse(rawJson: String, latencyMs: Long): PerceptionResult {
        return try {
            val root = json.parseToJsonElement(rawJson).jsonObject
            val reasoning = root["reasoning"]?.jsonPrimitive?.contentOrNull ?: ""
            
            // Scene description
            val sceneObj = root["scene"]?.jsonObject
            val sceneDesc = sceneObj?.entries?.joinToString("; ") { (k, v) ->
                "${k.replace('_', ' ')}: ${v.jsonPrimitive.contentOrNull ?: v.toString()}"
            } ?: ""

            val finalDescription = if (sceneDesc.isNotEmpty()) {
                sceneDesc
            } else if (reasoning.isNotEmpty()) {
                reasoning.split("|").map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("lang", true) } ?: ""
            } else {
                "Scene analyzed by Mirage VLM."
            }

            // Objects
            val rawObjects = root["objects"]?.jsonArray ?: JsonArray(emptyList())
            val objectMap = mutableMapOf<Int, String>()
            val detectedObjects = rawObjects.mapNotNull { element ->
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.intOrNull ?: 0
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "object"
                objectMap[id] = name

                val score = obj["score"]?.jsonPrimitive?.floatOrNull ?: 1.0f
                val bboxArr = obj["bbox"]?.jsonArray
                val box = if (bboxArr != null && bboxArr.size == 4) {
                    val x1 = (bboxArr[0].jsonPrimitive.floatOrNull ?: 0f) / 1000f
                    val y1 = (bboxArr[1].jsonPrimitive.floatOrNull ?: 0f) / 1000f
                    val x2 = (bboxArr[2].jsonPrimitive.floatOrNull ?: 0f) / 1000f
                    val y2 = (bboxArr[3].jsonPrimitive.floatOrNull ?: 0f) / 1000f
                    BoundingBox(
                        ymin = y1.coerceIn(0f, 1f),
                        xmin = x1.coerceIn(0f, 1f),
                        ymax = y2.coerceIn(0f, 1f),
                        xmax = x2.coerceIn(0f, 1f)
                    )
                } else null

                val descObj = obj["description"]?.jsonObject
                val attributes = descObj?.entries?.map { "${it.key}: ${it.value.jsonPrimitive.contentOrNull ?: it.value}" } ?: emptyList()

                val maskArrays = mutableListOf<List<Int>>()
                obj["mask"]?.jsonArray?.forEach { maskItem ->
                    maskItem.jsonArrayOrNull?.let { poly ->
                        maskArrays.add(poly.mapNotNull { it.jsonPrimitive.intOrNull })
                    }
                }

                DetectedObject(
                    label = name,
                    confidence = score,
                    attributes = attributes,
                    box = box,
                    mask = maskArrays,
                    colorHex = colorForName(name)
                )
            }

            // Relations
            val rawRelations = root["relations"]?.jsonArray ?: JsonArray(emptyList())
            val spatialRelations = rawRelations.mapNotNull { element ->
                val rel = element.jsonObject
                val subjId = rel["subject"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                val objId = rel["object"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                val pred = rel["predicate"]?.jsonPrimitive?.contentOrNull ?: "near"

                val subjName = objectMap[subjId] ?: "Entity #$subjId"
                val objName = objectMap[objId] ?: "Entity #$objId"

                SpatialRelation(
                    subject = subjName,
                    relation = pred.replace('_', ' '),
                    target = objName
                )
            }

            // OCR
            val rawText = root["text"]?.jsonArray ?: JsonArray(emptyList())
            val ocrBlocks = rawText.mapNotNull { element ->
                val txtObj = element.jsonObject
                val text = txtObj["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val score = txtObj["score"]?.jsonPrimitive?.floatOrNull ?: 1.0f
                val bboxArr = txtObj["bbox"]?.jsonArray
                val box = if (bboxArr != null && bboxArr.size == 4) {
                    val x1 = (bboxArr[0].jsonPrimitive.floatOrNull ?: 0f) / 1000f
                    val y1 = (bboxArr[1].jsonPrimitive.floatOrNull ?: 0f) / 1000f
                    val x2 = (bboxArr[2].jsonPrimitive.floatOrNull ?: 0f) / 1000f
                    val y2 = (bboxArr[3].jsonPrimitive.floatOrNull ?: 0f) / 1000f
                    BoundingBox(
                        ymin = y1.coerceIn(0f, 1f),
                        xmin = x1.coerceIn(0f, 1f),
                        ymax = y2.coerceIn(0f, 1f),
                        xmax = x2.coerceIn(0f, 1f)
                    )
                } else null

                OcrBlock(text = text, confidence = score, box = box)
            }

            PerceptionResult(
                description = finalDescription,
                reasoningTrace = reasoning,
                objects = detectedObjects,
                ocr = ocrBlocks,
                spatialRelations = spatialRelations,
                latencyMs = latencyMs,
                rawJson = rawJson
            )
        } catch (e: Exception) {
            PerceptionResult(
                description = "Analysis completed",
                reasoningTrace = "Raw output received (${rawJson.length} bytes)",
                latencyMs = latencyMs,
                rawJson = rawJson
            )
        }
    }

    private val JsonElement.jsonArrayOrNull: JsonArray?
        get() = try { this.jsonArray } catch (_: Exception) { null }
}
