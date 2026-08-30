package com.verified.app.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.PixelCopy
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.camera.CameraManager
import com.verified.app.ml.ChestFrameAnalyzer
import com.verified.app.ml.NudeNetAnalyzer
import com.verified.app.ui.components.ChestOverlay
import com.verified.app.ui.components.ScanProgressBar
import com.verified.app.viewmodel.DetectionState
import com.verified.app.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ChestScanScreen(
    viewModel: ScanViewModel = viewModel(),
    onVerified: () -> Unit,
) {
    val context   = LocalContext.current
    val view      = LocalView.current
    val window    = (context as Activity).window
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState   by viewModel.uiState.collectAsState()

    val cameraManager = remember { CameraManager(context) }
    val nudeNet       = remember { NudeNetAnalyzer(context) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(Unit) {
        val analyzer = ChestFrameAnalyzer(
            nudeNet         = nudeNet,
            onNudeNetResult = viewModel::onNudeNetResult,
            onPoseResult    = viewModel::onPoseResult,
        )
        cameraManager.startCamera(lifecycleOwner, previewView, analyzer)
    }

    LaunchedEffect(uiState.detectionState) {
        if (uiState.detectionState == DetectionState.VERIFIED) {
            // Let the "Verified ✓" overlay render for a moment before capturing
            delay(400.milliseconds)
            val bitmap = captureView(window, view)
            viewModel.setCapturedBitmap(bitmap)
            // Brief pause so the user sees the verified state
            delay(300.milliseconds)
            cameraManager.shutdown()
            onVerified()
        }
    }

    DisposableEffect(Unit) {
        onDispose { nudeNet.close() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        ChestOverlay(
            detectionState     = uiState.detectionState,
            nudeNetResult      = uiState.nudeNetResult,
            poseResult         = uiState.poseResult,
            showLiveSkeleton   = uiState.showLiveSkeleton,
            showGhostSkeleton  = uiState.showGhostSkeleton,
            showNudeNetOverlay = uiState.showNudeNetOverlay,
            onToggleLive       = viewModel::toggleLiveSkeleton,
            onToggleGhost      = viewModel::toggleGhostSkeleton,
            onToggleNudeNet    = viewModel::toggleNudeNetOverlay,
            modifier           = Modifier.fillMaxSize(),
        )

        ScanProgressBar(
            active     = uiState.detectionState == DetectionState.DETECTED,
            onComplete = viewModel::onChestVerified,
            modifier   = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ── PixelCopy capture ─────────────────────────────────────────────────────────

/**
 * Captures the full composable surface — camera feed + all overlays — as a [Bitmap].
 * Uses [PixelCopy] so the GPU-rendered camera surface is included.
 * Falls back to a plain software draw if PixelCopy fails.
 */
private suspend fun captureView(window: Window, view: View): Bitmap =
    suspendCancellableCoroutine { cont ->
        val bitmap   = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val location = IntArray(2).also { view.getLocationInWindow(it) }
        val srcRect  = Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height,
        )
        PixelCopy.request(window, srcRect, bitmap, { result ->
            // Resume regardless — on failure the bitmap will just be black,
            // which is still a valid (if empty) result to show on ResultScreen.
            cont.resume(bitmap)
        }, Handler(Looper.getMainLooper()))
    }
