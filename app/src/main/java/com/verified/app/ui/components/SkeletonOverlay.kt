package com.verified.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.verified.app.ml.PoseDetectionResult

// ── Connections to draw (pairs of PoseLandmark constants) ────────────────────

private val SKELETON_CONNECTIONS = listOf(
    PoseLandmark.LEFT_SHOULDER  to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_SHOULDER  to PoseLandmark.LEFT_ELBOW,
    PoseLandmark.LEFT_ELBOW     to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
    PoseLandmark.RIGHT_ELBOW    to PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_SHOULDER  to PoseLandmark.LEFT_HIP,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP       to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP       to PoseLandmark.LEFT_KNEE,
    PoseLandmark.RIGHT_HIP      to PoseLandmark.RIGHT_KNEE,
    PoseLandmark.NOSE           to PoseLandmark.LEFT_SHOULDER,
    PoseLandmark.NOSE           to PoseLandmark.RIGHT_SHOULDER,
)

// ── Ghost skeleton: ideal normalised positions (x, y) in [0..1] ─────────────
// These roughly match the torso stencil shape centred on screen.

private val GHOST_POSITIONS: Map<Int, Pair<Float, Float>> = mapOf(
    PoseLandmark.NOSE            to (0.50f to 0.08f),
    PoseLandmark.LEFT_SHOULDER   to (0.33f to 0.30f),
    PoseLandmark.RIGHT_SHOULDER  to (0.67f to 0.30f),
    PoseLandmark.LEFT_ELBOW      to (0.18f to 0.50f),
    PoseLandmark.RIGHT_ELBOW     to (0.82f to 0.50f),
    PoseLandmark.LEFT_WRIST      to (0.33f to 0.32f),
    PoseLandmark.RIGHT_WRIST     to (0.67f to 0.32f),
    PoseLandmark.LEFT_HIP        to (0.38f to 0.65f),
    PoseLandmark.RIGHT_HIP       to (0.62f to 0.65f),
    PoseLandmark.LEFT_KNEE       to (0.36f to 0.88f),
    PoseLandmark.RIGHT_KNEE      to (0.64f to 0.88f),
)

// ── Composables ──────────────────────────────────────────────────────────────

/**
 * Draws whichever skeleton overlays are toggled on.
 * Sits as a full-size layer on top of the camera / stencil.
 */
@Composable
fun SkeletonOverlay(
    poseResult: PoseDetectionResult?,
    showLive: Boolean,
    showGhost: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (showGhost) {
            drawGhostSkeleton(size.width, size.height)
        }
        if (showLive && poseResult?.pose != null) {
            drawLiveSkeleton(poseResult, size.width, size.height)
        }
    }
}

/**
 * Two horizontally-arranged pill toggle buttons.
 * Place these wherever suits the layout.
 */
@Composable
fun SkeletonToggleRow(
    showLive: Boolean,
    showGhost: Boolean,
    onToggleLive: () -> Unit,
    onToggleGhost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TogglePill(label = "Live", active = showLive, onClick = onToggleLive)
        TogglePill(label = "Guide", active = showGhost, onClick = onToggleGhost)
    }
}

@Composable
private fun TogglePill(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) Color.White else Color.White.copy(alpha = 0.20f)
    val fg = if (active) Color.Black else Color.White

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ── Drawing helpers ──────────────────────────────────────────────────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGhostSkeleton(
    canvasW: Float,
    canvasH: Float,
) {
    val color = Color.White.copy(alpha = 0.35f)
    val strokeWidth = 3.dp.toPx()
    val dotRadius = 5.dp.toPx()

    fun pos(landmark: Int): Offset? {
        val (nx, ny) = GHOST_POSITIONS[landmark] ?: return null
        return Offset(nx * canvasW, ny * canvasH)
    }

    // Connections
    for ((a, b) in SKELETON_CONNECTIONS) {
        val from = pos(a) ?: continue
        val to   = pos(b) ?: continue
        drawLine(color, from, to, strokeWidth, StrokeCap.Round)
    }

    // Dots
    for (landmark in GHOST_POSITIONS.keys) {
        val p = pos(landmark) ?: continue
        drawCircle(color, dotRadius, p)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiveSkeleton(
    result: PoseDetectionResult,
    canvasW: Float,
    canvasH: Float,
) {
    val pose = result.pose ?: return

    // Determine effective image dimensions after rotation.
    // When rotation is 90° or 270° the image is landscape but displayed portrait,
    // so width and height are transposed.
    val rotated = result.rotationDegrees == 90 || result.rotationDegrees == 270
    val imageW = if (rotated) result.imageHeight.toFloat() else result.imageWidth.toFloat()
    val imageH = if (rotated) result.imageWidth.toFloat()  else result.imageHeight.toFloat()

    fun landmarkToScreen(type: Int): Offset? {
        val lm = pose.getPoseLandmark(type) ?: return null
        if ((lm.inFrameLikelihood) < 0.4f) return null
        // Normalise to [0,1]
        val nx = lm.position.x / imageW
        val ny = lm.position.y / imageH
        // Mirror horizontally for front camera
        return Offset((1f - nx) * canvasW, ny * canvasH)
    }

    val highConf = Color(0xFF00C853.toInt())   // bright green — well-detected joint
    val lowConf  = Color(0xFFFFD600.toInt())   // amber — low-confidence joint
    val lineColor = Color.White.copy(alpha = 0.75f)
    val strokeWidth = 3.dp.toPx()
    val dotRadius = 6.dp.toPx()

    // Connections
    for ((a, b) in SKELETON_CONNECTIONS) {
        val from = landmarkToScreen(a) ?: continue
        val to   = landmarkToScreen(b) ?: continue
        drawLine(lineColor, from, to, strokeWidth, StrokeCap.Round)
    }

    // Dots — colour by individual confidence
    for ((a, _) in SKELETON_CONNECTIONS + SKELETON_CONNECTIONS.map { it.second to it.first }) {
        val lm = pose.getPoseLandmark(a) ?: continue
        val p  = landmarkToScreen(a) ?: continue
        val dotColor = if (lm.inFrameLikelihood > 0.7f) highConf else lowConf
        drawCircle(dotColor, dotRadius, p)
    }
}
