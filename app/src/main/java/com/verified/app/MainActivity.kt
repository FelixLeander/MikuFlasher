package com.verified.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.navigation.compose.rememberNavController
import com.verified.app.ui.screens.PermissionDeniedScreen
import com.verified.app.ui.screens.PermissionScreen
import com.verified.app.ui.theme.VerifiedAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VerifiedAppTheme {
                VerifiedRoot()
            }
        }
    }
}

@Composable
private fun VerifiedRoot() {
    val context = LocalContext.current

    fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PermissionChecker.PERMISSION_GRANTED

    var permissionGranted by remember { mutableStateOf(hasCameraPermission()) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionGranted = true
        } else {
            permissionDeniedPermanently = true
        }
    }

    when {
        permissionGranted -> {
            val navController = rememberNavController()
            AppNavigation(navController = navController)
        }

        permissionDeniedPermanently -> {
            PermissionDeniedScreen(
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }

        else -> {
            PermissionScreen(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }
    }
}
