package com.verified.app.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.camera.CameraManager
import com.verified.app.ml.PoseAnalyzer
import com.verified.app.ui.components.ChestOverlay
import com.verified.app.viewmodel.DetectionState
import com.verified.app.viewmodel.ScanViewModel
import kotlinx.coroutines.delay

@Composable
fun ChestScanScreen(
    viewModel: ScanViewModel = viewModel(),
    onVerified: () -> Unit,
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

    LaunchedEffect(Unit) {
        val analyzer = PoseAnalyzer { result -> viewModel.onPoseResult(result) }
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
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        ChestOverlay(
            detectionState = uiState.detectionState,
            poseResult = uiState.poseResult,
            showLiveSkeleton = uiState.showLiveSkeleton,
            showGhostSkeleton = uiState.showGhostSkeleton,
            onToggleLive = viewModel::toggleLiveSkeleton,
            onToggleGhost = viewModel::toggleGhostSkeleton,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
