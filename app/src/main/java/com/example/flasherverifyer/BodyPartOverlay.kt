package com.example.flasherverifyer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun SwipeBetweenScreensPreview() {
    SwipeBetweenScreens(Modifier.fillMaxSize())
}

@Composable
fun SwipeBetweenScreens(modifier: Modifier = Modifier) {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondViewportPageCount = 3 // precompose neighbors if wanted
    ) { page ->
        when (page) {
            0 -> {
                Box(modifier) {
                    BackgroundStencil(
                        modifier = modifier,
                        context = LocalContext.current,
                        stencil = R.drawable.torso_shape_fill,
                        draw = R.drawable.torso_shape,
                        scaleWidth = 1.7f,
                        scaleHeight = 1.7f
                    )
                    Image(painterResource(R.drawable.extremities), "", modifier, alpha = 1f)
                }
            }

            1 ->
                BackgroundStencil(
                    modifier = modifier,
                    context = LocalContext.current,
                    stencil = R.drawable.face_shape_miku_fill,
                    draw = R.drawable.face_shape_miku,
                    scaleWidth = 0.38f,
                    scaleHeight = 0.38f
                )

            2 -> BackgroundStencil(
                modifier = modifier,
                context = LocalContext.current,
                stencil = R.drawable.face_shape_miku_fill_full,
                draw = R.drawable.face_shape_miku,
                1f,
                1f
            )

            3 -> BackgroundStencil(
                modifier = modifier,
                context = LocalContext.current,
                stencil = R.drawable.face_shape_fill,
                draw = R.drawable.face_shape,
                1f,
                1f
            )
        }
    }
}
