package com.verified.app.ml

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import android.os.SystemClock

// How often each model is allowed to run.
// NudeNet is synchronous and heavy → lower rate.
// Pose is async and lighter → higher rate.
private const val NUDENET_INTERVAL_MS = 200L  // ~5 fps
private const val POSE_INTERVAL_MS    = 100L  // ~10 fps

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
        val now = SystemClock.elapsedRealtime()
        val runNudeNet = now - lastNudeNetMs >= NUDENET_INTERVAL_MS
        val runPose    = now - lastPoseMs    >= POSE_INTERVAL_MS

        // Nothing is due this frame — drop it immediately so the preview stays smooth
        if (!runNudeNet && !runPose) {
            imageProxy.close()
            return
        }

        // ── NudeNet — synchronous on the bitmap ──────────────────────────────
        if (runNudeNet) {
            lastNudeNetMs = now
            val bitmap = imageProxy.toBitmap()
            onNudeNetResult(nudeNet.analyze(bitmap))
            // bitmap is short-lived; GC will collect it; no explicit recycle needed
            // because imageProxy.toBitmap() returns a copy
        }

        // ── Pose — async; proxy must stay open until the Task completes ──────
        if (runPose) {
            lastPoseMs = now
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return
            }
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
                    .addOnCompleteListener {
                        // Always close here — Pose was the last thing holding the proxy
                        imageProxy.close()
                    }
            }
        } else {
            // Only NudeNet ran this frame; Pose is not using the proxy
            imageProxy.close()
        }
    }

    private fun computeChestConfidence(
        pose: com.google.mlkit.vision.pose.Pose,
        imageHeight: Float,
    ): Int {
        val ls    = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rs    = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val lh    = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rh    = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val nose  = pose.getPoseLandmark(PoseLandmark.NOSE)
        val lw    = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rw    = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val thr = 0.55f
        var score = 0

        val lsOk = (ls?.inFrameLikelihood ?: 0f) > thr
        val rsOk = (rs?.inFrameLikelihood ?: 0f) > thr
        if (lsOk && rsOk) score += 40 else if (lsOk || rsOk) score += 15

        val lhOk = (lh?.inFrameLikelihood ?: 0f) > thr
        val rhOk = (rh?.inFrameLikelihood ?: 0f) > thr
        if (lhOk && rhOk) score += 20 else if (lhOk || rhOk) score += 8

        if (lsOk && rsOk) {
            val avgY = (ls!!.position.y + rs!!.position.y) / 2f
            if ((avgY / imageHeight) in 0.25f..0.75f) score += 20
        }

        val noseLikelihood = nose?.inFrameLikelihood ?: 0f
        score += when {
            noseLikelihood < 0.4f  -> 20
            noseLikelihood < 0.65f -> 10
            else                   -> 0
        }

        // Wrists slightly below shoulders — normalized offset so it's scale-invariant.
        // Positive = wrist is lower in frame than shoulder (y increases downward).
        // Window [0.04, 0.30] covers "slightly raised / near chest" without
        // capturing arms fully hanging at the sides (> 0.30) or raised above (< 0.04).
        val leftWristOk  = wristSlightlyBelowShoulder(ls, lw, imageHeight)
        val rightWristOk = wristSlightlyBelowShoulder(rs, rw, imageHeight)
        if (leftWristOk && rightWristOk) score += 20
        else if (leftWristOk || rightWristOk) score += 8

        return score.coerceIn(0, 100)
    }

    /**
     * Returns true when [wrist] is between [minBelow] and [maxBelow] of [imageHeight]
     * below [shoulder] — i.e. "slightly below", not raised above and not hanging far down.
     */
    private fun wristSlightlyBelowShoulder(
        shoulder: PoseLandmark?,
        wrist: PoseLandmark?,
        imageHeight: Float,
        minBelow: Float = 0.04f,
        maxBelow: Float = 0.30f,
    ): Boolean {
        if (shoulder == null || wrist == null) return false
        if (wrist.inFrameLikelihood < 0.5f) return false
        val normalizedOffset = (wrist.position.y - shoulder.position.y) / imageHeight
        return normalizedOffset in minBelow..maxBelow
    }
}
