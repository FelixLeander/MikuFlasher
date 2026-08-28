package com.verified.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verified.app.ui.theme.*

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = "VERIFIED",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 36.sp,
                letterSpacing = 8.sp,
                color = MatrixGreen
            )

            Text(
                text = "━━━━━━━━━━━━━━━",
                fontFamily = FontFamily.Monospace,
                color = MatrixGreenDim
            )

            Text(
                text = "CAMERA ACCESS REQUIRED",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                color = OnSurface
            )

            Text(
                text = "This application requires camera access to perform biometric identity verification.\n\nYour compliance is appreciated.",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = OnSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MatrixGreen,
                    contentColor = BackgroundBlack
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "GRANT ACCESS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun PermissionDeniedScreen(onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = "ACCESS\nDENIED",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                letterSpacing = 4.sp,
                color = DangerRed,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Camera permission was denied.\nPlease enable it in app settings to proceed.",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = OnSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                modifier = Modifier.border(1.dp, DangerRed, RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = "OPEN SETTINGS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
