package com.eko.vision.presentation.camera

import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eko.vision.presentation.camera.components.BoundingBoxOverlay
import com.eko.vision.presentation.camera.components.MirageShowcaseSheet
import com.eko.vision.presentation.camera.components.PerceptionHud
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        CameraContent(viewModel = viewModel)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(24.dp)
                    .safeDrawingPadding()
            ) {
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Eko Vision processes camera frames 100% locally with the 22MB Mirage VLM. No images leave your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun CameraContent(
    viewModel: CameraViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    var isShowcaseVisible by remember { mutableStateOf(false) }

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fullscreen Native CameraX Hardware Viewfinder
        // Running in COMPATIBLE mode (TextureView) for seamless compositing & bitmap capture
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewViewRef = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraScreen", "Camera use cases binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // 2. Invisible 1.dp WebAssembly Engine Host (Keeps Chromium renderer thread active in Window)
        viewModel.headlessWebView?.let { engineView ->
            AndroidView(
                factory = {
                    (engineView.parent as? ViewGroup)?.removeView(engineView)
                    engineView
                },
                modifier = Modifier.size(1.dp)
            )
        }

        // 3. Captured Still Image Display (Shown when analyzing or showing results)
        AnimatedVisibility(
            visible = uiState.capturedBitmap != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            uiState.capturedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured Still Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 4. Native Dynamic Bounding Boxes, Polygon Masks, Spatial Arrows & OCR
        if (uiState.captureState == CaptureState.RESULT) {
            BoundingBoxOverlay(
                objects = uiState.perception.objects,
                ocrBlocks = uiState.perception.ocr,
                spatialRelations = uiState.perception.spatialRelations,
                showObjects = uiState.showBoundingBoxes,
                showMasks = uiState.showMasks,
                showRelations = uiState.showRelations,
                showOcr = uiState.showOcr,
                isScanning = false,
                modifier = Modifier.fillMaxSize()
            )
        } else if (uiState.captureState == CaptureState.FRAMING) {
            // In framing, display framing reticle
            BoundingBoxOverlay(
                objects = emptyList(),
                ocrBlocks = emptyList(),
                spatialRelations = emptyList(),
                showObjects = false,
                showMasks = false,
                showRelations = false,
                showOcr = false,
                isScanning = true,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 5. Native AI HUD (Telemetry bar, Scene description, Layer Toggles, Shutter)
        PerceptionHud(
            uiState = uiState,
            onToggleBoxes = viewModel::toggleBoundingBoxes,
            onToggleMasks = viewModel::toggleMasks,
            onToggleRelations = viewModel::toggleRelations,
            onToggleOcr = viewModel::toggleOcr,
            onTriggerScan = {
                val currentBitmap = previewViewRef?.bitmap
                if (currentBitmap != null) {
                    viewModel.startScan(currentBitmap)
                }
            },
            onResetToFraming = viewModel::resetToFraming,
            onOpenShowcase = { isShowcaseVisible = true },
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        )

        // 6. Native Material 3 Bottom Sheet Inspector (Entities, Spatial Graph, OCR, Reasoning, JSON)
        if (isShowcaseVisible) {
            MirageShowcaseSheet(
                perception = uiState.perception,
                onDismissRequest = { isShowcaseVisible = false }
            )
        }
    }
}
