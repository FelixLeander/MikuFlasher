package com.verified.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.verified.app.viewmodel.ScanViewModel
import java.io.File

@Composable
fun ResultScreen(
    viewModel: ScanViewModel = viewModel(),
    onRedo: () -> Unit,
) {
    val context = LocalContext.current
    // Read once — bitmap doesn't change while this screen is visible
    val bitmap  = remember { viewModel.capturedBitmap }

    // Permission launcher for saving on API < 29
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) bitmap?.let { saveToGallery(context, it) }
        else Toast.makeText(context, "Storage permission required to save", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── Captured image ────────────────────────────────────────────────────
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Bottom action bar ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    )
                )
                .padding(top = 48.dp, bottom = 52.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActionPill(label = "save") {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        saveLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        bitmap?.let { saveToGallery(context, it) }
                    }
                }
                ActionPill(label = "share") {
                    bitmap?.let { shareBitmap(context, it) }
                }
                ActionPill(label = "redo", tonal = true) {
                    viewModel.reset()
                    onRedo()
                }
            }
        }
    }
}

// ── Action pill ───────────────────────────────────────────────────────────────

@Composable
private fun ActionPill(
    label: String,
    tonal: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (tonal) Color.White.copy(alpha = 0.10f)
             else       Color.White.copy(alpha = 0.22f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Text(
            text  = label,
            color = Color.White,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── Save ──────────────────────────────────────────────────────────────────────

private fun saveToGallery(context: Context, bitmap: Bitmap) {
    try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "verified_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw Exception("MediaStore insert returned null")

        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
    }
}

// ── Share ─────────────────────────────────────────────────────────────────────

private fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        val shareDir = File(context.cacheDir, "share").also { it.mkdirs() }
        val file = File(shareDir, "verified.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    } catch (e: Exception) {
        Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
    }
}
