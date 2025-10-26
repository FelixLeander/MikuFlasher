package com.example.flasherverifyer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

//@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun SwipeBetweenScreens(modifier: Modifier = Modifier) {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        beyondViewportPageCount = 1 // precompose neighbors if wanted
    ) { page ->
        when (page) {
            0 -> BackgroundStencil(modifier, LocalContext.current,
                R.drawable.face_shape_fill, R.drawable.face_shape)

            1 -> BackgroundStencil(modifier, LocalContext.current
                , R.drawable.face_shape_miku_fill, R.drawable.face_shape_miku)

            2 -> BackgroundStencil(modifier, LocalContext.current,
                R.drawable.face_shape_miku_fill_full, R.drawable.face_shape_miku)

            3 -> BackgroundStencil(modifier, LocalContext.current,
                R.drawable.torso_shape_fill, R.drawable.torso_shape)
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
