package com.verified.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ScanStage { FACE, CHEST, COMPLETE }

enum class DetectionState { SCANNING, DETECTED, VERIFIED }

data class ScanUiState(
    val stage: ScanStage = ScanStage.FACE,
    val detectionState: DetectionState = DetectionState.SCANNING,
    val confidencePercent: Int = 0,
    val showOneMoreStep: Boolean = false,
)

class ScanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    // Called every frame by the analyzer with detection confidence 0..100
    fun onFaceDetectionUpdate(confidence: Int) {
        val state = _uiState.value
        if (state.stage != ScanStage.FACE || state.detectionState == DetectionState.VERIFIED) return

        _uiState.value = state.copy(
            confidencePercent = confidence.coerceIn(0, 100),
            detectionState = if (confidence >= 80) DetectionState.DETECTED else DetectionState.SCANNING
        )
    }

    fun onChestDetectionUpdate(confidence: Int) {
        val state = _uiState.value
        if (state.stage != ScanStage.CHEST || state.detectionState == DetectionState.VERIFIED) return

        _uiState.value = state.copy(
            confidencePercent = confidence.coerceIn(0, 100),
            detectionState = if (confidence >= 70) DetectionState.DETECTED else DetectionState.SCANNING
        )
    }

    // Called after the "VERIFIED" animation plays — advances stage
    fun onFaceVerified() {
        _uiState.value = _uiState.value.copy(
            detectionState = DetectionState.VERIFIED,
            confidencePercent = 100
        )
    }

    fun advanceToChestStage() {
        _uiState.value = ScanUiState(
            stage = ScanStage.CHEST,
            detectionState = DetectionState.SCANNING,
            confidencePercent = 0,
            showOneMoreStep = true
        )
    }

    fun onChestVerified() {
        _uiState.value = _uiState.value.copy(
            detectionState = DetectionState.VERIFIED,
            confidencePercent = 100
        )
    }

    fun advanceToComplete() {
        _uiState.value = ScanUiState(stage = ScanStage.COMPLETE)
    }

    fun reset() {
        _uiState.value = ScanUiState()
    }
}
