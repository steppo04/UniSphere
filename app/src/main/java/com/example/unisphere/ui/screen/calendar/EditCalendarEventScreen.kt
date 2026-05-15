package com.example.unisphere.ui.screen.calendar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.db.local.entity.CalendarTypeEntity
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCalendarEventScreen(
    navController: NavHostController,
    viewModel: EditCalendarEventViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Stati locali per la gestione dei dialog di aggiunta ed eliminazione dei tipi di calendario
    var showCreateDialog by remember { mutableStateOf(false) }
    var calendarToDelete by remember { mutableStateOf<CalendarTypeEntity?>(null) }

    // Cerchiamo l'oggetto calendario selezionato per mostrare nome e colore nella UI
    val selectedCalendar = state.calendarTypes.find { it.id == state.selectedCalendarId }
    val selectedCalendarName = selectedCalendar?.name ?: "Seleziona Calendario"

    // Permessi per la localizzazione
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            viewModel.onAction(AddCalendarEventAction.OnGetCurrentLocation)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(), // Spinge i bottoni sopra la tastiera
        topBar = {
            TopAppBar(
                title = { Text("Modifica Evento", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                // Chiude la tastiera se clicchi in un punto vuoto della colonna
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- TITOLO ---
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onAction(AddCalendarEventAction.OnTitleChanged(it)) },
                label = { Text("Titolo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            // --- SELEZIONE CALENDARIO DINAMICO ---
            ExposedDropdownMenuBox(
                expanded = state.isTypeExpanded,
                onExpandedChange = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(it)) }
            ) {
                OutlinedTextField(
                    value = selectedCalendarName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Calendario") },
                    leadingIcon = {
                        selectedCalendar?.let { cal ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(cal.color)))
                            )
                        } ?: Icon(Icons.Default.Category, contentDescription = null)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isTypeExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = state.isTypeExpanded,
                    onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(false)) }
                ) {
                    // Opzione fissa in cima per creare un nuovo calendario al volo
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Nuovo Calendario", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(false))
                            showCreateDialog = true
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    if (state.calendarTypes.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Nessun calendario, creane uno!", color = Color.Gray) },
                            onClick = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(false)) }
                        )
                    } else {
                        state.calendarTypes.forEach { cal ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(cal.color)))
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Text(cal.name)
                                        }
                                        // Tasto elimina tipo di calendario
                                        IconButton(
                                            onClick = {
                                                calendarToDelete = cal
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Elimina Calendario",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.onAction(AddCalendarEventAction.OnCalendarChanged(cal.id))
                                }
                            )
                        }
                    }
                }
            }

            // --- DATA ---
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Data") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable {
                    focusManager.clearFocus()
                    viewModel.onAction(AddCalendarEventAction.ToggleDatePicker(true))
                })
            }

            // --- ORARI ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.selectedStartTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Inizio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable {
                        focusManager.clearFocus()
                        viewModel.onAction(AddCalendarEventAction.ToggleStartTimePicker(true))
                    })
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.selectedEndTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fine") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable {
                        focusManager.clearFocus()
                        viewModel.onAction(AddCalendarEventAction.ToggleEndTimePicker(true))
                    })
                }
            }

            // --- LUOGO ---
            ExposedDropdownMenuBox(
                expanded = state.isLocationExpanded,
                onExpandedChange = { viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(it)) }
            ) {
                OutlinedTextField(
                    value = state.location,
                    onValueChange = { viewModel.onAction(AddCalendarEventAction.OnLocationChanged(it)) },
                    label = { Text("Luogo") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                    trailingIcon = {
                        if (state.isLoadingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                ExposedDropdownMenu(
                    expanded = state.isLocationExpanded,
                    onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(false)) }
                ) {
                    DropdownMenuItem(
                        text = { Text("Posizione attuale", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(false))
                            val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (hasLoc) viewModel.onAction(AddCalendarEventAction.OnGetCurrentLocation)
                            else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                    )
                    state.locationSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                viewModel.onAction(AddCalendarEventAction.OnLocationChanged(suggestion))
                                viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(false))
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }

            // --- NOTE ---
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onAction(AddCalendarEventAction.OnDescriptionChanged(it)) },
                label = { Text("Note aggiuntive") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- BOTTONE AGGIORNA ---
            Button(
                onClick = {
                    focusManager.clearFocus() // Chiude tastiera prima di salvare
                    viewModel.onAction(AddCalendarEventAction.OnSaveClicked) {
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = state.title.isNotBlank() && state.selectedCalendarId != 0
            ) {
                Text("Aggiorna Evento", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- DIALOGS DI CREAZIONE ED ELIMINAZIONE CALENDARI ---

    if (showCreateDialog) {
        var newCalName by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#34C759") }
        val palette = listOf("#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#007AFF", "#5856D6", "#AF52DE", "#8E8E93")

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nuovo Tipo Calendario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newCalName,
                        onValueChange = { newCalName = it },
                        label = { Text("Nome Calendario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Scegli Colore", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        palette.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { selectedColorHex = hex }
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onAction(AddCalendarEventAction.OnCreateCalendarType(newCalName, selectedColorHex))
                        showCreateDialog = false
                    },
                    enabled = newCalName.isNotBlank()
                ) { Text("Crea") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Annulla") }
            }
        )
    }

    calendarToDelete?.let { cal ->
        AlertDialog(
            onDismissRequest = { calendarToDelete = null },
            title = { Text("Elimina Calendario") },
            text = { Text("Sei sicuro di voler eliminare il calendario \"${cal.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onAction(AddCalendarEventAction.OnDeleteCalendarType(cal))
                        calendarToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { calendarToDelete = null }) { Text("Annulla") }
            }
        )
    }

    // --- DIALOGS (DatePicker e TimePickers) ---
    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleDatePicker(false)) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.onAction(AddCalendarEventAction.OnDateChanged(date))
                    }
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (state.showStartTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = state.selectedStartTime.hour, initialMinute = state.selectedStartTime.minute)
        AlertDialog(
            onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleStartTimePicker(false)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(AddCalendarEventAction.OnStartTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute)))
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (state.showEndTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = state.selectedEndTime.hour, initialMinute = state.selectedEndTime.minute)
        AlertDialog(
            onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleEndTimePicker(false)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAction(AddCalendarEventAction.OnEndTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute)))
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}