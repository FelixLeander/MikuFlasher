package com.example.flasherverifyer

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.createBitmap

//@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun SwipeBetweenScreens(modifier: Modifier = Modifier) {
    val pageCount = 5
    val pagerState = rememberPagerState(pageCount = { pageCount })
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondViewportPageCount = 1 // precompose neighbors if wanted
    ) { page ->
        when (page) {
//            0 -> Image(
//                painterResource(id = R.drawable.face_shape),
//                "Face shape",
//                Modifier.fillMaxSize()
//            )

            0 -> HoleFromImageVector(
                ImageVector.vectorResource(R.drawable.face_shape),
                overlayColor = Color.Black,
                modifier = Modifier.fillMaxSize()
                )
            1 -> TorsoOverlay(Modifier.fillMaxSize(), Color.Yellow)
            2 -> TorsoOverlay(Modifier.fillMaxSize(), Color.Green)
            3 -> TorsoOverlay(Modifier.fillMaxSize(), Color.Red)
            4 -> MikuFaceOverlay(Modifier.fillMaxSize())
            5 -> Box(Modifier.fillMaxSize())
        }
    }
}

@Preview
@Composable
fun TorsoOverlay(modifier: Modifier = Modifier, color: Color = Color.Magenta) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.extremities),
            contentDescription = "Decorative should head and arms.",
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(id = R.drawable.torso_shape),
            contentDescription = "Outline of the torso.",
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(color)
        )
    }
}

@Preview
@Composable
fun MikuFaceOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = modifier) {
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
            modifier = Modifier
                .fillMaxSize()
                .scale(0.8f)
        )
    }
}

@Preview
@Composable
fun InverseShapeBackground(modifier: Modifier = Modifier) {
    Box(Modifier
        .fillMaxSize()
        .background(Color.Green)) {
        Image(painterResource(id = R.drawable.face_shape), "Face shape", Modifier.fillMaxSize())
    }
}

@Preview
@Composable
fun HoleFromImageVectorPreview() {
        val vector = ImageVector.vectorResource(R.drawable.face_shape)
        HoleFromImageVector(
            vector,
            overlayColor = Color.Red,
            modifier = Modifier
        )
}

@Composable
fun HoleFromImageVector(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    overlayColor: Color = Color(0x99000000),
    outlineStrokeWidth: Float = 4f,
    outlineColor: Color = Color.Blue
) {
    // prepare PathParser once
    val parser = remember { PathParser() }

    Canvas(modifier = modifier) {
        // full dark overlay
        drawRect(overlayColor, size = size)

        // recursive drawer: apply group transforms and draw its children
        fun drawGroup(group: VectorGroup) {
            // Apply group's transforms in the same order as ImageVector rendering:
            // translate -> rotate (around pivot) -> scale (around pivot)
            withTransform({
                // translate
                translate(left = group.translationX, top = group.translationY)
                // rotate around pivot
                if (group.rotation != 0f) {
                    rotate(degrees = group.rotation, pivot = Offset(group.pivotX, group.pivotY))
                }
                // scale around pivot
                if (group.scaleX != 1f || group.scaleY != 1f) {
                    scale(scaleX = group.scaleX, scaleY = group.scaleY, pivot = Offset(group.pivotX, group.pivotY))
                }
            }) {
                // iterate children (VectorNode is either VectorGroup or VectorPath)
                for (i in 0 until group.size) {
                    val node = group[i]
                    when (node) {
                        is VectorGroup -> drawGroup(node)
                        is VectorPath -> {
                            // convert PathNode list to a Compose Path
                            parser.clear()
                            parser.addPathNodes(node.pathData)
                            val path = Path().also { parser.toPath(it) }

                            // treat the path as filled and punch hole with Clear
                            drawPath(path = path, color = Color.Transparent, style = Fill, blendMode = BlendMode.Clear)

                            // optionally draw outline/stroke on top
                            if (node.stroke != null || outlineStrokeWidth > 0f) {
                                drawPath(path = path, color = outlineColor, style = Stroke(width = outlineStrokeWidth))
                            }
                        }
                    }
                }
            }
        }

        // start recursion at root group
        drawGroup(imageVector.root)
    }
}

