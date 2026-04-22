package com.example.unisphere.ui.screen.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDate

data class CalendarState(
    val selectedDate: LocalDate = LocalDate.now(),
    val events: List<Pair<String, String>> = emptyList()
)

sealed interface CalendarAction {
    data class OnDateSelected(val date: LocalDate) : CalendarAction
}

class CalendarViewModel : ViewModel() {
    var state by mutableStateOf(CalendarState())
        private set

    init {
        loadEventsForDate(LocalDate.now())
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.OnDateSelected -> {
                state = state.copy(selectedDate = action.date)
                loadEventsForDate(action.date)
            }
        }
    }

    private fun loadEventsForDate(date: LocalDate) {
        val mockEvents = if (date == LocalDate.now()) {
            listOf("Lezione Analisi 1" to "09:00", "Laboratorio" to "14:00")
        } else {
            emptyList()
        }
        state = state.copy(events = mockEvents)
    }
}