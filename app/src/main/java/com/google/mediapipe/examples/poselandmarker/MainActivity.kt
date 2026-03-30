package com.google.mediapipe.examples.poselandmarker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.tooling.preview.Preview as ComposePreview

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val cameraController by lazy { CameraController(viewModel) }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.hasCameraPermission = granted

        if (!granted) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MediaPipePoseLandmarkerApp(viewModel, cameraController)
        }
    }

    override fun onStop() {
        super.onStop()
        cameraController.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraController.release()
    }
}

@ComposePreview
@Composable
fun MediaPipePoseLandmarkerApp(
    viewModel: MainViewModel,
    cameraController: CameraController
) {
    MaterialTheme {
        Surface(color = MaterialTheme.colors.background) {
            when {
                !viewModel.hasCameraPermission -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera permission required.\nPlease grant camera access.",
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    CameraLandmarkerScreen(viewModel, cameraController)
                }
            }

            viewModel.errorMessage?.let { error ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = error,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@ComposePreview
@Composable
fun CameraLandmarkerScreen(
    viewModel: MainViewModel,
    cameraController: CameraController
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            this.scaleType = PreviewView.ScaleType.FILL_START
        }
    }
    val overlayView = remember { OverlayView(context, null) }

    DisposableEffect(key1 = cameraController, key2 = overlayView) {
        if (viewModel.hasCameraPermission) {
            cameraController.start(context, overlayView) { error ->
                viewModel.errorMessage = error
            }
        }

        onDispose {
            cameraController.stop()
        }
    }

    LaunchedEffect(previewView, cameraController) {
        if (viewModel.hasCameraPermission) {
            cameraController.bindCamera(context, lifecycleOwner, previewView) { error ->
                viewModel.errorMessage = error
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        AndroidView(
            factory = { overlayView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
