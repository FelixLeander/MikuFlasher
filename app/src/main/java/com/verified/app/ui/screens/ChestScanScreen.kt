package com.verified.app.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.camera.CameraManager
import com.verified.app.ml.PoseAnalyzer
import com.verified.app.ui.components.ChestOverlay
import com.verified.app.viewmodel.DetectionState
import com.verified.app.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ChestScanScreen(
    viewModel: ScanViewModel = viewModel(),
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraManager = remember { CameraManager(context) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(Unit) {
        val analyzer = PoseAnalyzer { confidence ->
            viewModel.onChestDetectionUpdate(confidence)
        }
        cameraManager.startCamera(lifecycleOwner, previewView, analyzer)
    }

    LaunchedEffect(uiState.detectionState) {
        if (uiState.detectionState == DetectionState.DETECTED) {
            delay(800.milliseconds)
            viewModel.onChestVerified()
        }
        if (uiState.detectionState == DetectionState.VERIFIED) {
            delay(1800.milliseconds)
            cameraManager.shutdown()
            onVerified()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        ChestOverlay(
            detectionState = uiState.detectionState,
            modifier = Modifier.fillMaxSize()
        )
    }
}
