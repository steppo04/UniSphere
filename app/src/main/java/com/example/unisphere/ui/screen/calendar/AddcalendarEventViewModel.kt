package com.example.unisphere.ui.screen.calendar

import android.annotation.SuppressLint
import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class AddCalendarEventState(
    val title: String = "",
    val location: String = "",
    val description: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.of(9, 0),
    val selectedType: String = "Personale",
    val isTypeExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
    val calendarTypes: List<String> = listOf("Personale", "Esami", "Lezioni", "Basket"),
    val isLoadingLocation: Boolean = false,
    val isLocationExpanded: Boolean = false,
    val locationSuggestions: List<String> = emptyList(),
    val isSearchingSuggestions: Boolean = false
)

sealed interface AddCalendarEventAction {
    data class OnTitleChanged(val value: String) : AddCalendarEventAction
    data class OnLocationChanged(val value: String) : AddCalendarEventAction
    data class OnDescriptionChanged(val value: String) : AddCalendarEventAction
    data class OnTypeChanged(val value: String) : AddCalendarEventAction
    data class OnDateChanged(val value: LocalDate) : AddCalendarEventAction
    data class OnTimeChanged(val value: LocalTime) : AddCalendarEventAction
    data class ToggleTypeExpanded(val expanded: Boolean) : AddCalendarEventAction
    data class ToggleDatePicker(val show: Boolean) : AddCalendarEventAction
    data class ToggleTimePicker(val show: Boolean) : AddCalendarEventAction
    data class ToggleLocationExpanded(val expanded: Boolean) : AddCalendarEventAction
    data object OnSaveClicked : AddCalendarEventAction
    data object OnGetCurrentLocation : AddCalendarEventAction
}

class AddCalendarEventViewModel(application: Application) : AndroidViewModel(application) {

    var state by mutableStateOf(AddCalendarEventState())
        private set

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    fun onAction(action: AddCalendarEventAction, onBack: () -> Unit = {}) {
        when (action) {
            is AddCalendarEventAction.OnTitleChanged -> {
                state = state.copy(title = action.value)
            }
            is AddCalendarEventAction.OnDescriptionChanged -> {
                state = state.copy(description = action.value)
            }
            is AddCalendarEventAction.OnTypeChanged -> {
                state = state.copy(selectedType = action.value, isTypeExpanded = false)
            }
            is AddCalendarEventAction.OnDateChanged -> {
                state = state.copy(selectedDate = action.value, showDatePicker = false)
            }
            is AddCalendarEventAction.OnTimeChanged -> {
                state = state.copy(selectedTime = action.value, showTimePicker = false)
            }
            is AddCalendarEventAction.ToggleTypeExpanded -> {
                state = state.copy(isTypeExpanded = action.expanded)
            }
            is AddCalendarEventAction.ToggleLocationExpanded -> {
                state = state.copy(isLocationExpanded = action.expanded)
            }
            is AddCalendarEventAction.OnLocationChanged -> {
                state = state.copy(location = action.value, isLocationExpanded = true)
                fetchLocationSuggestions(action.value)
            }
            is AddCalendarEventAction.ToggleDatePicker -> {
                state = state.copy(showDatePicker = action.show)
            }
            is AddCalendarEventAction.ToggleTimePicker -> {
                state = state.copy(showTimePicker = action.show)
            }
            AddCalendarEventAction.OnSaveClicked -> {
                onBack()
            }
            AddCalendarEventAction.OnGetCurrentLocation -> {
                getCurrentLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        viewModelScope.launch {
            state = state.copy(isLoadingLocation = true)
            try {
                val result = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                result?.let { location ->
                    val addressName = getAddressFromLocation(location.latitude, location.longitude)

                    if (addressName != null) {
                        state = state.copy(location = addressName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                state = state.copy(isLoadingLocation = false)
            }
        }
    }
    private var searchJob: Job? = null // Per cancellare la ricerca precedente se l'utente scrive veloce

    private fun fetchLocationSuggestions(query: String) {
        if (query.length < 3) {
            state = state.copy(locationSuggestions = emptyList())
            return
        }

        // Cancelliamo la ricerca precedente per risparmiare risorse
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Aspetta 500ms dopo l'ultimo carattere digitato
            state = state.copy(isSearchingSuggestions = true)

            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                // Cerchiamo fino a 5 indirizzi simili
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 5)
                }

                val suggestions = addresses?.mapNotNull { it.getAddressLine(0) } ?: emptyList()
                state = state.copy(locationSuggestions = suggestions)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                state = state.copy(isSearchingSuggestions = false)
            }
        }
    }

    private suspend fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            continuation.resume(addresses.firstOrNull()?.getAddressLine(0))
                        }
                        override fun onError(errorMessage: String?) {
                            continuation.resume(null)
                        }
                    })
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
            } catch (e: Exception) {
                null
            }
        }
    }
}