package com.example.flasherverifyer

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner


@Preview(showBackground = true)
@Composable
fun CameraFeed(
    modifier: Modifier = Modifier,
    controller: LifecycleCameraController? = null
) {
    if (LocalInspectionMode.current)
    {
        Box(modifier = modifier.fillMaxSize().background(Color.DarkGray)){
        }
        return
    }

        val lifecycleOwner = LocalLifecycleOwner.current
        AndroidView(
            factory = {
                PreviewView(it).apply {
                    this.controller = controller
                    controller?.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = modifier
        )
}
