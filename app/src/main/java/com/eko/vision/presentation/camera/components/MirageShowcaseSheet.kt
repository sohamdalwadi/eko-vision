package com.eko.vision.presentation.camera.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eko.vision.core.model.PerceptionResult

enum class ShowcaseTab {
    ENTITIES_ATTRIBUTES,
    SPATIAL_GRAPH,
    OCR_TEXT,
    RAW_JSON
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MirageShowcaseSheet(
    perception: PerceptionResult,
    onDismissRequest: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ShowcaseTab.ENTITIES_ATTRIBUTES) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF101416),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.Gray.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Mirage VLM Perception Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }

                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            // Tab Selector Chips
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF00E5FF),
                edgePadding = 0.dp,
                divider = {}
            ) {
                ShowcaseTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    ShowcaseTab.ENTITIES_ATTRIBUTES -> "Entities (${perception.objects.size})"
                                    ShowcaseTab.SPATIAL_GRAPH -> "Scene Graph (${perception.spatialRelations.size})"
                                    ShowcaseTab.OCR_TEXT -> "OCR (${perception.ocr.size})"
                                    ShowcaseTab.RAW_JSON -> "Raw JSON"
                                },
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            when (selectedTab) {
                ShowcaseTab.ENTITIES_ATTRIBUTES -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Natural Language Scene Narrative Card
                        item {
                            Surface(
                                color = Color(0xFF181F24),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Scene Narrative",
                                            color = Color(0xFF00E5FF),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = perception.description,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                    if (perception.reasoningTrace.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = perception.reasoningTrace,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Entities with Attributes & Box Details
                        items(perception.objects) { obj ->
                            Surface(
                                color = Color(0xFF181F24),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(obj.colorHex))
                                            )
                                            Text(
                                                text = obj.label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White
                                            )
                                        }

                                        Text(
                                            text = "${(obj.confidence * 100).toInt()}% confidence",
                                            fontSize = 12.sp,
                                            color = Color(0xFF00E676),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Attribute Chips
                                    if (obj.attributes.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            obj.attributes.forEach { attr ->
                                                Surface(
                                                    color = Color(0xFF263238),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = attr,
                                                        color = Color(0xFFB0BEC5),
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Normalized Box Coordinates
                                    obj.box?.let { box ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Box: [ymin: %.2f, xmin: %.2f, ymax: %.2f, xmax: %.2f]".format(
                                                box.ymin, box.xmin, box.ymax, box.xmax
                                            ),
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ShowcaseTab.SPATIAL_GRAPH -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(perception.spatialRelations) { relation ->
                            Surface(
                                color = Color(0xFF181F24),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        color = Color(0xFF00363D),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = relation.subject,
                                            color = Color(0xFF8CEEFF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = relation.relation,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Surface(
                                        color = Color(0xFF332B00),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = relation.target,
                                            color = Color(0xFFFFD600),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ShowcaseTab.OCR_TEXT -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(perception.ocr) { ocr ->
                            Surface(
                                color = Color(0xFF181F24),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ocr.text,
                                            color = Color(0xFFFFD600),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Confidence: ${(ocr.confidence * 100).toInt()}%",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(ocr.text))
                                        Toast.makeText(context, "Copied text", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy Text",
                                            tint = Color.LightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                ShowcaseTab.RAW_JSON -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Exact Mirage Output Payload:",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            TextButton(onClick = {
                                clipboardManager.setText(AnnotatedString(perception.rawJson))
                                Toast.makeText(context, "Copied JSON payload", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF00E5FF)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy JSON", color = Color(0xFF00E5FF), fontSize = 12.sp)
                            }
                        }

                        Surface(
                            color = Color(0xFF0B0E10),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color(0xFF1F2933), RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            SelectionContainer {
                                LazyColumn(modifier = Modifier.padding(10.dp)) {
                                    item {
                                        Text(
                                            text = perception.rawJson,
                                            color = Color(0xFF8CEEFF),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 16.sp
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
}
