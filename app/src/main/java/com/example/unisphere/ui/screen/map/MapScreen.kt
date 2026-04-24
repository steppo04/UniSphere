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
import androidx.navigation.NavHostController
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar

data class PointOfInterest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val coordinates: String,
    val notes: String
)

@Composable
fun MapScreen(navController: NavHostController) {
    val context = LocalContext.current
    var pois by remember {
        mutableStateOf(
            mutableListOf(
                PointOfInterest(
                    name = "Biblioteca Centrale",
                    address = "Via dell'Università, 1",
                    coordinates = "41.8919, 12.5113",
                    notes = "Ottimo posto per studiare in silenzio."
                ),
                PointOfInterest(
                    name = "Mensa Universitaria",
                    address = "Piazza Studenti, 5",
                    coordinates = "41.8930, 12.5150",
                    notes = "Pranzo economico, chiude alle 15:00."
                )
            )
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPoi by remember { mutableStateOf<PointOfInterest?>(null) }

    Scaffold(
        topBar = { AppBar(title = "UniMaps", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
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

            // Pulsante per aprire un'app esterna (es. Google Maps per cercare coordinate)
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
                Text("Apri Mappa Esterna per cercare")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(pois) { poi ->
                    PoiCard(
                        poi = poi,
                        onClick = { selectedPoi = poi },
                        onDelete = { pois = pois.filter { it.id != poi.id }.toMutableList() }
                    )
                }
            }
        }
    }

    // Dialogo per visualizzare i dettagli del POI
    if (selectedPoi != null) {
        PoiDetailsDialog(
            poi = selectedPoi!!,
            onDismiss = { selectedPoi = null },
            onOpenInMaps = {
                val uri = Uri.parse("geo:0,0?q=${selectedPoi!!.coordinates}(${selectedPoi!!.name})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }
        )
    }

    // Dialogo per aggiungere un nuovo POI
    if (showAddDialog) {
        AddPoiDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, address, coords, notes ->
                pois = (pois + PointOfInterest(
                    name = name,
                    address = address,
                    coordinates = coords,
                    notes = notes
                )).toMutableList()
                showAddDialog = false
            }
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
                DetailRow(label = "Via", value = poi.address)
                DetailRow(label = "Coordinate", value = poi.coordinates)
                if (poi.notes.isNotBlank()) {
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

@Composable
fun AddPoiDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var coords by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Punto di Interesse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Indirizzo") })
                OutlinedTextField(value = coords, onValueChange = { coords = it }, label = { Text("Coordinate (lat, lng)") })
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Note") })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, address, coords, notes) }) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
