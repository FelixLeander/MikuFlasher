package com.google.mediapipe.examples.poselandmarker

import android.app.Activity
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.lifecycle.ProcessCameraProvider as CameraProvider

class CameraController(private val viewModel: MainViewModel) {

    private var poseLandmarkerHelper: PoseLandmarkerHelper? = null
    private var cameraProvider: CameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var preview: Preview? = null
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private fun createLandmarkerListener(
        context: Context,
        overlay: OverlayView,
        onError: (String) -> Unit
    ): PoseLandmarkerHelper.LandmarkerListener {
        return object : PoseLandmarkerHelper.LandmarkerListener {
            override fun onResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
                (context as? Activity)?.runOnUiThread {
                    if (resultBundle.results.isNotEmpty()) {
                        overlay.setResults(
                            resultBundle.results.first(),
                            resultBundle.inputImageHeight,
                            resultBundle.inputImageWidth,
                            RunningMode.LIVE_STREAM
                        )
                        overlay.invalidate()
                    }
                }
            }

            override fun onError(error: String, errorCode: Int) {
                onError(error)
            }
        }
    }

    fun start(cameraContext: Context, overlayView: OverlayView, onError: (String) -> Unit) {
        viewModel.errorMessage = null

        val listener = createLandmarkerListener(cameraContext, overlayView, onError)

        backgroundExecutor.execute {
            try {
                poseLandmarkerHelper = PoseLandmarkerHelper(
                    context = cameraContext,
                    runningMode = RunningMode.LIVE_STREAM,
                    minPoseDetectionConfidence = viewModel.currentMinPoseDetectionConfidence,
                    minPoseTrackingConfidence = viewModel.currentMinPoseTrackingConfidence,
                    minPosePresenceConfidence = viewModel.currentMinPosePresenceConfidence,
                    currentDelegate = viewModel.currentDelegate,
                    currentModel = viewModel.currentModel,
                    poseLandmarkerHelperListener = listener
                )
                viewModel.isCameraRunning = true
            } catch (e: Exception) {
                onError("Pose Landmarker init failed: ${e.message}")
                viewModel.errorMessage = "Pose Landmarker init failed: ${e.message}"
            }
        }
    }

    fun bindCamera(
        cameraContext: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onError: (String) -> Unit
    ) {
        val future = CameraProvider.getInstance(cameraContext)
        future.addListener({
            try {
                val cameraProvider = future.get()
                this.cameraProvider = cameraProvider

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()

                val previewBuild = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setTargetRotation(previewView.display.rotation)
                    .build()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .setTargetRotation(previewView.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                imageAnalysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                    poseLandmarkerHelper?.detectLiveStream(imageProxy, false)
                }

                this.preview = previewBuild
                this.imageAnalyzer = imageAnalysis

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewBuild,
                    imageAnalysis
                )
                previewBuild.surfaceProvider = previewView.surfaceProvider
            } catch (e: Exception) {
                onError("Camera bind failed: ${e.message}")
                viewModel.errorMessage = "Camera bind failed: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(cameraContext))
    }

    fun stop() {
        poseLandmarkerHelper?.clearPoseLandmarker()
        poseLandmarkerHelper = null
        cameraProvider?.unbindAll()
        viewModel.isCameraRunning = false
    }

    fun release() {
        stop()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
    }
}
