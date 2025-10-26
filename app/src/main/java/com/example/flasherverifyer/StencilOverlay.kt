package com.example.flasherverifyer

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import org.xmlpull.v1.XmlPullParser

@Preview
@Composable
fun MyPreview() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Blue)
    ) {

        val miku = AppCompatResources.getDrawable(LocalContext.current, R.drawable.face_shape_miku)
        if (miku == null)
            return

        PunchHole(Modifier.fillMaxSize(), LocalContext.current,false, drawable = miku)
    }
}

@Composable
fun PunchHole(modifier: Modifier = Modifier, context: Context, boolean: Boolean, drawable: Drawable) {
    Canvas(modifier) {
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val rectF = RectF(0f, 0f, size.width, size.height)

        val previous = nativeCanvas.saveLayer(rectF, null)
        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Draw a red rectangle as the background shape
        val wRect = 600f
        val hRect = 1200f
        paint.color = Color.Red.toArgb()
        nativeCanvas.drawRect(rectF, paint)

        // Set SRC_OVER xfermode explicitly (though it's the default)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)

        if (boolean) {

            // Draw a semi-transparent blue circle on top
            val xPosCircle = 600f
            val yPosCircle = 400f
            val radius = 200f
            paint.color = Color.Yellow.toArgb()
            nativeCanvas.drawCircle(xPosCircle, yPosCircle, radius, paint)
        } else {
            drawable.bounds = android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt())

            val path = drawableToPath(context.resources, R.drawable.face_shape_miku)
            nativeCanvas.drawPath(path, paint)
            Log.i("FELIX", "FUCK")
//            drawable.draw(nativeCanvas)

        }

        // clear xfermode
        paint.xfermode = null
        nativeCanvas.restoreToCount(previous)
    }
}

fun drawableToPath(resources: Resources, @DrawableRes drawable: Int): Path {
    val result = VectorDrawableParser.parsedVectorDrawable(resources, drawable)
    if (result == null)
        throw Error("Dev fucked up <3")

    val pathParser = PathParser().parsePathString(result.pathData)
    return pathParser.toPath().asAndroidPath()
}

data class ParsedVectorDrawable(
    val width: Float,
    val height: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val pathData: String
)

object VectorDrawableParser {
    private val digitsOnly = Regex("[^0-9.]")

    @SuppressLint("ResourceType")
    fun parsedVectorDrawable(
        resources: Resources,
        @DrawableRes drawable: Int
    ): ParsedVectorDrawable? {
        var pathData: String? = null
        var width: Float? = null
        var height: Float? = null
        var viewportWidth: Float? = null
        var viewportHeight: Float? = null

        // This is very simple parser, it doesn't support <group> tag, nested tags and other stuff
        resources.getXml(drawable).use { xml ->
            var event = xml.eventType
            while (event != XmlPullParser.END_DOCUMENT) {

                if (event != XmlPullParser.START_TAG) {
                    event = xml.next()
                    continue
                }

                when (xml.name) {
                    "vector" -> {
                        width = xml.getAttributeValue(getAttrPosition(xml, "width"))
                            .replace(digitsOnly, "")
                            .toFloatOrNull()
                        height = xml.getAttributeValue(getAttrPosition(xml, "height"))
                            .replace(digitsOnly, "")
                            .toFloatOrNull()
                        viewportWidth = xml.getAttributeValue(getAttrPosition(xml, "viewportWidth"))
                            .toFloatOrNull()
                        viewportHeight =
                            xml.getAttributeValue(getAttrPosition(xml, "viewportHeight"))
                                .toFloatOrNull()
                    }

                    "path" -> {
                        pathData = xml.getAttributeValue(getAttrPosition(xml, "pathData"))
                    }
                }

                event = xml.next()
            }
        }

        return ParsedVectorDrawable(
            width ?: return null,
            height ?: return null,
            viewportWidth ?: return null,
            viewportHeight ?: return null,
            pathData ?: return null
        )
    }

    private fun getAttrPosition(xml: XmlPullParser, attrName: String): Int =
        (0 until xml.attributeCount)
            .firstOrNull { i -> xml.getAttributeName(i) == attrName }
            ?: -1
}