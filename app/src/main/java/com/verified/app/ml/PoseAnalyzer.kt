package com.verified.app.ml

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

class PoseAnalyzer(
    private val onConfidenceUpdate: (Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val imageHeight = imageProxy.height.toFloat()

        detector.process(image)
            .addOnSuccessListener { pose ->
                val confidence = computeChestConfidence(pose, imageHeight)
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
     * Scores 0–100 for "chest is in frame".
     *
     * Strategy:
     *  - Both shoulders visible:             +40 pts
     *  - Both hips visible:                  +20 pts
     *  - Shoulders are in the vertical middle
     *    third of the frame (not face-only): +20 pts
     *  - Nose/face is NOT dominating frame
     *    (nose Y < 20% of frame height):     +20 pts bonus
     *    i.e. user has tilted camera down to show chest
     */
    private fun computeChestConfidence(
        pose: com.google.mlkit.vision.pose.Pose,
        imageHeight: Float
    ): Int {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        var score = 0
        val threshold = 0.55f

        // Shoulders visible
        val leftShoulderOk = (leftShoulder?.inFrameLikelihood ?: 0f) > threshold
        val rightShoulderOk = (rightShoulder?.inFrameLikelihood ?: 0f) > threshold
        if (leftShoulderOk && rightShoulderOk) score += 40
        else if (leftShoulderOk || rightShoulderOk) score += 15

        // Hips visible
        val leftHipOk = (leftHip?.inFrameLikelihood ?: 0f) > threshold
        val rightHipOk = (rightHip?.inFrameLikelihood ?: 0f) > threshold
        if (leftHipOk && rightHipOk) score += 20
        else if (leftHipOk || rightHipOk) score += 8

        // Shoulders should be roughly in the middle vertical band of the frame
        if (leftShoulderOk && rightShoulderOk) {
            val avgShoulderY = ((leftShoulder!!.position.y + rightShoulder!!.position.y) / 2f)
            val normalised = avgShoulderY / imageHeight  // 0 = top, 1 = bottom
            // Middle third: 0.33..0.66
            if (normalised in 0.25f..0.75f) score += 20
        }

        // Face not prominently in frame (camera is aimed at torso)
        val noseLikelihood = nose?.inFrameLikelihood ?: 0f
        if (noseLikelihood < 0.4f) {
            score += 20 // face not detected → camera pointed at chest
        } else if (noseLikelihood < 0.65f) {
            score += 10
        }

        return score.coerceIn(0, 100)
    }
}
