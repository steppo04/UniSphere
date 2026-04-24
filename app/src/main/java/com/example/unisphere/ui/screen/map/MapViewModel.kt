package com.example.unisphere.ui.screen.map

import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

data class MapState(
    val pois: List<PointOfInterest> = listOf(
        PointOfInterest(
            name = "Biblioteca Centrale",
            address = "Via dell'Università, 1, Roma",
            notes = "Ottimo posto per studiare in silenzio."
        ),
        PointOfInterest(
            name = "Mensa Universitaria",
            address = "Piazza Studenti, 5, Roma",
            notes = "Pranzo economico, chiude alle 15:00."
        )
    ),
    val showAddDialog: Boolean = false,
    val selectedPoi: PointOfInterest? = null,
    val newPoiName: String = "",
    val newPoiAddress: String = "",
    val newPoiNotes: String = "",
    val addressSuggestions: List<String> = emptyList(),
    val isLocating: Boolean = false
)

sealed interface MapAction {
    data class OnNameChanged(val value: String) : MapAction
    data class OnAddressChanged(val value: String, val context: Context) : MapAction
    data class OnNotesChanged(val value: String) : MapAction
    data object OnSavePoiClicked : MapAction
    data object OnAddPoiClicked : MapAction
    data object OnDismissAddDialog : MapAction
    data class OnDeletePoiClicked(val id: String) : MapAction
    data class OnPoiSelected(val poi: PointOfInterest?) : MapAction
    data class OnSuggestionSelected(val address: String) : MapAction
    data class OnUseCurrentLocation(val context: Context) : MapAction
}

class MapViewModel : ViewModel() {
    var state by mutableStateOf(MapState())
        private set

    private var searchJob: Job? = null

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.OnNameChanged -> {
                state = state.copy(newPoiName = action.value)
            }
            is MapAction.OnAddressChanged -> {
                state = state.copy(newPoiAddress = action.value)
                getSuggestions(action.value, action.context)
            }
            is MapAction.OnNotesChanged -> {
                state = state.copy(newPoiNotes = action.value)
            }
            MapAction.OnSavePoiClicked -> {
                if (state.newPoiName.isNotBlank() && state.newPoiAddress.isNotBlank()) {
                    val newPoi = PointOfInterest(
                        name = state.newPoiName,
                        address = state.newPoiAddress,
                        notes = state.newPoiNotes
                    )
                    state = state.copy(
                        pois = state.pois + newPoi,
                        showAddDialog = false,
                        newPoiName = "",
                        newPoiAddress = "",
                        newPoiNotes = "",
                        addressSuggestions = emptyList()
                    )
                }
            }
            MapAction.OnAddPoiClicked -> {
                state = state.copy(showAddDialog = true)
            }
            MapAction.OnDismissAddDialog -> {
                state = state.copy(showAddDialog = false, addressSuggestions = emptyList(), newPoiName = "", newPoiAddress = "", newPoiNotes = "")
            }
            is MapAction.OnDeletePoiClicked -> {
                state = state.copy(pois = state.pois.filter { it.id != action.id })
            }
            is MapAction.OnPoiSelected -> {
                state = state.copy(selectedPoi = action.poi)
            }
            is MapAction.OnSuggestionSelected -> {
                state = state.copy(newPoiAddress = action.address, addressSuggestions = emptyList())
            }
            is MapAction.OnUseCurrentLocation -> {
                getCurrentLocation(action.context)
            }
        }
    }

    private fun getSuggestions(query: String, context: Context) {
        searchJob?.cancel()
        if (query.length < 3) {
            state = state.copy(addressSuggestions = emptyList())
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(query, 5) { addresses ->
                        state = state.copy(addressSuggestions = addresses.mapNotNull { it.getAddressLine(0) })
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(query, 5)
                    state = state.copy(addressSuggestions = addresses?.mapNotNull { it.getAddressLine(0) } ?: emptyList())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun getCurrentLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        state = state.copy(isLocating = true)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                    val address = addresses.firstOrNull()?.getAddressLine(0) ?: ""
                                    state = state.copy(newPoiAddress = address, isLocating = false)
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                val address = addresses?.firstOrNull()?.getAddressLine(0) ?: ""
                                state = state.copy(newPoiAddress = address, isLocating = false)
                            }
                        } catch (e: Exception) {
                            state = state.copy(isLocating = false)
                        }
                    }
                } else {
                    state = state.copy(isLocating = false)
                }
            }.addOnFailureListener {
                state = state.copy(isLocating = false)
            }
        } catch (e: SecurityException) {
            state = state.copy(isLocating = false)
        }
    }
}
