package com.verified.app.viewmodel

import androidx.lifecycle.ViewModel
import com.verified.app.ml.PoseDetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ScanStage { FACE, CHEST, COMPLETE }
enum class DetectionState { SCANNING, DETECTED, VERIFIED }

data class ScanUiState(
    val stage: ScanStage = ScanStage.FACE,
    val detectionState: DetectionState = DetectionState.SCANNING,
    val confidencePercent: Int = 0,
    // Skeleton overlays
    val poseResult: PoseDetectionResult? = null,
    val showLiveSkeleton: Boolean = false,
    val showGhostSkeleton: Boolean = false,
)

class ScanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onFaceDetectionUpdate(confidence: Int) {
        val s = _uiState.value
        if (s.stage != ScanStage.FACE || s.detectionState == DetectionState.VERIFIED) return
        _uiState.value = s.copy(
            confidencePercent = confidence.coerceIn(0, 100),
            detectionState = if (confidence >= 80) DetectionState.DETECTED else DetectionState.SCANNING,
        )
    }

    fun onPoseResult(result: PoseDetectionResult) {
        val s = _uiState.value
        if (s.stage != ScanStage.CHEST || s.detectionState == DetectionState.VERIFIED) return
        _uiState.value = s.copy(
            confidencePercent = result.confidence.coerceIn(0, 100),
            poseResult = result,
            detectionState = if (result.confidence >= 70) DetectionState.DETECTED else DetectionState.SCANNING,
        )
    }

    fun toggleLiveSkeleton() {
        _uiState.value = _uiState.value.copy(showLiveSkeleton = !_uiState.value.showLiveSkeleton)
    }

    fun toggleGhostSkeleton() {
        _uiState.value = _uiState.value.copy(showGhostSkeleton = !_uiState.value.showGhostSkeleton)
    }

    fun onFaceVerified() {
        _uiState.value = _uiState.value.copy(detectionState = DetectionState.VERIFIED, confidencePercent = 100)
    }

    fun advanceToChestStage() {
        _uiState.value = ScanUiState(stage = ScanStage.CHEST)
    }

    fun onChestVerified() {
        _uiState.value = _uiState.value.copy(detectionState = DetectionState.VERIFIED, confidencePercent = 100)
    }

    fun advanceToComplete() {
        _uiState.value = ScanUiState(stage = ScanStage.COMPLETE)
    }

    fun reset() {
        _uiState.value = ScanUiState()
    }
}
