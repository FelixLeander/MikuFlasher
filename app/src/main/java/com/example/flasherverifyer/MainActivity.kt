package com.example.flasherverifyer
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.flasherverifyer.ui.theme.FlasherVerifyerTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasRequiredPermissions())
            requestPermissions(CAMERAX_PERMISSIONS, 0)

        enableEdgeToEdge()
        setContent {
            FlasherVerifyerTheme {
                MyPreview()
//                AppPreview(context = applicationContext)
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun AppPreview(modifier: Modifier = Modifier, context: Context = LocalContext.current) {
        val controller =
            if (LocalInspectionMode.current) {
                null
            } else {
                remember {
                    LifecycleCameraController(context).apply {
                        setEnabledUseCases(
                            CameraController.IMAGE_CAPTURE
                                    and CameraController.VIDEO_CAPTURE
                                    and CameraController.IMAGE_ANALYSIS
                        )
                    }
                }
            }

        Box(modifier = modifier.fillMaxSize()) {
            CameraFeed(
                modifier = modifier.fillMaxSize(),
                controller = controller
            )

//            MikuFaceOverlay(Modifier.background(Color.Red))
//            SwipeToSwitchDrawables(
//                listOf(R.drawable.face_shape, R.drawable.extremities_shape, R.drawable.face_shape_miku),
//                Modifier.fillMaxSize())

            SwipeBetweenScreens(Modifier)

            CameraSwitchButton(
                modifier = modifier,
                controller = controller
            )

            Column(
                modifier = modifier.align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BottomStatus(modifier = modifier)

                IconButton(modifier = Modifier
                    .fillMaxHeight(0.1f)
                    .fillMaxWidth(),onClick = {

                }) {
                    Icon(Icons.Outlined.Camera,
                        "Take a picture",
                        Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun CameraSwitchButton(
    modifier: Modifier = Modifier,
    controller: LifecycleCameraController? = null
) {
    IconButton(
        onClick = {
            controller?.cameraSelector =
                if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                    CameraSelector.DEFAULT_FRONT_CAMERA
                else
                    CameraSelector.DEFAULT_BACK_CAMERA
        },
        modifier = modifier
            .offset(32.dp, 32.dp)
            .scale(2.5f)
    ) {
        Icon(
            imageVector = Icons.Outlined.Cameraswitch,
            contentDescription = "Switch camera"
        )
    }
}

@Preview
@Composable
fun BottomStatus(
    modifier: Modifier = Modifier
) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Verified",
                modifier = Modifier,
                color = Color.White,
                fontSize = 40.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = "CheckCircle",
                modifier = Modifier.scale(2f),
                tint = Color.Green
            )
        }

        Text(
            text = "Thank you for your compliance!",
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            fontSize = 25.sp,
            textAlign = TextAlign.Center
        )
}