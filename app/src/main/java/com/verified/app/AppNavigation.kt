package com.verified.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.verified.app.ui.screens.ChestScanScreen
import com.verified.app.ui.screens.FaceScanScreen
import com.verified.app.ui.screens.ResultScreen
import com.verified.app.viewmodel.ScanViewModel

object Routes {
    const val FACE_SCAN = "face_scan"
    const val CHEST_SCAN = "chest_scan"
    const val RESULT = "result"
}

private val fadeSpec = tween<Float>(durationMillis = 450)

@Composable
fun AppNavigation(navController: NavHostController) {
    val scanViewModel: ScanViewModel = viewModel()

    NavHost(
        navController    = navController,
        startDestination = Routes.FACE_SCAN,
        enterTransition  = { fadeIn(fadeSpec) },
        exitTransition   = { fadeOut(fadeSpec) },
    ) {
        composable(Routes.FACE_SCAN) {
            FaceScanScreen(
                viewModel = scanViewModel,
                onVerified = {
                    scanViewModel.advanceToChestStage()
                    navController.navigate(Routes.CHEST_SCAN) {
                        popUpTo(Routes.FACE_SCAN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CHEST_SCAN) {
            ChestScanScreen(
                viewModel = scanViewModel,
                onVerified = {
                    scanViewModel.advanceToComplete()
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.CHEST_SCAN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.RESULT) {
            ResultScreen(
                viewModel = scanViewModel,
                onRedo = {
                    navController.navigate(Routes.FACE_SCAN) {
                        popUpTo(Routes.RESULT) { inclusive = true }
                    }
                },
            )
        }
    }
}
