package com.example.unisphere.ui.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@Composable
fun rememberImagePicker(
    onImageSelected: (Uri) -> Unit
): () -> Unit {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // LA CHIAVE DEL SUCCESSO: rememberSaveable!
    // Salviamo il percorso (String) invece del file, così sopravvive al riavvio della memoria
    var cameraTempFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    // --- 1. GALLERIA ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { galleryUri ->
            try {
                val destinationFile = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(galleryUri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                onImageSelected(Uri.fromFile(destinationFile))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- 2. FOTOCAMERA UFFICIALE TRAMITE FILE PROVIDER ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraTempFilePath?.let { path ->
                try {
                    val tempFile = File(path)
                    if (tempFile.exists()) {
                        // Creiamo il file definitivo
                        val destinationFile = File(context.filesDir, "profile_${System.currentTimeMillis()}.jpg")

                        // Spostiamo i dati dal file temporaneo a quello permanente
                        tempFile.inputStream().use { input ->
                            FileOutputStream(destinationFile).use { output ->
                                input.copyTo(output)
                            }
                        }

                        // Eliminiamo il file temporaneo di cache per fare pulizia
                        tempFile.delete()

                        // Aggiorniamo la UI con il percorso permanente e stabile
                        onImageSelected(Uri.fromFile(destinationFile))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Seleziona Immagine") },
            text = { Text("Vuoi scattare una nuova foto profilo o sceglierne una esistente dalla galleria?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Galleria") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    try {
                        val tempFile = File(context.cacheDir, "temp_camera_snap.jpg")

                        // Salviamo la Stringa nel rememberSaveable PRIMA di aprire la fotocamera
                        cameraTempFilePath = tempFile.absolutePath

                        // Generiamo l'URI sicuro tramite il FileProvider
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            tempFile
                        )

                        // Lanciamo la fotocamera
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }) { Text("Fotocamera") }
            }
        )
    }

    return { showDialog = true }
}