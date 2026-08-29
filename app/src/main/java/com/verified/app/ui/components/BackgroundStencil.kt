package com.verified.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.compose.ui.graphics.Color

/**
 * Draws a frosted white overlay over the whole composable, then:
 *  1. DST_OUT cuts a transparent hole in the shape of [stencil]
 *  2. DST_OVER draws the [draw] outline sitting underneath everything
 *
 * The result: camera feed shows through the body-part cutout; the outline
 * sits on top of the camera at the edges of the shape.
 */
@Composable
fun BackgroundStencil(
    modifier: Modifier = Modifier,
    @DrawableRes stencil: Int,
    @DrawableRes draw: Int,
    drawTint: Color = Color.Unspecified,
) {
    val context = LocalContext.current

    Canvas(modifier) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val fullRectF = RectF(0f, 0f, size.width, size.height)
        val layer = nativeCanvas.saveLayer(fullRectF, null)

        val paint = Paint().apply { isAntiAlias = true }

        // Frosted white layer
        paint.color = Color(1f, 1f, 1f, 0.6f).toArgb()
        nativeCanvas.drawRect(fullRectF, paint)

        // Cut out the body part shape — no tint
        paint.colorFilter = null
        nativeCanvas.drawDrawable(context, size, stencil, paint, PorterDuff.Mode.DST_OUT)

        // Draw outline — apply tint if provided
        paint.colorFilter = if (drawTint != Color.Unspecified)
            PorterDuffColorFilter(drawTint.toArgb(), PorterDuff.Mode.SRC_IN)
        else null
        nativeCanvas.drawDrawable(context, size, draw, paint, PorterDuff.Mode.DST_OVER)

        paint.xfermode = null
        paint.colorFilter = null
        nativeCanvas.restoreToCount(layer)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun NativeCanvas.drawDrawable(
    context: Context,
    parentSize: Size,
    @DrawableRes drawableRes: Int,
    paint: Paint,
    mode: PorterDuff.Mode,
) {
    val bitmap = vectorToBitmap(context, drawableRes)
    // Scale to fill the full available width, preserving aspect ratio
    val scale = parentSize.width / bitmap.width.toFloat()
    val scaled = bitmap.scale(
        parentSize.width.toInt(),
        (bitmap.height * scale).toInt(),
    )
    paint.xfermode = PorterDuffXfermode(mode)
    drawBitmapCentered(scaled, parentSize.width / 2f, parentSize.height / 2f, paint)
}

private fun vectorToBitmap(context: Context, @DrawableRes id: Int): Bitmap {
    val drawable = AppCompatResources.getDrawable(context, id)
        ?: error("Drawable $id not found")
    require(drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
        "Drawable $id has no intrinsic size"
    }
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    return drawable.toBitmap()
}

private fun NativeCanvas.drawBitmapCentered(bitmap: Bitmap, cx: Float, cy: Float, paint: Paint) {
    drawBitmap(bitmap, cx - bitmap.width / 2f, cy - bitmap.height / 2f, paint)
}
