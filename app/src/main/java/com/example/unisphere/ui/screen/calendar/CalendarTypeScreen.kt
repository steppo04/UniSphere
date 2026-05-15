package com.example.unisphere.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.db.local.entity.CalendarTypeEntity // Assicurati che il path sia corretto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsScreen(
    navController: NavHostController,
    viewModel: CalendarSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var selectedCalendar by remember { mutableStateOf<CalendarTypeEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I miei Calendari", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedCalendar = null
                showSheet = true
            }) {
                Icon(Icons.Default.Add, "Aggiungi")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.calendars) { cal ->
                CalendarItemRow(
                    calendar = cal,
                    onEdit = {
                        selectedCalendar = cal
                        showSheet = true
                    },
                    onDelete = { viewModel.deleteCalendar(cal) }
                )
            }
        }

        if (showSheet) {
            CalendarEditSheet(
                calendar = selectedCalendar,
                onDismiss = { showSheet = false },
                onSave = { name, color ->
                    viewModel.saveCalendar(name, color, selectedCalendar?.id ?: 0)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
fun CalendarItemRow(calendar: CalendarTypeEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(calendar.color)))
            )
            Spacer(Modifier.width(16.dp))
            Text(calendar.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// --- IL COMPONENTE CHE HAI CHIESTO DI COLLOCARE ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEditSheet(
    calendar: CalendarTypeEntity?, // Nome aggiornato
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(calendar?.name ?: "") }
    var selectedColor by remember { mutableStateOf(calendar?.color ?: "#34C759") }

    val palette = listOf("#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#007AFF", "#5856D6", "#AF52DE", "#8E8E93")

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                text = if (calendar == null) "Nuovo Calendario" else "Modifica Calendario",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome Calendario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Scegli Colore", style = MaterialTheme.typography.labelLarge)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                palette.forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(hex)))
                            .clickable { selectedColor = hex }
                            .border(
                                width = if (selectedColor == hex) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary, // Colore bordo più visibile
                                shape = CircleShape
                            )
                    )
                }
            }

            Button(
                onClick = { onSave(name, selectedColor) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salva", fontWeight = FontWeight.Bold)
            }
        }
    }
}