package com.verified.app.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.camera.CameraManager
import com.verified.app.ml.FaceAnalyzer
import com.verified.app.ui.components.FaceOverlay
import com.verified.app.ui.components.ScanProgressBar
import com.verified.app.viewmodel.DetectionState
import com.verified.app.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FaceScanScreen(
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
        val analyzer = FaceAnalyzer { confidence ->
            viewModel.onFaceDetectionUpdate(confidence)
        }
        cameraManager.startCamera(lifecycleOwner, previewView, analyzer)
    }

    // ScanProgressBar owns the DETECTED → VERIFIED transition via onComplete.
    // This LaunchedEffect only handles post-VERIFIED navigation.
    LaunchedEffect(uiState.detectionState) {
        if (uiState.detectionState == DetectionState.VERIFIED) {
            delay(1800.milliseconds)
            cameraManager.shutdown()
            onVerified()
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        FaceOverlay(
            detectionState = uiState.detectionState,
            modifier = Modifier.fillMaxSize(),
        )

        ScanProgressBar(
            active = uiState.detectionState == DetectionState.DETECTED,
            onComplete = viewModel::onFaceVerified,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
