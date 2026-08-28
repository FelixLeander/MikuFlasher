package com.verified.app.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.camera.CameraManager
import com.verified.app.ml.PoseAnalyzer
import com.verified.app.ui.components.ChestOverlay
import com.verified.app.ui.components.OneMoreStepBanner
import com.verified.app.viewmodel.DetectionState
import com.verified.app.viewmodel.ScanViewModel
import kotlinx.coroutines.delay

@Composable
fun ChestScanScreen(
    viewModel: ScanViewModel = viewModel(),
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraManager = remember { CameraManager(context) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Banner visibility: show briefly on entry, then hide so scanner is visible
    var bannerVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // Show "ONE MORE STEP" banner for 2.5s then slide it away
        delay(2500)
        bannerVisible = false

        // Start camera after the banner theatrics
        val analyzer = PoseAnalyzer { confidence ->
            viewModel.onChestDetectionUpdate(confidence)
        }
        cameraManager.startCamera(lifecycleOwner, previewView, analyzer)
    }

    LaunchedEffect(uiState.detectionState) {
        if (uiState.detectionState == DetectionState.DETECTED) {
            delay(800)
            viewModel.onChestVerified()
        }
        if (uiState.detectionState == DetectionState.VERIFIED) {
            delay(1800)
            cameraManager.shutdown()
            onVerified()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Live camera feed
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Stencil overlay
        ChestOverlay(
            detectionState = uiState.detectionState,
            modifier = Modifier.fillMaxSize()
        )

        // "ONE MORE STEP" banner slides in from top
        OneMoreStepBanner(
            visible = bannerVisible,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
