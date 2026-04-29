package com.example.unisphere.ui.screen.map

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: MapViewModel = viewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    
    // Configurazione osmdroid (necessaria per il corretto funzionamento dei tile)
    Configuration.getInstance().userAgentValue = context.packageName

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
        ) {
            // Mappa OpenStreetMap (Metà superiore)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            // Focus iniziale su Cesena
                            val cesena = GeoPoint(44.1391, 12.2432)
                            controller.setCenter(cesena)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { mapView ->
                        // Rimuoviamo i vecchi marker e aggiungiamo quelli attuali
                        mapView.overlays.clear()
                        state.pois.forEach { poi ->
                            val marker = Marker(mapView)
                            marker.position = GeoPoint(poi.latitude, poi.longitude)
                            marker.title = poi.name
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.setOnMarkerClickListener { _, _ ->
                                viewModel.onAction(MapAction.OnPoiSelected(poi))
                                true
                            }
                            mapView.overlays.add(marker)
                        }
                        
                        // Se un POI è selezionato, centra la mappa su di esso
                        state.selectedPoi?.let { selected ->
                            mapView.controller.animateTo(GeoPoint(selected.latitude, selected.longitude))
                        }
                        
                        mapView.invalidate()
                    }
                )

                // Card sovrapposta per i dettagli (Micro-dettagli)
                androidx.compose.animation.AnimatedVisibility(
                    visible = state.selectedPoi != null,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                ) {
                    val currentPoi = state.selectedPoi
                    if (currentPoi != null) {
                        PoiSmallCard(
                            poi = currentPoi,
                            onClose = { viewModel.onAction(MapAction.OnPoiSelected(null)) },
                            onOpenInMaps = {
                                val uri = Uri.parse("geo:${currentPoi.latitude},${currentPoi.longitude}?q=${Uri.encode(currentPoi.address)}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            // Lista dei punti salvati (Metà inferiore)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "I tuoi luoghi salvati",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (state.pois.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nessun luogo salvato. Aggiungine uno!", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.pois) { poi ->
                            PoiListItem(
                                poi = poi,
                                onClick = {
                                    viewModel.onAction(MapAction.OnPoiSelected(poi))
                                },
                                onDelete = { viewModel.onAction(MapAction.OnDeletePoiClicked(poi.id)) }
                            )
                        }
                    }
                }
            }
        }
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
fun PoiListItem(poi: PointOfInterest, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(poi.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(poi.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun PoiSmallCard(poi: PointOfInterest, onClose: () -> Unit, onOpenInMaps: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(poi.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi")
                }
            }
            Text(poi.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenInMaps,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Indicazioni", fontSize = 14.sp)
            }
        }
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
                        onValueChange = { 
                            onAction(MapAction.OnAddressChanged(it))
                            expanded = it.length >= 3
                        },
                        label = { Text("Indirizzo / Via") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (state.isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { onAction(MapAction.OnUseCurrentLocation) }) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Usa posizione attuale")
                                }
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(),
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
