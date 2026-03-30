/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mediapipe.examples.poselandmarker

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.core.content.ContextCompat.getMainExecutor
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.examples.poselandmarker.PoseLandmarkerHelper
import com.google.mediapipe.examples.poselandmarker.OverlayView
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private var hasCameraPermission by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            MediaPipePoseLandmarkerApp(hasCameraPermission = hasCameraPermission)
        }
    }
}

@Composable
fun MediaPipePoseLandmarkerApp(hasCameraPermission: Boolean) {
    MaterialTheme {
        Surface(color = MaterialTheme.colors.background) {
            if (hasCameraPermission) {
                CameraLandmarkerScreen()
            } else {
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
        }
    }
}

@Composable
fun CameraLandmarkerScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val viewModel = MainViewModel()

    val previewView = remember {
        PreviewView(context).apply {
            this.scaleType = PreviewView.ScaleType.FILL_START
        }
    }
    val overlayView = remember {
        OverlayView(context, null)
    }

    val poseLandmarkerHelper = remember { mutableStateOf<PoseLandmarkerHelper?>(null) }
    val backgroundExecutor = remember { Executors.newSingleThreadExecutor() }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = object : PoseLandmarkerHelper.LandmarkerListener {
            override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
                (context as? Activity)?.runOnUiThread {
                    if (resultBundle.results.isNotEmpty()) {
                        overlayView.setResults(
                            resultBundle.results.first(),
                            resultBundle.inputImageHeight,
                            resultBundle.inputImageWidth,
                            RunningMode.LIVE_STREAM
                        )
                        overlayView.invalidate()
                    }
                }
            }

            override fun onError(error: String, errorCode: Int) {
                (context as? Activity)?.runOnUiThread {
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    errorMessage.value = error
                }
            }
        }

        backgroundExecutor.execute {
            try {
                poseLandmarkerHelper.value = PoseLandmarkerHelper(
                    context = context,
                    runningMode = RunningMode.LIVE_STREAM,
                    minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                    minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                    minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                    currentDelegate = viewModel.currentDelegate,
                    currentModel = viewModel.currentModel,
                    poseLandmarkerHelperListener = listener
                )
            } catch (e: Exception) {
                errorMessage.value = "Pose Landmarker init failed: ${e.message}"
            }
        }

        onDispose {
            poseLandmarkerHelper.value?.clearPoseLandmarker()
            backgroundExecutor.shutdown()
            backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    LaunchedEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(previewView.display.rotation)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(previewView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalyzer.setAnalyzer(backgroundExecutor) { imageProxy ->
                poseLandmarkerHelper.value?.detectLiveStream(
                    imageProxy,
                    false
                )
            }

            cameraProvider.unbindAll()

            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                preview.setSurfaceProvider(previewView.surfaceProvider)
            } catch (exc: Exception) {
                errorMessage.value = "Camera bind failed: ${exc.message}"
            }
        }, ContextCompat.getMainExecutor(context))
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

        errorMessage.value?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
    }
}
