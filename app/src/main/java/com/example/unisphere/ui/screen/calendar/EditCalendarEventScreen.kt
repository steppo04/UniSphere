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
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.composables.UniSphereAlertDialog
import com.example.unisphere.ui.composables.UniSphereButton
import com.example.unisphere.ui.composables.UniSphereTextField
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

    var showCreateDialog by remember { mutableStateOf(false) }
    var calendarToDelete by remember { mutableStateOf<CalendarTypeEntity?>(null) }
    val selectedCalendar = state.calendarTypes.find { it.id == state.selectedCalendarId }
    val selectedCalendarName = selectedCalendar?.name ?: "Seleziona Calendario"

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.onAction(AddCalendarEventAction.OnGetCurrentLocation)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
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
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Titolo Evento
            UniSphereTextField(
                value = state.title,
                onValueChange = { viewModel.onAction(AddCalendarEventAction.OnTitleChanged(it)) },
                label = "Titolo",
                leadingIcon = Icons.Default.Title,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
            )

            // Selezione Calendario
            ExposedDropdownMenuBox(
                expanded = state.isTypeExpanded,
                onExpandedChange = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(it)) }
            ) {
                UniSphereTextField(
                    value = selectedCalendarName,
                    onValueChange = {},
                    label = "Calendario",
                    leadingIcon = Icons.Default.Category,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.isTypeExpanded) }
                )

                ExposedDropdownMenu(
                    expanded = state.isTypeExpanded,
                    onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(false)) }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Nuovo Calendario", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(false)); showCreateDialog = true }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    if (state.calendarTypes.isEmpty()) {
                        DropdownMenuItem(text = { Text("Nessun calendario, creane uno!", color = Color.Gray) }, onClick = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(false)) })
                    } else {
                        state.calendarTypes.forEach { cal ->
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(cal.color))))
                                            Spacer(Modifier.width(12.dp))
                                            Text(cal.name)
                                        }
                                        IconButton(onClick = { calendarToDelete = cal }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                onClick = { viewModel.onAction(AddCalendarEventAction.OnCalendarChanged(cal.id)) }
                            )
                        }
                    }
                }
            }

            // Selezione Data
            Box(modifier = Modifier.fillMaxWidth()) {
                UniSphereTextField(
                    value = state.selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    onValueChange = {},
                    label = "Data",
                    leadingIcon = Icons.Default.CalendarToday,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable { focusManager.clearFocus(); viewModel.onAction(AddCalendarEventAction.ToggleDatePicker(true)) })
            }

            // Selezione Intervallo Orario
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    UniSphereTextField(
                        value = state.selectedStartTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        onValueChange = {},
                        label = "Inizio",
                        leadingIcon = Icons.Default.AccessTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { focusManager.clearFocus(); viewModel.onAction(AddCalendarEventAction.ToggleStartTimePicker(true)) })
                }
                Box(modifier = Modifier.weight(1f)) {
                    UniSphereTextField(
                        value = state.selectedEndTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        onValueChange = {},
                        label = "Fine",
                        leadingIcon = Icons.Default.AccessTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(Modifier.matchParentSize().clickable { focusManager.clearFocus(); viewModel.onAction(AddCalendarEventAction.ToggleEndTimePicker(true)) })
                }
            }

            // Luogo con logica GPS
            ExposedDropdownMenuBox(
                expanded = state.isLocationExpanded && state.locationSuggestions.isNotEmpty(),
                onExpandedChange = { viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(it)) }
            ) {
                UniSphereTextField(
                    value = state.location,
                    onValueChange = { nuovoLuogo -> viewModel.onAction(AddCalendarEventAction.OnLocationChanged(nuovoLuogo)) },
                    label = "Luogo",
                    leadingIcon = Icons.Default.Place,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    trailingIcon = {
                        if (state.isLoadingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (hasLoc) viewModel.onAction(AddCalendarEventAction.OnGetCurrentLocation)
                                    else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                                }
                            ) { Icon(Icons.Default.MyLocation, "Usa posizione attuale", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                )

                if (state.locationSuggestions.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = state.isLocationExpanded, onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(false)) }) {
                        state.locationSuggestions.forEach { suggestion ->
                            DropdownMenuItem(text = { Text(suggestion) }, onClick = { viewModel.onAction(AddCalendarEventAction.OnLocationChanged(suggestion)); viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(false)); focusManager.clearFocus() })
                        }
                    }
                }
            }

            // Note Aggiuntive
            UniSphereTextField(
                value = state.description,
                onValueChange = { viewModel.onAction(AddCalendarEventAction.OnDescriptionChanged(it)) },
                label = "Note aggiuntive",
                leadingIcon = null,
                singleLine = false,
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bottone Aggiorna Evento
            UniSphereButton(
                text = "Aggiorna Evento",
                onClick = { focusManager.clearFocus(); viewModel.onAction(AddCalendarEventAction.OnSaveClicked) { navController.popBackStack() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.title.isNotBlank() && state.selectedCalendarId != 0
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Dialog: Creazione Nuovo Tipo Calendario
    if (showCreateDialog) {
        var newCalName by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#34C759") }
        val palette = listOf("#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#007AFF", "#5856D6", "#AF52DE", "#8E8E93")

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nuovo Tipo Calendario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    UniSphereTextField(value = newCalName, onValueChange = { newCalName = it }, label = "Nome Calendario", modifier = Modifier.fillMaxWidth())
                    Text("Scegli Colore", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        palette.forEach { hex ->
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))).clickable { selectedColorHex = hex }.border(if (selectedColorHex == hex) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape))
                        }
                    }
                }
            },
            confirmButton = {
                UniSphereButton(text = "Crea", onClick = { viewModel.onAction(AddCalendarEventAction.OnCreateCalendarType(newCalName, selectedColorHex)); showCreateDialog = false }, enabled = newCalName.isNotBlank())
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Annulla") } }
        )
    }

    // --- REFACTOR COMPLETATO: Sostituito AlertDialog nativo con UniSphereAlertDialog globale ---
    calendarToDelete?.let { cal ->
        UniSphereAlertDialog(
            title = "Elimina Calendario",
            text = "Sei sicuro di voler eliminare il calendario \"${cal.name}\"?",
            confirmText = "Elimina",
            onConfirm = {
                viewModel.onAction(AddCalendarEventAction.OnDeleteCalendarType(cal))
                calendarToDelete = null
            },
            onDismiss = { calendarToDelete = null },
            dismissText = "Annulla"
        )
    }

    // System Pickers Nativi
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
            confirmButton = { TextButton(onClick = { viewModel.onAction(AddCalendarEventAction.OnStartTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute))) }) { Text("OK") } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (state.showEndTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = state.selectedEndTime.hour, initialMinute = state.selectedEndTime.minute)
        AlertDialog(
            onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleEndTimePicker(false)) },
            confirmButton = { TextButton(onClick = { viewModel.onAction(AddCalendarEventAction.OnEndTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute))) }) { Text("OK") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}