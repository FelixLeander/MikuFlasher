package com.verified.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verified.app.DetectionConfig

/**
 * A thin progress bar that fills linearly over [durationMs] while [active] is true.
 * Snaps back to zero the moment [active] becomes false (interrupted detection).
 * Calls [onComplete] exactly once when the fill reaches 1.0.
 *
 * Reusable across scan stages: wire [active] to your detection state and
 * [onComplete] to the verification callback for that stage.
 */
@Composable
fun ScanProgressBar(
    active: Boolean,
    modifier: Modifier = Modifier,
    durationMs: Int = DetectionConfig.SCAN_HOLD_MS,
    barHeight: Dp = 2.dp,
    color: Color = Color.White.copy(alpha = 0.45f),
    trackColor: Color = Color.White.copy(alpha = 0.08f),
    onComplete: () -> Unit = {},
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(active) {
        if (active) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = LinearEasing),
            )
            onComplete()
        } else {
            progress.snapTo(0f)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight),
    ) {
        // Track
        drawRect(color = trackColor)
        // Fill
        drawRect(
            color = color,
            size = Size(width = size.width * progress.value, height = size.height),
        )
    }
}
