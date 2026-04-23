package com.example.unisphere.ui.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun rememberCameraLauncher(
    onPictureTaken: (Uri) -> Unit = {}
): Pair<Uri?, () -> Unit> {
    var launcherUri by remember { mutableStateOf<Uri?>(null) }
    var pictureUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { pictureTaken ->
        if (pictureTaken) launcherUri?.let {
            pictureUri = it
            onPictureTaken(it)
        }
    }

    val ctx = LocalContext.current
    val takePicture = {
        val file = File.createTempFile("tmp_image", ".jpg", ctx.externalCacheDir)
        launcherUri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.provider", file)
        launcher.launch(launcherUri!!)
    }

    return pictureUri to takePicture
}