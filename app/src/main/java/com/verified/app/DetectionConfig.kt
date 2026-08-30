package com.verified.app

/**
 * All tuneable thresholds and scoring weights in one place.
 *
 * Groups:
 *  - Frame throttling    — how often each model runs
 *  - NudeNet inference   — confidence / NMS cut-offs
 *  - Face detection      — scoring weights and gate threshold
 *  - Pose / chest        — landmark visibility and body-position scoring
 *  - Wrist position      — "slightly below shoulder" window
 *  - Verification gate   — hold duration driving the progress bar
 */
object DetectionConfig {

    // ── Frame throttling ──────────────────────────────────────────────────────
    /** Minimum ms between NudeNet inference calls (~5 fps). */
    const val NUDENET_INTERVAL_MS: Long = 200L
    /** Minimum ms between Pose inference calls (~10 fps). */
    const val POSE_INTERVAL_MS: Long    = 100L

    // ── NudeNet inference ─────────────────────────────────────────────────────
    /** Class score below this is discarded before NMS. */
    const val NUDENET_CONF_THRESHOLD: Float    = 0.25f
    /** IoU above this suppresses a lower-confidence duplicate box. */
    const val NUDENET_NMS_IOU_THRESHOLD: Float = 0.45f
    /** TFLite inference thread count. */
    const val NUDENET_THREADS: Int             = 2

    // ── Face detection — scoring ──────────────────────────────────────────────
    /** Baseline points awarded when any face is found in the frame. */
    const val FACE_BASE_SCORE: Int           = 50
    /** Minimum face size relative to the frame (0–1). */
    const val FACE_MIN_SIZE: Float           = 0.15f
    // Yaw (left/right head turn) reward bands
    const val FACE_YAW_TIGHT_DEG: Float      = 10f   // ≤ this → full reward
    const val FACE_YAW_MEDIUM_DEG: Float     = 20f
    const val FACE_YAW_LOOSE_DEG: Float      = 35f   // > this → no reward
    const val FACE_YAW_SCORE_TIGHT: Int      = 20
    const val FACE_YAW_SCORE_MEDIUM: Int     = 10
    const val FACE_YAW_SCORE_LOOSE: Int      = 5
    // Eye-open probability thresholds
    const val FACE_EYE_OPEN_HIGH: Float      = 0.7f  // both eyes open → full reward
    const val FACE_EYE_OPEN_LOW: Float       = 0.5f  // one eye open  → partial reward
    const val FACE_EYE_SCORE_BOTH: Int       = 30
    const val FACE_EYE_SCORE_ONE: Int        = 15
    /** Score must reach this to flip the stage to DETECTED. */
    const val FACE_DETECTED_THRESHOLD: Int   = 80

    // ── Pose / chest — scoring ────────────────────────────────────────────────
    /** Minimum inFrameLikelihood for a landmark to count as visible. */
    const val POSE_LANDMARK_THRESHOLD: Float = 0.55f
    // Shoulder visibility
    const val POSE_SHOULDER_SCORE_BOTH: Int  = 40
    const val POSE_SHOULDER_SCORE_ONE: Int   = 15
    // Hip visibility
    const val POSE_HIP_SCORE_BOTH: Int       = 20
    const val POSE_HIP_SCORE_ONE: Int        = 8
    // Shoulders should sit in the middle vertical band of the frame
    const val POSE_SHOULDER_Y_MIN: Float     = 0.25f  // normalised [0 = top, 1 = bottom]
    const val POSE_SHOULDER_Y_MAX: Float     = 0.75f
    const val POSE_SHOULDER_Y_SCORE: Int     = 20
    // Nose prominence (low = camera aimed at torso, not face)
    const val POSE_NOSE_LOW_THRESHOLD: Float  = 0.40f
    const val POSE_NOSE_HIGH_THRESHOLD: Float = 0.65f
    const val POSE_NOSE_SCORE_ABSENT: Int     = 20
    const val POSE_NOSE_SCORE_PARTIAL: Int    = 10

    // ── Wrist position ────────────────────────────────────────────────────────
    /** Minimum inFrameLikelihood for a wrist landmark to be trusted. */
    const val WRIST_LANDMARK_THRESHOLD: Float = 0.50f
    /**
     * Wrist must be at least this fraction of [imageHeight] below the shoulder.
     * Prevents arms-raised / wrists-at-shoulder-level from passing.
     */
    const val WRIST_BELOW_MIN: Float          = 0.04f
    /**
     * Wrist must be no more than this fraction of [imageHeight] below the shoulder.
     * Prevents arms-fully-hanging-at-sides from passing.
     */
    const val WRIST_BELOW_MAX: Float          = 0.30f
    const val WRIST_SCORE_BOTH: Int           = 20
    const val WRIST_SCORE_ONE: Int            = 8

    // ── Verification gate ─────────────────────────────────────────────────────
    /**
     * How long (ms) detection must be uninterrupted before the stage is verified.
     * This is the duration the [ScanProgressBar] fills over.
     */
    const val SCAN_HOLD_MS: Int = 800
}
