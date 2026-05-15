package com.example.unisphere.ui.screen.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.db.local.entity.PointOfInterestEntity
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    Configuration.getInstance().userAgentValue = context.packageName

    // --- SEZIONE PERMESSI RUNTME ANDROID ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            // Se l'utente concede il permesso, avvia il recupero della posizione
            viewModel.onAction(MapAction.OnUseCurrentLocation)
        }
    }

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
                            val cesena = GeoPoint(44.1391, 12.2432)
                            controller.setCenter(cesena)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { mapView ->
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

                        state.selectedPoi?.let { selected ->
                            mapView.controller.animateTo(GeoPoint(selected.latitude, selected.longitude))
                        }

                        mapView.invalidate()
                    }
                )

                if (state.selectedPoi != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .fillMaxWidth()
                    ) {
                        PoiSmallCard(
                            poi = state.selectedPoi,
                            onClose = { viewModel.onAction(MapAction.OnPoiSelected(null)) },
                            onOpenInMaps = {
                                val intentUri = Uri.parse("geo:${state.selectedPoi.latitude},${state.selectedPoi.longitude}?q=${Uri.encode(state.selectedPoi.address)}")
                                val intent = Intent(Intent.ACTION_VIEW, intentUri)
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
                        items(state.pois, key = { it.id }) { poi ->
                            PoiListItem(
                                poi = poi,
                                onClick = { viewModel.onAction(MapAction.OnPoiSelected(poi)) },
                                onDelete = { viewModel.onAction(MapAction.OnDeletePoiClicked(poi)) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddPoiDialog(
            state = state,
            onAction = viewModel::onAction,
            onDismiss = { viewModel.onAction(MapAction.OnDismissAddDialog) },
            onConfirm = { viewModel.onAction(MapAction.OnSavePoiClicked) },
            onLocationRequest = {
                // Controllo preliminare se i permessi sono già stati accordati in passato
                val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarseLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (fineLocationGranted || coarseLocationGranted) {
                    viewModel.onAction(MapAction.OnUseCurrentLocation)
                } else {
                    // Altrimenti lancia il pop-up di richiesta permessi di sistema
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }
        )
    }
}

@Composable
fun PoiListItem(poi: PointOfInterestEntity, onClick: () -> Unit, onDelete: () -> Unit) {
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
fun PoiSmallCard(poi: PointOfInterestEntity, onClose: () -> Unit, onOpenInMaps: () -> Unit) {
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
    onConfirm: () -> Unit,
    onLocationRequest: () -> Unit // Callback delegata al check dei permessi
) {
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.addressSuggestions) {
        expanded = state.addressSuggestions.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Punto di Interesse", fontWeight = FontWeight.Bold) },
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
                                IconButton(onClick = onLocationRequest) { // <--- ORA CHIAMA IL VERIFICATORE DEI PERMESSI
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