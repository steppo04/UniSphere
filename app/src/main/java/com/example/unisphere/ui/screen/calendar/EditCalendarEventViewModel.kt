package com.example.unisphere.ui.screen.calendar

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.db.local.entity.CalendarTypeEntity
import com.example.unisphere.db.local.entity.EventEntity
import com.example.unisphere.repository.EventRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EditCalendarEventViewModel @Inject constructor(
    application: Application,
    private val repository: EventRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    var state by mutableStateOf(AddCalendarEventState())
        private set

    private val eventId: Int = checkNotNull(savedStateHandle["eventId"])
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var searchJob: Job? = null

    init {
        loadUserCalendars()
        loadEventDetails()
    }

    private fun loadUserCalendars() {
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
            // Ascoltiamo i calendari dell'utente in tempo reale anche in modifica
            repository.getCalendarsForUser(uid).collectLatest { calendars ->
                state = state.copy(calendarTypes = calendars)
            }
        }
    }

    private fun loadEventDetails() {
        viewModelScope.launch {
            val event = repository.getEventById(eventId).firstOrNull()
            if (event != null) {
                state = state.copy(
                    title = event.title,
                    location = event.location,
                    description = event.description,
                    selectedDate = event.date,
                    selectedStartTime = event.startTime,
                    selectedEndTime = event.endTime,
                    selectedCalendarId = event.calendar
                )
            }
        }
    }

    fun onAction(action: AddCalendarEventAction, onBack: () -> Unit = {}) {
        when (action) {
            is AddCalendarEventAction.OnTitleChanged -> state = state.copy(title = action.value)
            is AddCalendarEventAction.OnLocationChanged -> {
                state = state.copy(location = action.value, isLocationExpanded = true)
                fetchLocationSuggestions(action.value)
            }
            is AddCalendarEventAction.OnDescriptionChanged -> state = state.copy(description = action.value)
            is AddCalendarEventAction.OnCalendarChanged -> state = state.copy(selectedCalendarId = action.value, isTypeExpanded = false)
            is AddCalendarEventAction.OnDateChanged -> state = state.copy(selectedDate = action.value, showDatePicker = false)

            is AddCalendarEventAction.OnStartTimeChanged -> state = state.copy(selectedStartTime = action.value, showStartTimePicker = false)
            is AddCalendarEventAction.ToggleStartTimePicker -> state = state.copy(showStartTimePicker = action.show)

            is AddCalendarEventAction.OnEndTimeChanged -> state = state.copy(selectedEndTime = action.value, showEndTimePicker = false)
            is AddCalendarEventAction.ToggleEndTimePicker -> state = state.copy(showEndTimePicker = action.show)

            is AddCalendarEventAction.ToggleTypeExpanded -> state = state.copy(isTypeExpanded = action.expanded)
            is AddCalendarEventAction.ToggleLocationExpanded -> state = state.copy(isLocationExpanded = action.expanded)
            is AddCalendarEventAction.ToggleDatePicker -> state = state.copy(showDatePicker = action.show)

            // Gestione dei metodi di aggiunta ed eliminazione dei calendari ereditati dall'interfaccia
            is AddCalendarEventAction.OnCreateCalendarType -> createCalendarType(action.name, action.colorHex)
            is AddCalendarEventAction.OnDeleteCalendarType -> deleteCalendarType(action.calendar)

            AddCalendarEventAction.OnGetCurrentLocation -> getCurrentLocation()
            AddCalendarEventAction.OnSaveClicked -> updateEvent(onBack)
        }
    }

    private fun createCalendarType(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
            val newCalendar = CalendarTypeEntity(
                name = name,
                color = colorHex,
                userId = uid
            )
            repository.saveCalendar(newCalendar)
        }
    }

    private fun deleteCalendarType(calendar: CalendarTypeEntity) {
        viewModelScope.launch {
            repository.deleteCalendar(calendar)
        }
    }

    private fun updateEvent(onSuccess: () -> Unit) {
        if (state.title.isBlank() || state.selectedCalendarId == 0) return

        viewModelScope.launch {
            val eventOld = repository.getEventById(eventId).firstOrNull()

            if (eventOld != null) {
                val updatedEvent = eventOld.copy(
                    title = state.title,
                    location = state.location,
                    description = state.description,
                    date = state.selectedDate,
                    startTime = state.selectedStartTime,
                    endTime = state.selectedEndTime,
                    calendar = state.selectedCalendarId
                )
                repository.saveEvent(updatedEvent)
                onSuccess()
            }
        }
    }

    // --- LOGICA GPS & GEOCODER ---
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        viewModelScope.launch {
            state = state.copy(isLoadingLocation = true)
            try {
                val result = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                result?.let { location ->
                    val addressName = getAddressFromLocation(location.latitude, location.longitude)
                    if (addressName != null) state = state.copy(location = addressName)
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { state = state.copy(isLoadingLocation = false) }
        }
    }

    private fun fetchLocationSuggestions(query: String) {
        if (query.length < 3) {
            state = state.copy(locationSuggestions = emptyList())
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            state = state.copy(isSearchingSuggestions = true)
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 5)
                }
                val suggestions = addresses?.mapNotNull { it.getAddressLine(0) } ?: emptyList()
                state = state.copy(locationSuggestions = suggestions)
            } catch (e: Exception) { e.printStackTrace() }
            finally { state = state.copy(isSearchingSuggestions = false) }
        }
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        return try {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
            }
        } catch (e: Exception) { null }
    }
}