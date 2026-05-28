package com.example.unisphere.ui.screen.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.unisphere.ui.composables.UniSphereAlertDialog
import com.example.unisphere.ui.composables.UniSphereButton
import com.example.unisphere.ui.composables.UniSphereEmptyState
import com.example.unisphere.ui.composables.UniSphereListItem
import com.example.unisphere.ui.composables.UniSphereSectionHeader
import com.example.unisphere.ui.composables.UniSphereTextField
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    navController: NavHostController,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    var poiToDelete by remember { mutableStateOf<PointOfInterestEntity?>(null) }

    Configuration.getInstance().userAgentValue = context.packageName

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.onAction(MapAction.OnUseCurrentLocation)
        }
    }

    Scaffold(
        topBar = { AppBar(title = "UniMaps", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(MapAction.OnAddPoiClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = "Aggiungi Punto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Contenitore Box isolato per la mappa OSM
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
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
                        updateMapMarkers(mapView, state.pois, state.selectedPoi) { poi ->
                            viewModel.onAction(MapAction.OnPoiSelected(poi))
                        }
                    }
                )

                if (state.selectedPoi != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .padding(horizontal = 16.dp)
            ) {
                UniSphereSectionHeader(title = "I tuoi luoghi salvati")

                if (state.pois.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        UniSphereEmptyState(
                            icon = Icons.Default.Map,
                            title = "Nessun luogo salvato",
                            description = "La tua mappa è un foglio bianco. Aggiungi i tuoi punti di interesse importanti (es. aule, mense o biblioteche) per trovarli subito!",
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(state.pois, key = { it.id }) { poi ->
                            PoiListItem(
                                poi = poi,
                                onClick = { viewModel.onAction(MapAction.OnPoiSelected(poi)) },
                                onDelete = { poiToDelete = poi }
                            )
                        }
                    }
                }
            }
        }
    }

    poiToDelete?.let { poi ->
        UniSphereAlertDialog(
            title = "Elimina Luogo",
            text = "Sei sicuro di voler eliminare \"${poi.name}\"? Questo rimuoverà il marker permanente dalla tua mappa.",
            confirmText = "Elimina",
            onConfirm = {
                viewModel.onAction(MapAction.OnDeletePoiClicked(poi))
                poiToDelete = null
            },
            onDismiss = { poiToDelete = null },
            dismissText = "Annulla"
        )
    }

    if (state.showAddDialog) {
        AddPoiDialog(
            state = state,
            onAction = viewModel::onAction,
            onDismiss = { viewModel.onAction(MapAction.OnDismissAddDialog) },
            onConfirm = { viewModel.onAction(MapAction.OnSavePoiClicked) },
            onLocationRequest = {
                val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarseLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (fineLocationGranted || coarseLocationGranted) {
                    viewModel.onAction(MapAction.OnUseCurrentLocation)
                } else {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }
        )
    }
}

// Elemento singolo della lista luoghi
@Composable
fun PoiListItem(poi: PointOfInterestEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        UniSphereListItem(
            headlineText = poi.name,
            supportingText = poi.address,
            leadingBarColor = MaterialTheme.colorScheme.primary,
            onClick = onClick,
            trailingContent = {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Elimina",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
}
private fun updateMapMarkers(
    mapView: MapView,
    pois: List<PointOfInterestEntity>,
    selectedPoi: PointOfInterestEntity?,
    onPoiSelected: (PointOfInterestEntity) -> Unit
) {
    mapView.overlays.clear()

    pois.forEach { poi ->
        val marker = Marker(mapView)
        marker.position = GeoPoint(poi.latitude, poi.longitude)
        marker.title = poi.name
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.setOnMarkerClickListener { _, _ ->
            onPoiSelected(poi)
            true
        }
        mapView.overlays.add(marker)
    }

    selectedPoi?.let { selected ->
        mapView.controller.animateTo(GeoPoint(selected.latitude, selected.longitude))
    }

    mapView.invalidate()
}
// Card fluttuante per visualizzare i dettagli rapidi del POI toccato
@Composable
fun PoiSmallCard(poi: PointOfInterestEntity, onClose: () -> Unit, onOpenInMaps: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(poi.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(poi.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Chiudi", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenInMaps,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Indicazioni", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// Dialog di immissione dati per un nuovo POI
@Composable
fun AddPoiDialog(
    state: MapState,
    onAction: (MapAction) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onLocationRequest: () -> Unit
) {
    val expanded = remember(state.addressSuggestions) { state.addressSuggestions.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuovo Luogo", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {

                UniSphereTextField(
                    value = state.newPoiName,
                    onValueChange = { onAction(MapAction.OnNameChanged(it)) },
                    label = "Nome del luogo (es. Università)",
                    leadingIcon = Icons.Outlined.Badge,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    UniSphereTextField(
                        value = state.newPoiAddress,
                        onValueChange = { onAction(MapAction.OnAddressChanged(it)) },
                        label = "Indirizzo / Via",
                        leadingIcon = Icons.Outlined.Place,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (state.isLocating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                IconButton(onClick = onLocationRequest) {
                                    Icon(Icons.Default.MyLocation, contentDescription = "Usa posizione attuale", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onAction(MapAction.OnAddressChanged(state.newPoiAddress)) },
                        modifier = Modifier.fillMaxWidth(0.9f),
                        properties = PopupProperties(focusable = false)
                    ) {
                        state.addressSuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion, fontSize = 14.sp) },
                                onClick = { onAction(MapAction.OnSuggestionSelected(suggestion)) }
                            )
                        }
                    }
                }

                UniSphereTextField(
                    value = state.newPoiNotes,
                    onValueChange = { onAction(MapAction.OnNotesChanged(it)) },
                    label = "Note aggiuntive (opzionale)",
                    leadingIcon = null,
                    singleLine = false,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            UniSphereButton(
                text = "Salva",
                onClick = onConfirm,
                enabled = state.newPoiName.isNotBlank() && state.newPoiAddress.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) { Text("Annulla", color = MaterialTheme.colorScheme.outline) }
        }
    )
}