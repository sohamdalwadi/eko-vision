package com.eko.vision.presentation.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eko.vision.presentation.camera.CameraUiState
import com.eko.vision.presentation.camera.CaptureState

/**
 * Native Jetpack Compose Perception HUD for Tap-to-Scan workflow:
 * - FRAMING: 60 FPS live camera preview, top model status, large shutter button.
 * - ANALYZING: Still image frozen, scanning radar progress indicator.
 * - RESULT: Overlaid bounding boxes/masks/relations/OCR, scene description card, layer toggles, retake action.
 */
@Composable
fun PerceptionHud(
    uiState: CameraUiState,
    onToggleBoxes: () -> Unit,
    onToggleMasks: () -> Unit,
    onToggleRelations: () -> Unit,
    onToggleOcr: () -> Unit,
    onTriggerScan: () -> Unit,
    onResetToFraming: () -> Unit,
    onOpenShowcase: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD: Status Bar & Scene Description
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Telemetry / Status Pill
            Surface(
                color = Color.Black.copy(alpha = 0.82f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Model Name & Status Dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isModelLoaded) Color(0xFF00E676) else Color(0xFFFFD600))
                        )
                        Text(
                            text = if (uiState.isModelLoaded) "Mirage 22MB VLM" else uiState.engineStatus,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Mode / Latency Status
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (uiState.captureState) {
                            CaptureState.ANALYZING -> {
                                CircularProgressIndicator(
                                    color = Color(0xFF00E5FF),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Inferring...",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                            }
                            CaptureState.RESULT -> {
                                if (uiState.lastLatencyMs > 0) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "Latency",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${uiState.lastLatencyMs} ms",
                                        color = Color(0xFF00E5FF),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            CaptureState.FRAMING -> {
                                Text(
                                    text = "Tap to Scan",
                                    color = Color(0xFF00E676),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Scene Description / Guidance Card
            when (uiState.captureState) {
                CaptureState.FRAMING -> {
                    Surface(
                        color = Color.Black.copy(alpha = 0.70f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Framing",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Aim camera at any scene and tap the shutter below to scan objects, masks, relations and text.",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                CaptureState.ANALYZING -> {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF00E676),
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Analyzing scene with Mirage VLM...",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "Evaluating multimodal vision-language representations on device CPU",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                CaptureState.RESULT -> {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.clickable { onOpenShowcase() }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.perception.description.isNotEmpty()) {
                                        uiState.perception.description
                                    } else {
                                        "Scene analyzed by Mirage VLM."
                                    },
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Details",
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = Color(0x3300E5FF),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.perception.objects.size} Objects",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        color = Color(0x3300E676),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.perception.spatialRelations.size} Relations",
                                            color = Color(0xFF00E676),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (uiState.perception.ocr.isNotEmpty()) {
                                        Surface(
                                            color = Color(0x33FFD600),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "${uiState.perception.ocr.size} OCR",
                                                color = Color(0xFFFFD600),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Tap to Inspect Breakdown",
                                    color = Color(0xFF8CEEFF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // CENTER: Animated Scanning Pulse in ANALYZING state
        if (uiState.captureState == CaptureState.ANALYZING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.70f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00E676),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Processing Frame",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // BOTTOM HUD: Layer Toggles + Shutter Button or Action Dock
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Layer Toggles (Visible when Results are displayed)
            AnimatedVisibility(
                visible = uiState.captureState == CaptureState.RESULT,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudTogglePill(
                        label = "Boxes",
                        icon = Icons.Default.CropFree,
                        selected = uiState.showBoundingBoxes,
                        activeColor = Color(0xFF00E5FF),
                        onClick = onToggleBoxes
                    )
                    HudTogglePill(
                        label = "Masks",
                        icon = Icons.Default.Layers,
                        selected = uiState.showMasks,
                        activeColor = Color(0xFF00E676),
                        onClick = onToggleMasks
                    )
                    HudTogglePill(
                        label = "Relations",
                        icon = Icons.Default.Share,
                        selected = uiState.showRelations,
                        activeColor = Color(0xFFD500F9),
                        onClick = onToggleRelations
                    )
                    HudTogglePill(
                        label = "OCR",
                        icon = Icons.Default.TextFields,
                        selected = uiState.showOcr,
                        activeColor = Color(0xFFFFD600),
                        onClick = onToggleOcr
                    )
                }
            }

            // Bottom Action Controls
            when (uiState.captureState) {
                CaptureState.FRAMING -> {
                    // Prominent Shutter Button for Tap-to-Scan
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable(
                                    enabled = uiState.isModelLoaded,
                                    onClick = onTriggerScan
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isModelLoaded) Color(0xFF00E676) else Color.Gray)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan Scene",
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(28.dp)
                                )
                            }
                        }

                        Text(
                            text = if (uiState.isModelLoaded) "TAP TO SCAN" else "LOADING MODEL...",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                CaptureState.ANALYZING -> {
                    // Analyzing status dock
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E676),
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Running Mirage forward pass...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                CaptureState.RESULT -> {
                    // Result Actions: [ Retake / New Scan ]  [ Breakdown Inspector ]
                    Surface(
                        color = Color.Black.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Retake / New Scan Button
                            Button(
                                onClick = onResetToFraming,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "New Scan",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "New Scan",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Open Breakdown Inspector Button
                            Button(
                                onClick = onOpenShowcase,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF).copy(alpha = 0.20f),
                                    contentColor = Color(0xFF00E5FF)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DataObject,
                                        contentDescription = "Breakdown",
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Breakdown",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom HUD Toggle Pill:
 * Active: glowing colored border, tinted background, colored icon, bold white text, active dot.
 * Inactive: dim grey border, dark background, dim grey icon and text.
 */
@Composable
private fun HudTogglePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) activeColor.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.65f)
    val borderColor = if (selected) activeColor else Color.White.copy(alpha = 0.20f)
    val contentColor = if (selected) activeColor else Color.White.copy(alpha = 0.45f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            }
        }
    }
}
