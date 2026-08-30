package com.verified.app.ml

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.verified.app.DetectionConfig

class ChestFrameAnalyzer(
    private val nudeNet: NudeNetAnalyzer,
    private val onNudeNetResult: (NudeNetResult) -> Unit,
    private val onPoseResult: (PoseDetectionResult) -> Unit,
) : ImageAnalysis.Analyzer {

    private val poseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private var lastNudeNetMs = 0L
    private var lastPoseMs    = 0L

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val now        = SystemClock.elapsedRealtime()
        val runNudeNet = now - lastNudeNetMs >= DetectionConfig.NUDENET_INTERVAL_MS
        val runPose    = now - lastPoseMs    >= DetectionConfig.POSE_INTERVAL_MS

        if (!runNudeNet && !runPose) {
            imageProxy.close()
            return
        }

        if (runNudeNet) {
            lastNudeNetMs = now
            onNudeNetResult(nudeNet.analyze(imageProxy.toBitmap()))
        }

        if (runPose) {
            lastPoseMs = now
            val mediaImage = imageProxy.image
            if (mediaImage == null) { imageProxy.close(); return }
            val rotation = imageProxy.imageInfo.rotationDegrees
            val imgW = imageProxy.width
            val imgH = imageProxy.height

            InputImage.fromMediaImage(mediaImage, rotation).let { input ->
                poseDetector.process(input)
                    .addOnSuccessListener { pose ->
                        val conf = computeChestConfidence(pose, imgH.toFloat())
                        onPoseResult(PoseDetectionResult(conf, pose, imgW, imgH, rotation))
                    }
                    .addOnFailureListener {
                        onPoseResult(PoseDetectionResult(0, null, imgW, imgH, rotation))
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
        } else {
            imageProxy.close()
        }
    }

    private fun computeChestConfidence(
        pose: com.google.mlkit.vision.pose.Pose,
        imageHeight: Float,
    ): Int {
        val ls   = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rs   = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val lh   = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rh   = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val lw   = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rw   = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val thr = DetectionConfig.POSE_LANDMARK_THRESHOLD
        var score = 0

        val lsOk = (ls?.inFrameLikelihood ?: 0f) > thr
        val rsOk = (rs?.inFrameLikelihood ?: 0f) > thr
        if (lsOk && rsOk) score += DetectionConfig.POSE_SHOULDER_SCORE_BOTH
        else if (lsOk || rsOk) score += DetectionConfig.POSE_SHOULDER_SCORE_ONE

        val lhOk = (lh?.inFrameLikelihood ?: 0f) > thr
        val rhOk = (rh?.inFrameLikelihood ?: 0f) > thr
        if (lhOk && rhOk) score += DetectionConfig.POSE_HIP_SCORE_BOTH
        else if (lhOk || rhOk) score += DetectionConfig.POSE_HIP_SCORE_ONE

        if (lsOk && rsOk) {
            val avgY = (ls!!.position.y + rs!!.position.y) / 2f
            if ((avgY / imageHeight) in DetectionConfig.POSE_SHOULDER_Y_MIN..DetectionConfig.POSE_SHOULDER_Y_MAX)
                score += DetectionConfig.POSE_SHOULDER_Y_SCORE
        }

        val noseLikelihood = nose?.inFrameLikelihood ?: 0f
        score += when {
            noseLikelihood < DetectionConfig.POSE_NOSE_LOW_THRESHOLD  -> DetectionConfig.POSE_NOSE_SCORE_ABSENT
            noseLikelihood < DetectionConfig.POSE_NOSE_HIGH_THRESHOLD -> DetectionConfig.POSE_NOSE_SCORE_PARTIAL
            else -> 0
        }

        val leftWristOk  = wristSlightlyBelowShoulder(ls, lw, imageHeight)
        val rightWristOk = wristSlightlyBelowShoulder(rs, rw, imageHeight)
        if (leftWristOk && rightWristOk) score += DetectionConfig.WRIST_SCORE_BOTH
        else if (leftWristOk || rightWristOk) score += DetectionConfig.WRIST_SCORE_ONE

        return score.coerceIn(0, 100)
    }

    private fun wristSlightlyBelowShoulder(
        shoulder: com.google.mlkit.vision.pose.PoseLandmark?,
        wrist: com.google.mlkit.vision.pose.PoseLandmark?,
        imageHeight: Float,
    ): Boolean {
        if (shoulder == null || wrist == null) return false
        if (wrist.inFrameLikelihood < DetectionConfig.WRIST_LANDMARK_THRESHOLD) return false
        val normalizedOffset = (wrist.position.y - shoulder.position.y) / imageHeight
        return normalizedOffset in DetectionConfig.WRIST_BELOW_MIN..DetectionConfig.WRIST_BELOW_MAX
    }
}
