package com.example.unisphere.ui.screen.calendar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarEvent(
    navController: NavHostController,
    viewModel: AddCalendarEventViewModel = viewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.onAction(AddCalendarEventAction.OnGetCurrentLocation)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aggiungi Evento", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- TITOLO ---
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.onAction(AddCalendarEventAction.OnTitleChanged(it)) },
                label = { Text("Titolo") },
                modifier = Modifier.fillMaxWidth()
            )

            // --- TIPO CALENDARIO ---
            ExposedDropdownMenuBox(
                expanded = state.isTypeExpanded,
                onExpandedChange = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(it)) }
            ) {
                OutlinedTextField(
                    value = state.selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Calendario") },
                    trailingIcon = { if (state.isLoadingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) }
                )
                ExposedDropdownMenu(
                    expanded = state.isTypeExpanded,
                    onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleTypeExpanded(false)) }
                ) {
                    state.calendarTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = { viewModel.onAction(AddCalendarEventAction.OnTypeChanged(type)) }
                        )
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
                Box(Modifier.matchParentSize().clickable { viewModel.onAction(AddCalendarEventAction.ToggleDatePicker(true)) })
            }

            // --- ORARIO ---
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Orario") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(Modifier.matchParentSize().clickable { viewModel.onAction(AddCalendarEventAction.ToggleTimePicker(true)) })
            }

            // --- LUOGO ---
            ExposedDropdownMenuBox(
                expanded = state.isLocationExpanded,
                onExpandedChange = { viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(it)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = state.location,
                    onValueChange = { viewModel.onAction(AddCalendarEventAction.OnLocationChanged(it)) },
                    label = { Text("Luogo") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = state.isLocationExpanded,
                    onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(false)) }
                ) {
                    // Posizione Attuale
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Posizione attuale", fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(false))
                            val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                            if (hasFineLocation || hasCoarseLocation) {
                                viewModel.onAction(AddCalendarEventAction.OnGetCurrentLocation)
                            } else {
                                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        }
                    )

                    if (state.isSearchingSuggestions) {
                        DropdownMenuItem(text = { CircularProgressIndicator(Modifier.size(20.dp)) }, onClick = {}, enabled = false)
                    }

                    state.locationSuggestions.forEach { suggestion ->
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(suggestion, fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                viewModel.onAction(AddCalendarEventAction.OnLocationChanged(suggestion))
                                viewModel.onAction(AddCalendarEventAction.ToggleLocationExpanded(false))
                            }
                        )
                    }
                }
            }

            // --- NOTE AGGIUNTIVE ---
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.onAction(AddCalendarEventAction.OnDescriptionChanged(it)) },
                label = { Text("Note aggiuntive") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- BOTTONE SALVA ---
            Button(
                onClick = { viewModel.onAction(AddCalendarEventAction.OnSaveClicked) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salva Evento", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- DIALOGS (DatePicker & TimePicker) ---
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

    if (state.showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = state.selectedTime.hour, initialMinute = state.selectedTime.minute)
        AlertDialog(
            onDismissRequest = { viewModel.onAction(AddCalendarEventAction.ToggleTimePicker(false)) },
            confirmButton = {
                TextButton(onClick = {
                    val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    viewModel.onAction(AddCalendarEventAction.OnTimeChanged(time))
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}