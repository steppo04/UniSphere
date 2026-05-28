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
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.db.local.entity.PointOfInterestEntity
import com.example.unisphere.repository.PoiRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

data class MapState(
    val pois: List<PointOfInterestEntity> = emptyList(),
    val showAddDialog: Boolean = false,
    val selectedPoi: PointOfInterestEntity? = null,
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
    data class OnDeletePoiClicked(val poi: PointOfInterestEntity) : MapAction
    data class OnPoiSelected(val poi: PointOfInterestEntity?) : MapAction
    data class OnSuggestionSelected(val address: String) : MapAction
    data object OnUseCurrentLocation : MapAction
}

@HiltViewModel
class MapViewModel @Inject constructor(
    application: Application,
    private val poiRepository: PoiRepository
) : AndroidViewModel(application) {

    var state by mutableStateOf(MapState())
        private set

    private var searchJob: Job? = null
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    init {
        loadUserPois()
    }

    private fun loadUserPois() {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
        viewModelScope.launch {
            poiRepository.getPois(uid).collectLatest { list ->
                state = state.copy(pois = list)
            }
        }
    }

    fun onAction(action: MapAction) {
        when (action) {
            is MapAction.OnNameChanged -> state = state.copy(newPoiName = action.value)
            is MapAction.OnAddressChanged -> {
                state = state.copy(newPoiAddress = action.value)
                fetchAddressSuggestions(action.value)
            }
            is MapAction.OnNotesChanged -> state = state.copy(newPoiNotes = action.value)

            MapAction.OnSavePoiClicked -> {
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
                if (state.newPoiName.isNotBlank() && state.newPoiAddress.isNotBlank()) {
                    viewModelScope.launch {
                        val coords = getCoordinatesFromAddress(state.newPoiAddress)
                        val newPoi = PointOfInterestEntity(
                            userUid = uid,
                            name = state.newPoiName,
                            address = state.newPoiAddress,
                            notes = state.newPoiNotes,
                            latitude = coords?.first ?: 0.0,
                            longitude = coords?.second ?: 0.0
                        )
                        poiRepository.savePoi(newPoi)

                        state = state.copy(
                            showAddDialog = false,
                            newPoiName = "",
                            newPoiAddress = "",
                            newPoiNotes = "",
                            addressSuggestions = emptyList()
                        )
                    }
                }
            }
            MapAction.OnAddPoiClicked -> state = state.copy(showAddDialog = true)
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
                viewModelScope.launch {
                    poiRepository.deletePoi(action.poi)
                    if (state.selectedPoi?.id == action.poi.id) {
                        state = state.copy(selectedPoi = null)
                    }
                }
            }
            is MapAction.OnPoiSelected -> state = state.copy(selectedPoi = action.poi)
            is MapAction.OnSuggestionSelected -> state = state.copy(newPoiAddress = action.address, addressSuggestions = emptyList())
            MapAction.OnUseCurrentLocation -> getCurrentLocation()
        }
    }

    // Cerca gli indirizzi consigliati tramite query testuale
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

    // Traduce una stringa testuale in coordinate Lat/Lng mediante Geocoder I/O thread
    private suspend fun getCoordinatesFromAddress(addressName: String): Pair<Double, Double>? {
        val geocoder = Geocoder(getApplication(), Locale.getDefault())
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(addressName, 1)
                addresses?.firstOrNull()?.let { Pair(it.latitude, it.longitude) }
            } catch (e: Exception) { null }
        }
    }

    // Intercetta la posizione GPS corrente inserendola nel modulo indirizzo
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        viewModelScope.launch {
            state = state.copy(isLocating = true)
            try {
                val result = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                result?.let { location ->
                    val addressName = getAddressFromLocation(location.latitude, location.longitude)
                    if (addressName != null) state = state.copy(newPoiAddress = addressName)
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
                } catch (e: Exception) { continuation.resume(null) }
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.getAddressLine(0)
                } catch (e: Exception) { null }
            }
        }
    }
}