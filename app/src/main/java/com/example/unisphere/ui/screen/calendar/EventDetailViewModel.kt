package com.example.unisphere.ui.screen.calendar

import android.app.Application
import android.location.Geocoder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.local.entity.EventEntity
import com.example.unisphere.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.util.Locale
import javax.inject.Inject

// --- STATO DEL DETTAGLIO AGGIORNATO ---
data class EventDetailState(
    val event: EventEntity? = null,
    val calendarName: String = "Caricamento...",
    val calendarColorHex: String = "#8E8E93", // Grigio neutro di default
    val isLoading: Boolean = true,
    val geoPoint: GeoPoint? = null
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val repository: EventRepository,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var state by mutableStateOf(EventDetailState())
        private set

    private val eventId: Int = checkNotNull(savedStateHandle["eventId"])

    init {
        loadEvent()
    }

    private fun loadEvent() {
        viewModelScope.launch {
            // Ascolta l'evento dal DB in tempo reale
            repository.getEventById(eventId).collectLatest { event ->
                if (event != null) {
                    // Coordinate per la mappa
                    val point = if (event.location.isNotBlank()) {
                        getCoordinatesFromAddress(event.location)
                    } else null

                    // Recupera la lista dei calendari dell'utente per trovare quello associato
                    val calendars = repository.getCalendarsForUser(event.userUid).firstOrNull() ?: emptyList()
                    val matchedCalendar = calendars.find { it.id == event.calendar }

                    state = state.copy(
                        event = event,
                        calendarName = matchedCalendar?.name ?: "Nessun Calendario",
                        calendarColorHex = matchedCalendar?.color ?: "#8E8E93",
                        geoPoint = point,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Trasforma l'indirizzo testuale in un GeoPoint (Latitudine e Longitudine)
     */
    private suspend fun getCoordinatesFromAddress(address: String): GeoPoint? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(application, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)

                results?.firstOrNull()?.let {
                    GeoPoint(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Elimina l'evento corrente dal database
     */
    fun deleteEvent(onSuccess: () -> Unit) {
        state.event?.let { currentEvent ->
            viewModelScope.launch {
                repository.deleteEvent(currentEvent)
                onSuccess()
            }
        }
    }
}