package com.verified.app.viewmodel

import androidx.lifecycle.ViewModel
import com.verified.app.ml.BreastState
import com.verified.app.ml.NudeNetResult
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
    // NudeNet
    val nudeNetResult: NudeNetResult? = null,
    // Skeleton overlay
    val poseResult: PoseDetectionResult? = null,
    val showLiveSkeleton: Boolean = false,
    val showGhostSkeleton: Boolean = false,
    val showNudeNetOverlay: Boolean = false,
)

class ScanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // ── Stage 1: face ─────────────────────────────────────────────────────────

    fun onFaceDetectionUpdate(confidence: Int) {
        val s = _uiState.value
        if (s.stage != ScanStage.FACE || s.detectionState == DetectionState.VERIFIED) return
        _uiState.value = s.copy(
            confidencePercent = confidence.coerceIn(0, 100),
            detectionState = if (confidence >= 80) DetectionState.DETECTED
                             else DetectionState.SCANNING,
        )
    }

    fun onFaceVerified() {
        _uiState.value = _uiState.value.copy(
            detectionState = DetectionState.VERIFIED,
            confidencePercent = 100,
        )
    }

    fun advanceToChestStage() {
        _uiState.value = ScanUiState(stage = ScanStage.CHEST)
    }

    // ── Stage 2: chest ────────────────────────────────────────────────────────

    /**
     * NudeNet drives verification for Stage 2.
     * Any breast detection (covered or exposed) above threshold counts as DETECTED.
     */
    fun onNudeNetResult(result: NudeNetResult) {
        val s = _uiState.value
        if (s.stage != ScanStage.CHEST || s.detectionState == DetectionState.VERIFIED) return
        _uiState.value = s.copy(
            nudeNetResult = result,
            confidencePercent = (result.breastConfidence * 100).toInt().coerceIn(0, 100),
            detectionState = if (result.breastState != BreastState.NONE) DetectionState.DETECTED
                             else DetectionState.SCANNING,
        )
    }

    /** Pose result feeds the skeleton overlay only — does not affect verification. */
    fun onPoseResult(result: PoseDetectionResult) {
        val s = _uiState.value
        if (s.stage != ScanStage.CHEST || s.detectionState == DetectionState.VERIFIED) return
        _uiState.value = s.copy(poseResult = result)
    }

    fun onChestVerified() {
        _uiState.value = _uiState.value.copy(
            detectionState = DetectionState.VERIFIED,
            confidencePercent = 100,
        )
    }

    fun advanceToComplete() {
        _uiState.value = ScanUiState(stage = ScanStage.COMPLETE)
    }

    // ── Skeleton toggles ──────────────────────────────────────────────────────

    fun toggleLiveSkeleton() {
        _uiState.value = _uiState.value.copy(
            showLiveSkeleton = !_uiState.value.showLiveSkeleton,
        )
    }

    fun toggleGhostSkeleton() {
        _uiState.value = _uiState.value.copy(
            showGhostSkeleton = !_uiState.value.showGhostSkeleton,
        )
    }

    fun toggleNudeNetOverlay() {
        _uiState.value = _uiState.value.copy(
            showNudeNetOverlay = !_uiState.value.showNudeNetOverlay,
        )
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    fun reset() {
        _uiState.value = ScanUiState()
    }
}
