package com.example.flasherverifyer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap

@Composable
fun BackgroundStencil(
    modifier: Modifier = Modifier,
    context: Context,
    @DrawableRes stencil: Int,
    @DrawableRes draw: Int
) {
    Canvas(modifier) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val fullRectF = RectF(0f, 0f, size.width, size.height)
        val previous = nativeCanvas.saveLayer(fullRectF, null)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        paint.color = Color(0f, 0f, 0f, 0.9f).toArgb()
        nativeCanvas.drawRect(fullRectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)

        val bitmapStencil = vectorToBitmap(
            context,
            stencil,
            size.width.toInt() / 2,
            size.height.toInt() / 4
        )
        if (bitmapStencil != null) {
            paint.color = Color.Red.toArgb()
            nativeCanvas.drawBitmap(bitmapStencil, size.width / 4, size.height / 3, paint)
        }

        val bitmapImage = vectorToBitmap(
            context,
            draw,
            size.width.toInt() / 2,
            size.height.toInt() / 4
        )
        if (bitmapImage != null) {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
            nativeCanvas.drawBitmap(bitmapImage, size.width / 4, size.height / 3, Paint())
        }

        paint.xfermode = null
        nativeCanvas.restoreToCount(previous)
    }
}

fun vectorToBitmap(
    context: Context,
    @DrawableRes drawableId: Int,
    width: Int,
    height: Int
): Bitmap? {
    val drawable =
        AppCompatResources.getDrawable(context, drawableId) ?: error("Drawable not found")

    if (width == 0 || height == 0) {
        return null
    }

    drawable.setBounds(0, 0, width, height)
    val bitmap = drawable.toBitmap(width, height)
    return bitmap
}