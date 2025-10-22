package com.example.flasherverifyer

import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true)
@Composable
fun TestPreview() {
    CustomFaceHoleOverlay(
        modifier = Modifier.fillMaxSize(),
        overlayColor = Color.Black.copy(alpha = 0.8f)
    )
}

@Preview(showBackground = true)
@Composable
fun FocusedVectorWithDarkAround() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.6f))
            val path = Path().apply {
                addOval(Rect(center = center, radius = 100f))
            }
            clipPath(path, clipOp = ClipOp.Difference) {
                drawRect(Color.Transparent)
            }
        }
        Icon(
            painter = painterResource(R.drawable.face_shape_miku),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize().scale(0.8f)
        )
    }
}

@Composable
fun CustomFaceHoleOverlay(
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = 0.6f)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val layer = native.saveLayer(0f, 0f, w, h, null)

            // Draw semi-transparent overlay
            val paint = android.graphics.Paint().apply {
                color = overlayColor.toArgb()
                style = android.graphics.Paint.Style.FILL
            }
            native.drawRect(0f, 0f, w, h, paint)

            // Punch a transparent “face-like” shape
            val clearPaint = android.graphics.Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                style = android.graphics.Paint.Style.FILL
            }

            val path = Path().apply {
                // Approximate head shape: top round, sides wide, chin narrower
                moveTo(w * 0.3f, h * 0.25f)                      // left temple
                cubicTo(
                    w * 0.25f, h * 0.45f,                        // left cheek
                    w * 0.25f, h * 0.65f,                        // jaw start
                    w * 0.4f, h * 0.8f                           // left jaw
                )
                cubicTo(
                    w * 0.5f, h * 0.9f,                          // chin
                    w * 0.6f, h * 0.9f,                          // right jaw
                    w * 0.7f, h * 0.8f                           // right cheek
                )
                cubicTo(
                    w * 0.85f, h * 0.65f,                        // right temple
                    w * 0.85f, h * 0.45f,                        // top curve start
                    w * 0.7f, h * 0.25f                          // top right head
                )
                close()
            }

            native.drawPath(path.asAndroidPath(), clearPaint)
            native.restoreToCount(layer)
        }
    }
}