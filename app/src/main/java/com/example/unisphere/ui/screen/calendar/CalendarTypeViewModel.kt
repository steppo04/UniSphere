package com.example.unisphere.ui.screen.calendar


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.local.entity.CalendarTypeEntity
import com.example.unisphere.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CalendarSettingsState(
    val calendars: List<CalendarTypeEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CalendarSettingsViewModel @Inject constructor(
    private val repository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarSettingsState())
    val state: StateFlow<CalendarSettingsState> = _state

    // ID Utente (simulato o preso dal tuo AuthManager)
    private val currentUserId = "user_123"

    init {
        loadCalendars()
    }

    private fun loadCalendars() {
        viewModelScope.launch {
            repository.getCalendarsForUser(currentUserId).collectLatest { list ->
                _state.value = _state.value.copy(calendars = list, isLoading = false)
            }
        }
    }

    fun saveCalendar(name: String, color: String, id: Int = 0) {
        viewModelScope.launch {
            val calendar = CalendarTypeEntity(
                id = id,
                name = name,
                color = color,
                userId = currentUserId
            )
            repository.saveCalendar(calendar)
        }
    }

    fun deleteCalendar(calendar: CalendarTypeEntity) {
        viewModelScope.launch {
            repository.deleteCalendar(calendar)
        }
    }
}