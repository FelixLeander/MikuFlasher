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
import com.verified.app.ml.PoseDetectionResult
import com.verified.app.viewmodel.DetectionState

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

@Composable
fun ChestOverlay(
    detectionState: DetectionState,
    poseResult: PoseDetectionResult?,
    showLiveSkeleton: Boolean,
    showGhostSkeleton: Boolean,
    onToggleLive: () -> Unit,
    onToggleGhost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Frosted stencil
        BackgroundStencil(
            modifier = Modifier.fillMaxSize(),
            stencil = R.drawable.torso_shape_fill,
            draw = R.drawable.torso_shape,
        )

        // Extremities bitmap — fill width, centred vertically
        Image(
            painter = painterResource(R.drawable.extremities),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.Center),
        )

        // Skeleton overlays (sit above stencil, below UI chrome)
        SkeletonOverlay(
            poseResult = poseResult,
            showLive = showLiveSkeleton,
            showGhost = showGhostSkeleton,
            modifier = Modifier.fillMaxSize(),
        )

        // Bottom chrome: toggles → stage label → scan status
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonToggleRow(
                showLive = showLiveSkeleton,
                showGhost = showGhostSkeleton,
                onToggleLive = onToggleLive,
                onToggleGhost = onToggleGhost,
            )

            Text(
                text = "One more step.",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            ScanStatusContent(
                detectionState = detectionState,
                scanningLabel = "Scanning…",
            )
        }
    }
}

// ── Shared status helpers ─────────────────────────────────────────────────────

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
fun ScanStatusContent(
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
