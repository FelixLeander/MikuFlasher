package com.verified.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.ui.theme.*
import com.verified.app.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CompleteScreen(
    viewModel: ScanViewModel = viewModel(),
    onReplay: () -> Unit
) {
    // Blinking cursor effect for the terminal aesthetic
    var cursorVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(530.milliseconds)
            cursorVisible = !cursorVisible
        }
    }

    // Staggered text reveal
    var line1Visible by remember { mutableStateOf(false) }
    var line2Visible by remember { mutableStateOf(false) }
    var line3Visible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200.milliseconds);  line1Visible = true
        delay(600.milliseconds);  line2Visible = true
        delay(500.milliseconds);  line3Visible = true
        delay(800.milliseconds);  buttonVisible = true
    }

    // Pulse animation for the main ACCESS GRANTED text
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            // Top separator line
            if (line1Visible) {
                Text(
                    text = "━━━━━━━━━━━━━━━━━━━━━━",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MatrixGreenDim
                )
            }

            // Main verdict
            if (line1Visible) {
                Text(
                    text = "ACCESS\nGRANTED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 52.sp,
                    letterSpacing = 6.sp,
                    lineHeight = 60.sp,
                    textAlign = TextAlign.Center,
                    color = MatrixGreen.copy(alpha = pulse),
                    modifier = Modifier.alpha(pulse)
                )
            }

            if (line2Visible) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "IDENTITY VERIFIED ✓",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = SuccessGlow
                    )
                    Text(
                        text = "BIOMETRICS VERIFIED ✓",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = SuccessGlow
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Thank you for your compliance.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = OnSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (line3Visible) {
                // Terminal log lines
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MatrixGreenDim.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .padding(12.dp)
                ) {
                    TerminalLine("> STAGE_1 [FACE]   ........... OK")
                    TerminalLine("> STAGE_2 [CHEST]  ........... OK")
                    TerminalLine("> COMPLIANCE_SCORE ........... 100%")
                    Row {
                        TerminalLine("> AWAITING_NEXT_DIRECTIVE ")
                        if (cursorVisible) {
                            Text(
                                text = "█",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MatrixGreen
                            )
                        }
                    }
                }
            }

            // Bottom separator
            if (line3Visible) {
                Text(
                    text = "━━━━━━━━━━━━━━━━━━━━━━",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = MatrixGreenDim
                )
            }

            // Replay button
            if (buttonVisible) {
                Button(
                    onClick = {
                        viewModel.reset()
                        onReplay()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MatrixGreen
                    ),
                    modifier = Modifier
                        .border(1.dp, MatrixGreen, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "[ RUN VERIFICATION AGAIN ]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalLine(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = MatrixGreen.copy(alpha = 0.8f)
    )
}
