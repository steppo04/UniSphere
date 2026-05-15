package com.example.unisphere.ui.screen.calendar

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.db.local.entity.EventEntity
import com.example.unisphere.ui.composables.NavigationRoute
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    navController: NavHostController,
    viewModel: EventDetailViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val event = state.event
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Stato per mostrare/nascondere l'alert di cancellazione
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Convertiamo il colore Hex dello stato in un Color di Compose sicuro
    val calendarColor = remember(state.calendarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(state.calendarColorHex))
        } catch (_: Exception) {
            Color.Gray // Fallback se il colore è vuoto o errato
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MediumTopAppBar(
                title = { Text(event?.title ?: "Dettaglio", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    if (event != null) {
                        IconButton(onClick = {
                            navController.navigate(NavigationRoute.EditCalendarEventScreen(eventId = event.id))
                        }) {
                            Icon(Icons.Default.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Elimina Evento") },
                text = { Text("Sei sicuro di voler eliminare questo evento? L'azione è irreversibile.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteEvent { navController.popBackStack() }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Elimina")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (event != null) {
            val contentModifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())

            if (isLandscape) {
                Row(contentModifier) {
                    Column(Modifier.weight(1f).padding(16.dp)) {
                        EventMainInfo(event = event, calendarName = state.calendarName, calendarColor = calendarColor)
                    }
                    Box(Modifier.weight(1.2f).fillMaxHeight().padding(16.dp)) { MapCard(state.geoPoint) }
                }
            } else {
                Column(contentModifier.padding(16.dp)) {
                    EventMainInfo(event = event, calendarName = state.calendarName, calendarColor = calendarColor)

                    SectionLabel("POSIZIONE")
                    MapCard(state.geoPoint)

                    if (event.description.isNotBlank()) {
                        SectionLabel("NOTE")
                        InfoCard {
                            Text(
                                event.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun EventMainInfo(event: EventEntity, calendarName: String, calendarColor: Color) {
    InfoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = calendarColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        event.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ITALY)),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${event.startTime} - ${event.endTime}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(calendarColor))
                Spacer(Modifier.width(12.dp))
                Text("Calendario: ", color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text(
                    text = calendarName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (event.location.isNotBlank()) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(event.location, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun InfoCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun MapCard(geoPoint: GeoPoint?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (geoPoint != null) {
            OSMView(geoPoint)
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Mappa non disponibile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun OSMView(point: GeoPoint) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(point)

                if (isDark) {
                    val inverseMatrix = floatArrayOf(
                        -1.0f, 0f, 0f, 0f, 255f,
                        0f, -1.0f, 0f, 0f, 255f,
                        0f, 0f, -1.0f, 0f, 255f,
                        0f, 0f, 0f, 1.0f, 0f
                    )
                    getOverlayManager().getTilesOverlay().setColorFilter(android.graphics.ColorMatrixColorFilter(inverseMatrix))
                }

                val marker = org.osmdroid.views.overlay.Marker(this)
                marker.position = point
                marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                marker.title = "Luogo"
                overlays.add(marker)
            }
        },
        update = { view ->
            view.controller.animateTo(point)
        },
        modifier = Modifier.fillMaxSize()
    )
}