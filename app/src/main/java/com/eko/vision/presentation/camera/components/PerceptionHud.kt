package com.eko.vision.presentation.camera.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eko.vision.presentation.camera.CameraUiState
import com.eko.vision.presentation.camera.VisionMode

@Composable
fun PerceptionHud(
    uiState: CameraUiState,
    onToggleFreeze: () -> Unit,
    onToggleBoxes: () -> Unit,
    onToggleOcr: () -> Unit,
    onModeSelect: (VisionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD: Telemetry Bar & Model Info
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isModelLoaded) Color(0xFF00E676) else Color.Red)
                        )
                        Text(
                            text = "Mirage (22MB VLM)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.averageLatencyMs} ms",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "%.1f FPS".format(uiState.fps),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Real-time Scene Description Pill
            if (uiState.perception.description.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.70f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = uiState.perception.description,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // BOTTOM HUD: Controls & Mode Switcher
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Select Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VisionMode.values().forEach { mode ->
                    FilterChip(
                        selected = uiState.activeMode == mode,
                        onClick = { onModeSelect(mode) },
                        label = {
                            Text(
                                text = when (mode) {
                                    VisionMode.LIVE_PERCEPTION -> "Live HUD"
                                    VisionMode.DOCUMENT_SCAN -> "Doc / OCR"
                                    VisionMode.ASSISTIVE_VOICE -> "Voice Narrator"
                                },
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Quick Actions Bar
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleBoxes) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = "Toggle Boxes",
                            tint = if (uiState.showBoundingBoxes) Color(0xFF00E5FF) else Color.Gray
                        )
                    }

                    IconButton(onClick = onToggleOcr) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Toggle OCR",
                            tint = if (uiState.showOcr) Color(0xFFFFD600) else Color.Gray
                        )
                    }

                    FilledIconButton(
                        onClick = onToggleFreeze,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (uiState.isFrozen) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Freeze Frame"
                        )
                    }
                }
            }
        }
    }
}
