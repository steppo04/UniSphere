package com.example.unisphere.ui.screen.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.db.local.entity.CalendarTypeEntity
import com.example.unisphere.db.local.entity.EventEntity
import com.example.unisphere.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// --- STATO AGGIORNATO ---
data class CalendarState(
    val selectedDate: LocalDate = LocalDate.now(),
    val events: List<EventEntity> = emptyList(),
    val calendars: List<CalendarTypeEntity> = emptyList() // Aggiunta la lista dei calendari per mappare i colori
)

sealed interface CalendarAction {
    data class OnDateSelected(val date: LocalDate) : CalendarAction
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    var state by mutableStateOf(CalendarState())
        private set

    private var eventsJob: Job? = null
    private var calendarsJob: Job? = null

    init {
        loadUserCalendars()
        onAction(CalendarAction.OnDateSelected(LocalDate.now()))
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.OnDateSelected -> {
                state = state.copy(selectedDate = action.date)
                observeEvents(action.date)
            }
        }
    }

    private fun loadUserCalendars() {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        calendarsJob?.cancel()
        calendarsJob = viewModelScope.launch {
            // Restiamo in ascolto dei tipi di calendario per aggiornare i colori al volo se cambiano
            eventRepository.getCalendarsForUser(uid).collectLatest { listaCalendari ->
                state = state.copy(calendars = listaCalendari)
            }
        }
    }

    private fun observeEvents(date: LocalDate) {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return

        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            eventRepository.getEventsByDate(uid, date).collect { listaEventi ->
                state = state.copy(events = listaEventi)
            }
        }
    }
}