package com.example.flasherverifyer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale

@Preview
@Composable
fun BackgroundStencilPreview() {
    BackgroundStencil(
        Modifier.fillMaxSize(), LocalContext.current,
        R.drawable.torso_shape_fill, R.drawable.torso_shape, 1f, 1f
    )
}

@Composable
fun BackgroundStencil(
    modifier: Modifier = Modifier,
    context: Context,
    @DrawableRes stencil: Int,
    @DrawableRes draw: Int,
    scaleWidth: Float,
    scaleHeight: Float
) {
    Canvas(modifier) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val fullRectF = RectF(0f, 0f, size.width, size.height)
        val previous = nativeCanvas.saveLayer(fullRectF, null)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        paint.color = Color(255f, 255f, 255f, 0.6f).toArgb()
        nativeCanvas.drawRect(fullRectF, paint)



        nativeCanvas.drawDrawable(
            context,
            size,
            stencil,
            scaleWidth,
            scaleHeight,
            paint,
            PorterDuff.Mode.DST_OUT
        )

        nativeCanvas.drawDrawable(
            context,
            size,
            draw,
            scaleWidth,
            scaleHeight,
            paint,
            PorterDuff.Mode.DST_OVER
        )

//            var stencilBitmap = vectorToBitmap(context, stencil)
//            stencilBitmap = stencilBitmap.scale(stencilBitmap.width * scaleWidth, stencilBitmap.height * scaleHeight)
//            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
//            nativeCanvas.drawBitmapCenter(stencilBitmap, size.width / 2, size.height / 2, paint)
//
//            var displayBitmap = vectorToBitmap(context, draw)
//            displayBitmap = displayBitmap.scale(displayBitmap.width * scaleWidth, displayBitmap.height * scaleHeight)
//            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
//            nativeCanvas.drawBitmapCenter(displayBitmap, size.width / 2, size.height / 2)

        paint.xfermode = null
        nativeCanvas.restoreToCount(previous)
    }
}

fun NativeCanvas.drawDrawable(
    context: Context,
    parentSize: Size,
    @DrawableRes stencil: Int,
    scaleWidth: Float,
    scaleHeight: Float,
    paint: Paint,
    porterDuffMode: PorterDuff.Mode
) {
    var stencilBitmap = vectorToBitmap(context, stencil)

    val width = (stencilBitmap.width * scaleWidth).toInt()
    val height = (stencilBitmap.height * scaleHeight).toInt()
    stencilBitmap = stencilBitmap.scale(width, height)

    paint.xfermode = PorterDuffXfermode(porterDuffMode)
    this.drawBitmapCenter(stencilBitmap, parentSize.width / 2, parentSize.height / 2, paint)
}

fun vectorToBitmap(
    context: Context,
    @DrawableRes drawableId: Int
): Bitmap {
    val drawable =
        AppCompatResources.getDrawable(context, drawableId) ?: error("Drawable not found")

    if (drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
        error("whyever this drawable has no intrinsic size")
    }

    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    val bitmap = drawable.toBitmap()
    return bitmap
}

fun NativeCanvas.drawBitmapCenter(bitmap: Bitmap, left: Float, top: Float, paint: Paint) {
    val halfWidth = bitmap.width / 2
    val halfHeight = bitmap.height / 2

    val calLeft = left - halfWidth
    val calTop = top - halfHeight

    this.drawBitmap(bitmap, calLeft, calTop, paint)
}