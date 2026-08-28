package com.verified.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// Palette
val MatrixGreen = Color(0xFF00FF41)
val MatrixGreenDim = Color(0xFF009924)
val DangerRed = Color(0xFFFF2222)
val DangerRedDark = Color(0xFFAA0000)
val BackgroundBlack = Color(0xFF000000)
val SurfaceDark = Color(0xFF0A0A0A)
val OnSurface = Color(0xFFCCCCCC)
val SuccessGlow = Color(0xFF00FF88)

private val DarkColorScheme = darkColorScheme(
    primary = MatrixGreen,
    onPrimary = BackgroundBlack,
    background = BackgroundBlack,
    surface = SurfaceDark,
    onBackground = MatrixGreen,
    onSurface = OnSurface,
    error = DangerRed,
)

val TerminalTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        color = MatrixGreen
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        color = MatrixGreen
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = OnSurface
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = OnSurface
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        color = MatrixGreenDim
    )
)

@Composable
fun VerifiedAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = TerminalTypography,
        content = content
    )
}
