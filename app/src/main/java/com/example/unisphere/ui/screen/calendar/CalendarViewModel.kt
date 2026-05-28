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
import java.time.YearMonth
import javax.inject.Inject

data class CalendarState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentMonth: YearMonth = YearMonth.now(),
    val gridDays: List<LocalDate?> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val calendars: List<CalendarTypeEntity> = emptyList()
)

sealed interface CalendarAction {
    data class OnDateSelected(val date: LocalDate) : CalendarAction
    data class OnMonthChanged(val month: YearMonth) : CalendarAction
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
        generateCalendarGrid(state.currentMonth)
        observeEvents(state.selectedDate)
    }

    fun onAction(action: CalendarAction) {
        when (action) {
            is CalendarAction.OnDateSelected -> {
                if (state.selectedDate != action.date) {
                    state = state.copy(selectedDate = action.date)
                    observeEvents(action.date)
                }
            }
            is CalendarAction.OnMonthChanged -> {
                state = state.copy(currentMonth = action.month)
                generateCalendarGrid(action.month)
            }
        }
    }

    private fun generateCalendarGrid(month: YearMonth) {
        val firstDayOfMonth = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value - 1

        val totalGridItems = mutableListOf<LocalDate?>()

        for (i in 0 until firstDayOfWeekIndex) {
            totalGridItems.add(null)
        }

        for (day in 1..daysInMonth) {
            totalGridItems.add(month.atDay(day))
        }

        state = state.copy(gridDays = totalGridItems)
    }

    private fun loadUserCalendars() {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        calendarsJob?.cancel()
        calendarsJob = viewModelScope.launch {
            eventRepository.getCalendarsForUser(uid).collectLatest { listaCalendari ->
                state = state.copy(calendars = listaCalendari)
            }
        }
    }

    private fun observeEvents(date: LocalDate) {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            eventRepository.getEventsByDate(uid, date).collectLatest { listaEventi ->
                state = state.copy(events = listaEventi)
            }
        }
    }
}