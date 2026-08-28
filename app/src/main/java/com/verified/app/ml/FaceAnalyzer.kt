package com.verified.app.ml

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val onConfidenceUpdate: (Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .build()

    private val detector = FaceDetection.getClient(options)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                val confidence = computeFaceConfidence(faces)
                onConfidenceUpdate(confidence)
            }
            .addOnFailureListener {
                onConfidenceUpdate(0)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Converts raw face detections to a 0–100 confidence score.
     *
     * Scoring:
     *  - At least one face detected:          +50 pts
     *  - Face is roughly centred (yaw < 25°): +20 pts
     *  - Eyes open probability both > 0.7:    +30 pts
     */
    private fun computeFaceConfidence(faces: List<Face>): Int {
        if (faces.isEmpty()) return 0

        val face = faces.first()
        var score = 50

        // Reward facing forward
        val yaw = Math.abs(face.headEulerAngleY)
        score += when {
            yaw < 10f -> 20
            yaw < 20f -> 10
            yaw < 35f -> 5
            else -> 0
        }

        // Reward both eyes open
        val leftEye = face.leftEyeOpenProbability ?: 0f
        val rightEye = face.rightEyeOpenProbability ?: 0f
        if (leftEye > 0.7f && rightEye > 0.7f) score += 30
        else if (leftEye > 0.5f || rightEye > 0.5f) score += 15

        return score.coerceIn(0, 100)
    }
}
