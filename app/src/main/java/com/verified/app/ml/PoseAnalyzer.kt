package com.verified.app.ml

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

/**
 * Carries both the detection confidence and the raw pose for skeleton rendering.
 * [imageWidth] / [imageHeight] are the pre-rotation pixel dimensions of the
 * analysed frame — needed to normalise landmark coordinates for display.
 */
data class PoseDetectionResult(
    val confidence: Int,
    val pose: Pose?,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int,
)

class PoseAnalyzer(
    private val onResult: (PoseDetectionResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)
        val imgW = imageProxy.width
        val imgH = imageProxy.height

        detector.process(image)
            .addOnSuccessListener { pose ->
                val confidence = computeChestConfidence(pose, imgH.toFloat())
                onResult(PoseDetectionResult(confidence, pose, imgW, imgH, rotation))
            }
            .addOnFailureListener {
                onResult(PoseDetectionResult(0, null, imgW, imgH, rotation))
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun computeChestConfidence(pose: Pose, imageHeight: Float): Int {
        val leftShoulder  = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip       = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip      = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val nose          = pose.getPoseLandmark(PoseLandmark.NOSE)

        var score = 0
        val threshold = 0.55f

        val leftShoulderOk  = (leftShoulder?.inFrameLikelihood  ?: 0f) > threshold
        val rightShoulderOk = (rightShoulder?.inFrameLikelihood ?: 0f) > threshold
        if (leftShoulderOk && rightShoulderOk) score += 40
        else if (leftShoulderOk || rightShoulderOk) score += 15

        val leftHipOk  = (leftHip?.inFrameLikelihood  ?: 0f) > threshold
        val rightHipOk = (rightHip?.inFrameLikelihood ?: 0f) > threshold
        if (leftHipOk && rightHipOk) score += 20
        else if (leftHipOk || rightHipOk) score += 8

        if (leftShoulderOk && rightShoulderOk) {
            val avgY = (leftShoulder!!.position.y + rightShoulder!!.position.y) / 2f
            if ((avgY / imageHeight) in 0.25f..0.75f) score += 20
        }

        val noseLikelihood = nose?.inFrameLikelihood ?: 0f
        if (noseLikelihood < 0.4f)      score += 20
        else if (noseLikelihood < 0.65f) score += 10

        return score.coerceIn(0, 100)
    }
}
