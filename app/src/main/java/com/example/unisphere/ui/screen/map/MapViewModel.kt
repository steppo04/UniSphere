package com.example.unisphere.ui.screen.map

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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class PointOfInterest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val notes: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class MapState(
    val pois: List<PointOfInterest> = listOf(
        PointOfInterest(
            name = "Campus Cesena",
            address = "Via dell'Università, 50, Cesena",
            notes = "Sede principale dei corsi di Informatica.",
            latitude = 44.1481,
            longitude = 12.2359
        ),
        PointOfInterest(
            name = "Mensa Universitaria Cesena",
            address = "Via Pasi, 30, Cesena",
            notes = "Mensa per studenti universitari.",
            latitude = 44.1465,
            longitude = 12.2415
        )
    ),
    val showAddDialog: Boolean = false,
    val selectedPoi: PointOfInterest? = null,
    val newPoiName: String = "",
    val newPoiAddress: String = "",
    val newPoiNotes: String = "",
    val addressSuggestions: List<String> = emptyList(),
    val isLocating: Boolean = false,
    val isSearchingSuggestions: Boolean = false
)

sealed interface MapAction {
    data class OnNameChanged(val value: String) : MapAction
    data class OnAddressChanged(val value: String) : MapAction
    data class OnNotesChanged(val value: String) : MapAction
    data object OnSavePoiClicked : MapAction
    data object OnAddPoiClicked : MapAction
    data object OnDismissAddDialog : MapAction
    data class OnDeletePoiClicked(val id: String) : MapAction
    data class OnPoiSelected(val poi: PointOfInterest?) : MapAction
    data class OnSuggestionSelected(val address: String) : MapAction
    data object OnUseCurrentLocation : MapAction
}

class MapViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf(MapState())
        private set

    private var searchJob: Job? = null
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.OnNameChanged -> {
                state = state.copy(newPoiName = action.value)
            }
            is MapAction.OnAddressChanged -> {
                state = state.copy(newPoiAddress = action.value)
                fetchAddressSuggestions(action.value)
            }
            is MapAction.OnNotesChanged -> {
                state = state.copy(newPoiNotes = action.value)
            }
            MapAction.OnSavePoiClicked -> {
                if (state.newPoiName.isNotBlank() && state.newPoiAddress.isNotBlank()) {
                    viewModelScope.launch {
                        val coords = getCoordinatesFromAddress(state.newPoiAddress)
                        val newPoi = PointOfInterest(
                            name = state.newPoiName,
                            address = state.newPoiAddress,
                            notes = state.newPoiNotes,
                            latitude = coords?.first ?: 0.0,
                            longitude = coords?.second ?: 0.0
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
            }
            MapAction.OnAddPoiClicked -> {
                state = state.copy(showAddDialog = true)
            }
            MapAction.OnDismissAddDialog -> {
                state = state.copy(
                    showAddDialog = false,
                    addressSuggestions = emptyList(),
                    newPoiName = "",
                    newPoiAddress = "",
                    newPoiNotes = ""
                )
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
            MapAction.OnUseCurrentLocation -> {
                getCurrentLocation()
            }
        }
    }

    private fun fetchAddressSuggestions(query: String) {
        if (query.length < 3) {
            state = state.copy(addressSuggestions = emptyList())
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
                state = state.copy(addressSuggestions = suggestions)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                state = state.copy(isSearchingSuggestions = false)
            }
        }
    }

    private suspend fun getCoordinatesFromAddress(addressName: String): Pair<Double, Double>? {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(addressName, 1)
                addresses?.firstOrNull()?.let {
                    Pair(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        viewModelScope.launch {
            state = state.copy(isLocating = true)
            try {
                val result = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()

                result?.let { location ->
                    val addressName = getAddressFromLocation(location.latitude, location.longitude)
                    if (addressName != null) {
                        state = state.copy(newPoiAddress = addressName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                state = state.copy(isLocating = false)
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
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}
