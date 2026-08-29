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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint as NativePaint
import android.graphics.Rect as NativeRect
import android.graphics.Typeface
import androidx.compose.ui.graphics.nativeCanvas
import com.google.mlkit.vision.pose.PoseLandmark
import com.verified.app.ml.NudeNetResult
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
 * Three horizontally-arranged pill toggle buttons.
 */
@Composable
fun SkeletonToggleRow(
    showLive: Boolean,
    showGhost: Boolean,
    showNudeNet: Boolean,
    onToggleLive: () -> Unit,
    onToggleGhost: () -> Unit,
    onToggleNudeNet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TogglePill(label = "Live",    active = showLive,    onClick = onToggleLive)
        TogglePill(label = "Guide",   active = showGhost,   onClick = onToggleGhost)
        TogglePill(label = "Classes", active = showNudeNet, onClick = onToggleNudeNet)
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

// ── NudeNet bounding-box overlay ─────────────────────────────────────────────

/**
 * Draws a labelled bounding box for every detection in [nudeNetResult].
 * Coordinates are mapped directly from the model's normalised [0,1] output
 * to canvas dimensions — works well for content near the centre of frame.
 */
@Composable
fun NudeNetBoxOverlay(
    nudeNetResult: NudeNetResult?,
    modifier: Modifier = Modifier,
) {
    if (nudeNetResult == null || nudeNetResult.detections.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val labelTextSize = 11.sp.toPx()
        val strokePx = 2.dp.toPx()
        val padPx = 4.dp.toPx()

        for (det in nudeNetResult.detections) {
            val color = nudeNetClassColor(det.classIndex)
            val argb = color.toArgb()

            val x1 = det.x1 * size.width
            val y1 = det.y1 * size.height
            val x2 = det.x2 * size.width
            val y2 = det.y2 * size.height

            // Semi-transparent fill
            nativeCanvas.drawRect(
                x1, y1, x2, y2,
                NativePaint().apply {
                    this.color = color.copy(alpha = 0.15f).toArgb()
                    style = NativePaint.Style.FILL
                }
            )

            // Box outline
            nativeCanvas.drawRect(
                x1, y1, x2, y2,
                NativePaint().apply {
                    this.color = argb
                    style = NativePaint.Style.STROKE
                    strokeWidth = strokePx
                    isAntiAlias = true
                }
            )

            // Label: class name + confidence %
            val label = "${det.className}  ${(det.confidence * 100).toInt()}%"
            val textPaint = NativePaint().apply {
                this.color = android.graphics.Color.WHITE
                textSize = labelTextSize
                isAntiAlias = true
                typeface = Typeface.MONOSPACE
            }
            val bounds = NativeRect()
            textPaint.getTextBounds(label, 0, label.length, bounds)

            val labelW = bounds.width() + padPx * 2
            val labelH = bounds.height() + padPx * 2
            // Clamp label above the box; if too close to top, draw inside instead
            val labelTop = if (y1 - labelH >= 0) y1 - labelH else y1

            nativeCanvas.drawRect(
                x1, labelTop, x1 + labelW, labelTop + labelH,
                NativePaint().apply { this.color = color.copy(alpha = 0.85f).toArgb() }
            )
            nativeCanvas.drawText(
                label,
                x1 + padPx,
                labelTop + labelH - padPx,
                textPaint,
            )
        }
    }
}

/** Consistent colour per class — grouped by semantic category. */
private fun nudeNetClassColor(classIndex: Int): Color = when (classIndex) {
    3    -> Color(0xFFFF4444)  // FEMALE_BREAST_EXPOSED   — red
    16   -> Color(0xFF44DD44)  // FEMALE_BREAST_COVERED   — green
    4    -> Color(0xFFFF0000)  // FEMALE_GENITALIA_EXPOSED — deep red
    0    -> Color(0xFF00BBBB)  // FEMALE_GENITALIA_COVERED — teal
    14   -> Color(0xFFDD2222)  // MALE_GENITALIA_EXPOSED   — dark red
    15   -> Color(0xFF009999)  // ANUS_COVERED             — dark teal
    6    -> Color(0xFFFF6600)  // ANUS_EXPOSED             — orange
    2    -> Color(0xFFFF8800)  // BUTTOCKS_EXPOSED         — amber
    17   -> Color(0xFF44AAAA)  // BUTTOCKS_COVERED         — light teal
    5    -> Color(0xFFFF9999)  // MALE_BREAST_EXPOSED      — light red
    1, 12 -> Color(0xFF6699FF) // FACE_FEMALE / FACE_MALE  — blue
    13   -> Color(0xFFFFCC44)  // BELLY_EXPOSED            — yellow
    8    -> Color(0xFFCCBB44)  // BELLY_COVERED            — dark yellow
    7, 9 -> Color(0xFFAAFFAA)  // FEET_EXPOSED / COVERED   — light green
    10,11 -> Color(0xFFCCCCFF) // ARMPITS                  — lavender
    else -> Color(0xFFCCCCCC)  // fallback                 — light grey
}
