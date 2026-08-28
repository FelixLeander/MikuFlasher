package com.verified.app.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.camera.CameraManager
import com.verified.app.ml.FaceAnalyzer
import com.verified.app.ui.components.FaceOverlay
import com.verified.app.viewmodel.DetectionState
import com.verified.app.viewmodel.ScanViewModel
import kotlinx.coroutines.delay

@Composable
fun FaceScanScreen(
    viewModel: ScanViewModel = viewModel(),
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    // Hold the CameraManager in a remembered object tied to this composable's lifetime
    val cameraManager = remember { CameraManager(context) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Start camera once
    LaunchedEffect(Unit) {
        val analyzer = FaceAnalyzer { confidence ->
            viewModel.onFaceDetectionUpdate(confidence)
        }
        cameraManager.startCamera(lifecycleOwner, previewView, analyzer)
    }

    // When face is DETECTED, hold briefly then mark VERIFIED
    LaunchedEffect(uiState.detectionState) {
        if (uiState.detectionState == DetectionState.DETECTED) {
            delay(800)
            viewModel.onFaceVerified()
        }
        if (uiState.detectionState == DetectionState.VERIFIED) {
            delay(1800)
            cameraManager.shutdown()
            onVerified()
        }
    }

    DisposableEffect(Unit) {
        onDispose { /* cameraManager.shutdown() called before navigate */ }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Live camera feed
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Stencil overlay
        FaceOverlay(
            detectionState = uiState.detectionState,
            modifier = Modifier.fillMaxSize()
        )
    }
}
