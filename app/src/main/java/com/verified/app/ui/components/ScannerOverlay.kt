package com.verified.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verified.app.R
import com.verified.app.viewmodel.DetectionState

/**
 * Face scan overlay: frosted stencil cutout of the face shape + status text.
 */
@Composable
fun FaceOverlay(
    detectionState: DetectionState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Stencil layer
        BackgroundStencil(
            modifier = Modifier.fillMaxSize(),
            stencil = R.drawable.face_shape_fill,
            draw = R.drawable.face_shape,
            scaleWidth = 1f,
            scaleHeight = 1f,
        )

        // Status text at the bottom
        ScanStatus(
            detectionState = detectionState,
            scanningLabel = "Scanning face…",
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Chest scan overlay: frosted stencil cutout of the torso shape,
 * with extremities drawn on top, + status text.
 */
@Composable
fun ChestOverlay(
    detectionState: DetectionState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Stencil layer
        BackgroundStencil(
            modifier = Modifier.fillMaxSize(),
            stencil = R.drawable.torso_shape_fill,
            draw = R.drawable.torso_shape,
            scaleWidth = 1.7f,
            scaleHeight = 1.7f,
        )

        // Extremities outline drawn on top of stencil
        Image(
            painter = painterResource(R.drawable.extremities),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )

        // Status text at the bottom
        ScanStatus(
            detectionState = detectionState,
            scanningLabel = "Scanning…",
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ── Shared status row ─────────────────────────────────────────────────────────

@Composable
private fun ScanStatus(
    detectionState: DetectionState,
    scanningLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (detectionState) {
            DetectionState.SCANNING, DetectionState.DETECTED -> {
                val infiniteTransition = rememberInfiniteTransition(label = "spin")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                    label = "spinAngle",
                )
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    text = scanningLabel,
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                )
            }

            DetectionState.VERIFIED -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = "Verified",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Thank you for your compliance.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
