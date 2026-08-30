package com.verified.app.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.camera.CameraManager
import com.verified.app.ml.ChestFrameAnalyzer
import com.verified.app.ml.NudeNetAnalyzer
import com.verified.app.ui.components.ChestOverlay
import com.verified.app.viewmodel.DetectionState
import com.verified.app.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ChestScanScreen(
    viewModel: ScanViewModel = viewModel(),
    onVerified: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraManager = remember { CameraManager(context) }
    val nudeNet = remember { NudeNetAnalyzer(context) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Start camera with combined analyzer
    LaunchedEffect(Unit) {
        val analyzer = ChestFrameAnalyzer(
            nudeNet         = nudeNet,
            onNudeNetResult = viewModel::onNudeNetResult,
            onPoseResult    = viewModel::onPoseResult,
        )
        cameraManager.startCamera(lifecycleOwner, previewView, analyzer)
    }

    // Verification gate — NudeNet drives DETECTED; hold briefly then VERIFIED
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

    // Clean up TFLite interpreter when the screen leaves composition
    DisposableEffect(Unit) {
        onDispose { nudeNet.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        ChestOverlay(
            detectionState    = uiState.detectionState,
            nudeNetResult     = uiState.nudeNetResult,
            poseResult        = uiState.poseResult,
            showLiveSkeleton  = uiState.showLiveSkeleton,
            showGhostSkeleton = uiState.showGhostSkeleton,
            showNudeNetOverlay = uiState.showNudeNetOverlay,
            onToggleLive      = viewModel::toggleLiveSkeleton,
            onToggleGhost     = viewModel::toggleGhostSkeleton,
            onToggleNudeNet   = viewModel::toggleNudeNetOverlay,
            modifier          = Modifier.fillMaxSize(),
        )
    }
}
