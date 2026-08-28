package com.verified.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verified.app.R
import com.verified.app.viewmodel.DetectionState
import androidx.compose.foundation.Image

/**
 * Face scan overlay: frosted stencil cutout of the face shape + status text.
 */
@Composable
fun FaceOverlay(
    detectionState: DetectionState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        BackgroundStencil(
            modifier = Modifier.fillMaxSize(),
            stencil = R.drawable.face_shape_fill,
            draw = R.drawable.face_shape,
        )

        ScanStatus(
            detectionState = detectionState,
            scanningLabel = "Scanning face…",
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Chest scan overlay: frosted stencil cutout of the torso shape,
 * with extremities drawn on top, + permanent stage label + status text.
 */
@Composable
fun ChestOverlay(
    detectionState: DetectionState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        BackgroundStencil(
            modifier = Modifier.fillMaxSize(),
            stencil = R.drawable.torso_shape_fill,
            draw = R.drawable.torso_shape,
        )

        // Extremities bitmap scaled to fill width, matching stencil
        Image(
            painter = painterResource(R.drawable.extremities),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.Center),
        )

        // Stage label + scan status always at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "One more step.",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            ScanStatusContent(
                detectionState = detectionState,
                scanningLabel = "Scanning…",
            )
        }
    }
}

// ── Shared status content ─────────────────────────────────────────────────────

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
        ScanStatusContent(detectionState, scanningLabel)
    }
}

@Composable
private fun ScanStatusContent(
    detectionState: DetectionState,
    scanningLabel: String,
) {
    when (detectionState) {
        DetectionState.SCANNING, DetectionState.DETECTED -> {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(4.dp))
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
