package com.example.unisphere.ui.screen.calendar

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

data class AddCalendarEventState(
    val title: String = "",
    val location: String = "",
    val description: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedStartTime: LocalTime = LocalTime.of(9, 0),
    val selectedEndTime: LocalTime = LocalTime.of(10, 0),
    val isButtonEnabled: Boolean = false,
    val isTimeError: Boolean = false,
    val timeErrorMessage: String? = null,
    val selectedCalendarId: Int = 0,
    val calendarTypes: List<CalendarTypeEntity> = emptyList(),
    val isTypeExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val showStartTimePicker: Boolean = false,
    val showEndTimePicker: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val isLocationExpanded: Boolean = false,
    val locationSuggestions: List<String> = emptyList(),
    val isSearchingSuggestions: Boolean = false
)

sealed interface AddCalendarEventAction {
    data class OnTitleChanged(val value: String) : AddCalendarEventAction
    data class OnLocationChanged(val value: String) : AddCalendarEventAction
    data class OnDescriptionChanged(val value: String) : AddCalendarEventAction
    data class OnCalendarChanged(val value: Int) : AddCalendarEventAction
    data class OnDateChanged(val value: LocalDate) : AddCalendarEventAction
    data class OnStartTimeChanged(val value: LocalTime) : AddCalendarEventAction
    data class OnEndTimeChanged(val value: LocalTime) : AddCalendarEventAction
    data class ToggleTypeExpanded(val expanded: Boolean) : AddCalendarEventAction
    data class ToggleDatePicker(val show: Boolean) : AddCalendarEventAction
    data class ToggleStartTimePicker(val show: Boolean) : AddCalendarEventAction
    data class ToggleEndTimePicker(val show: Boolean) : AddCalendarEventAction
    data class ToggleLocationExpanded(val expanded: Boolean) : AddCalendarEventAction
    data class OnCreateCalendarType(val name: String, val colorHex: String) : AddCalendarEventAction
    data class OnDeleteCalendarType(val calendar: CalendarTypeEntity) : AddCalendarEventAction
    data object OnGetCurrentLocation : AddCalendarEventAction
    data object OnSaveClicked : AddCalendarEventAction
}

@HiltViewModel
class AddCalendarEventViewModel @Inject constructor(
    application: Application,
    private val eventRepository: EventRepository
) : AndroidViewModel(application) {

    var state by mutableStateOf(AddCalendarEventState())
        private set

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private var searchJob: Job? = null

    init {
        loadUserCalendars()
    }

    // Carica le categorie di calendario associate all'account
    private fun loadUserCalendars() {
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
            eventRepository.getCalendarsForUser(uid).collectLatest { calendars ->
                state = state.copy(
                    calendarTypes = calendars,
                    selectedCalendarId = if (state.selectedCalendarId == 0 || calendars.none { it.id == state.selectedCalendarId }) {
                        calendars.firstOrNull()?.id ?: 0
                    } else {
                        state.selectedCalendarId
                    }
                )
                validateFormState()
            }
        }
    }

    fun onAction(action: AddCalendarEventAction, onBack: () -> Unit = {}) {
        when (action) {
            is AddCalendarEventAction.OnTitleChanged -> {
                state = state.copy(title = action.value)
                validateFormState()
            }
            is AddCalendarEventAction.OnLocationChanged -> {
                state = state.copy(location = action.value, isLocationExpanded = true)
                fetchLocationSuggestions(action.value)
                validateFormState()
            }
            is AddCalendarEventAction.OnDescriptionChanged -> state = state.copy(description = action.value)
            is AddCalendarEventAction.OnCalendarChanged -> {
                state = state.copy(selectedCalendarId = action.value, isTypeExpanded = false)
                validateFormState()
            }
            is AddCalendarEventAction.OnDateChanged -> {
                state = state.copy(selectedDate = action.value, showDatePicker = false)
                validateFormState()
            }
            is AddCalendarEventAction.OnStartTimeChanged -> {
                state = state.copy(selectedStartTime = action.value, showStartTimePicker = false)
                validateFormState()
            }
            is AddCalendarEventAction.ToggleStartTimePicker -> state = state.copy(showStartTimePicker = action.show)
            is AddCalendarEventAction.OnEndTimeChanged -> {
                state = state.copy(selectedEndTime = action.value, showEndTimePicker = false)
                validateFormState()
            }
            is AddCalendarEventAction.ToggleEndTimePicker -> state = state.copy(showEndTimePicker = action.show)
            is AddCalendarEventAction.ToggleTypeExpanded -> state = state.copy(isTypeExpanded = action.expanded)
            is AddCalendarEventAction.ToggleLocationExpanded -> state = state.copy(isLocationExpanded = action.expanded)
            is AddCalendarEventAction.ToggleDatePicker -> state = state.copy(showDatePicker = action.show)
            is AddCalendarEventAction.OnCreateCalendarType -> createCalendarType(action.name, action.colorHex)
            is AddCalendarEventAction.OnDeleteCalendarType -> deleteCalendarType(action.calendar)
            AddCalendarEventAction.OnGetCurrentLocation -> getCurrentLocation()
            AddCalendarEventAction.OnSaveClicked -> saveEventToRoom(onBack)
        }
    }

    // Esegue i controlli di integrità oraria ed abilitazione form
    private fun validateFormState() {
        val isTimeInvalid = state.selectedEndTime.isBefore(state.selectedStartTime)

        state = state.copy(
            isTimeError = isTimeInvalid,
            timeErrorMessage = if (isTimeInvalid) "L'orario di fine non può antecedere quello d'inizio." else null,
            isButtonEnabled = state.title.isNotBlank() && state.selectedCalendarId != 0 && !isTimeInvalid
        )
    }

    // Inserisce una nuova categoria di calendario nel database
    private fun createCalendarType(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
            val newCalendar = CalendarTypeEntity(name = name, color = colorHex, userId = uid)
            eventRepository.saveCalendar(newCalendar)
        }
    }

    // Rimuove una categoria di calendario dal database
    private fun deleteCalendarType(calendar: CalendarTypeEntity) {
        viewModelScope.launch {
            eventRepository.deleteCalendar(calendar)
        }
    }

    // Salva il nuovo evento all'interno del database
    fun saveEventToRoom(onSuccess: () -> Unit) {
        if (!state.isButtonEnabled) return

        viewModelScope.launch {
            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (uid != null) {
                val nuovoEvento = EventEntity(
                    userUid = uid,
                    title = state.title,
                    location = state.location,
                    description = state.description,
                    date = state.selectedDate,
                    startTime = state.selectedStartTime,
                    endTime = state.selectedEndTime,
                    calendar = state.selectedCalendarId
                )
                eventRepository.saveEvent(nuovoEvento)
                onSuccess()
            }
        }
    }

    // Recupera la posizione geografica corrente tramite FusedLocationProvider
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        viewModelScope.launch {
            state = state.copy(isLoadingLocation = true)
            try {
                val result = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                result?.let { location ->
                    val addressName = getAddressFromLocation(location.latitude, location.longitude)
                    if (addressName != null) {
                        state = state.copy(location = addressName)
                        validateFormState()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { state = state.copy(isLoadingLocation = false) }
        }
    }

    // Esegue l'autocompletamento del testo per gli indirizzi tramite Geocoder
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

    // Converte lat/lng in una stringa testuale leggibile
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