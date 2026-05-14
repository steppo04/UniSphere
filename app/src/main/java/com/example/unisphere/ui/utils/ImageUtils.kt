package com.example.unisphere.ui.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun rememberImagePicker(
    onImageSelected: (Uri) -> Unit
): () -> Unit {
    val context = LocalContext.current

    var showDialog by rememberSaveable { mutableStateOf(false) }
    var tempUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val permanentUri = saveImageToInternalStorage(context, it)
            if (permanentUri != null) onImageSelected(Uri.parse(permanentUri))
        }
    }

    // contratto standard per la fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        android.util.Log.d("FOTOCAMERA", "Successo scatto: $success")
        if (success && tempUriString != null) {
            val uri = Uri.parse(tempUriString!!)
            // Copiamo l'immagine temporanea rendendola definitiva con il timestamp corretto
            val permanentUri = saveImageToInternalStorage(context, uri)
            android.util.Log.d("FOTOCAMERA", "Nuovo percorso: $permanentUri")
            if (permanentUri != null) onImageSelected(Uri.parse(permanentUri))
        }
    }

    val openDialog = { showDialog = true }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Seleziona Foto Profilo") },
            text = { Text("Vuoi scattare una nuova foto o sceglierne una dalla galleria?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Galleria")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    try {
                        // CREAZIONE FILE TEMPORANEO NELLA CACHE PRIVATA
                        val cacheDir = context.externalCacheDir ?: context.cacheDir
                        val file = File.createTempFile("tmp_profile_take_", ".jpg", cacheDir)

                        // Generiamo l'URI sicuro tramite il FileProvider dichiarato nel Manifest
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )

                        tempUriString = uri.toString()
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) {
                    Text("Fotocamera")
                }
            }
        )
    }

    return openDialog
}


private fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "user_profile_photo.jpg")

        val outputStream = FileOutputStream(file)
        inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }

        outputStream.flush()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}
