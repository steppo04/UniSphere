package com.example.unisphere.ui.screen.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar

data class PointOfInterest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val notes: String
)

@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: MapViewModel = viewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    Scaffold(
        topBar = { AppBar(title = "UniMaps", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onAction(MapAction.OnAddPoiClicked) }) {
                Icon(Icons.Default.AddLocation, contentDescription = "Aggiungi Punto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                "I tuoi luoghi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Pulsante per aprire un'app esterna per cercare coordinate/luoghi
            Button(
                onClick = {
                    val gmmIntentUri = Uri.parse("geo:0,0?q=università")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    context.startActivity(mapIntent)
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Apri Mappa Esterna")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.pois) { poi ->
                    PoiCard(
                        poi = poi,
                        onClick = { viewModel.onAction(MapAction.OnPoiSelected(poi)) },
                        onDelete = { viewModel.onAction(MapAction.OnDeletePoiClicked(poi.id)) }
                    )
                }
            }
        }
    }

    // Dialogo per visualizzare i dettagli del POI
    if (state.selectedPoi != null) {
        PoiDetailsDialog(
            poi = state.selectedPoi,
            onDismiss = { viewModel.onAction(MapAction.OnPoiSelected(null)) },
            onOpenInMaps = {
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(state.selectedPoi.address)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }
        )
    }

    // Dialogo per aggiungere un nuovo POI
    if (state.showAddDialog) {
        AddPoiDialog(
            state = state,
            onAction = viewModel::onAction,
            onDismiss = { viewModel.onAction(MapAction.OnDismissAddDialog) },
            onConfirm = { viewModel.onAction(MapAction.OnSavePoiClicked) }
        )
    }
}

@Composable
fun PoiCard(poi: PointOfInterest, onClick: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(poi.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(poi.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun PoiDetailsDialog(poi: PointOfInterest, onDismiss: () -> Unit, onOpenInMaps: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(poi.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "Indirizzo", value = poi.address)
                if (poi.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Note:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(poi.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenInMaps) {
                Icon(Icons.Default.Directions, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Apri in Maps")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPoiDialog(
    state: MapState,
    onAction: (MapAction) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.addressSuggestions) {
        expanded = state.addressSuggestions.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Punto di Interesse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.newPoiName,
                    onValueChange = { onAction(MapAction.OnNameChanged(it)) },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.newPoiAddress,
                        onValueChange = { onAction(MapAction.OnAddressChanged(it, context)) },
                        label = { Text("Indirizzo / Via") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (state.isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { onAction(MapAction.OnUseCurrentLocation(context)) }) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Usa posizione attuale")
                                }
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        properties = PopupProperties(focusable = false)
                    ) {
                        state.addressSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    onAction(MapAction.OnSuggestionSelected(suggestion))
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.newPoiNotes,
                    onValueChange = { onAction(MapAction.OnNotesChanged(it)) },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = state.newPoiName.isNotBlank() && state.newPoiAddress.isNotBlank()
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
