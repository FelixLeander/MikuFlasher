package com.example.flasherverifyer

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissionWithRationale(permission: String, rationale: String) {
    Log.i("FML", "permission: $permission, rationale: $rationale")
    val permissionState = rememberPermissionState(permission)
    when (permissionState.status) {
        PermissionStatus.Granted -> {}
        is PermissionStatus.Denied -> {
            val showDialog = remember { mutableStateOf(true) }
            if (showDialog.value) {
                AlertDialog(
                    title = { Text("$rationale Permission Required") },
                    onDismissRequest = {
                        Log.i("FML", "onDismissRequest")
                        showDialog.value = false
                    },
                    confirmButton = {
                        Button(
                            {
                                Log.i("FML", "confirmButton")
                                permissionState.launchPermissionRequest()
                            },
                            content = { Text("Allow $rationale") })
                    },
                    dismissButton = {
                        Button({
                            Log.i("FML", "dismissButton")
                            showDialog.value = false
                        }, content = { Text("Forbid $rationale") })
                    }
                )
            }
        }
    }
}
