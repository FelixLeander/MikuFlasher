package com.verified.app.ml

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.verified.app.DetectionConfig

class FaceAnalyzer(
    private val onConfidenceUpdate: (Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(DetectionConfig.FACE_MIN_SIZE)
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
                onConfidenceUpdate(computeFaceConfidence(faces))
            }
            .addOnFailureListener {
                onConfidenceUpdate(0)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    /**
     * Scoring (max 100):
     *  - At least one face detected:                      +[FACE_BASE_SCORE]
     *  - Face yaw within tight/medium/loose band:         +[FACE_YAW_SCORE_*]
     *  - Both eyes open probability above threshold:      +[FACE_EYE_SCORE_BOTH]
     *  - One eye open:                                    +[FACE_EYE_SCORE_ONE]
     */
    private fun computeFaceConfidence(faces: List<Face>): Int {
        if (faces.isEmpty()) return 0

        val face = faces.first()
        var score = DetectionConfig.FACE_BASE_SCORE

        val yaw = Math.abs(face.headEulerAngleY)
        score += when {
            yaw < DetectionConfig.FACE_YAW_TIGHT_DEG  -> DetectionConfig.FACE_YAW_SCORE_TIGHT
            yaw < DetectionConfig.FACE_YAW_MEDIUM_DEG -> DetectionConfig.FACE_YAW_SCORE_MEDIUM
            yaw < DetectionConfig.FACE_YAW_LOOSE_DEG  -> DetectionConfig.FACE_YAW_SCORE_LOOSE
            else                                       -> 0
        }

        val leftEye  = face.leftEyeOpenProbability  ?: 0f
        val rightEye = face.rightEyeOpenProbability ?: 0f
        score += when {
            leftEye  > DetectionConfig.FACE_EYE_OPEN_HIGH &&
            rightEye > DetectionConfig.FACE_EYE_OPEN_HIGH -> DetectionConfig.FACE_EYE_SCORE_BOTH
            leftEye  > DetectionConfig.FACE_EYE_OPEN_LOW  ||
            rightEye > DetectionConfig.FACE_EYE_OPEN_LOW  -> DetectionConfig.FACE_EYE_SCORE_ONE
            else -> 0
        }

        return score.coerceIn(0, 100)
    }
}
