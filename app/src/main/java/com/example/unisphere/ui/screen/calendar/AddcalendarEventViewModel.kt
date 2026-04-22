package com.example.unisphere.ui.screen.calendar

import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

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
    val calendarTypes: List<String> = listOf("Personale", "Esami", "Lezioni", "Basket")
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
    data object OnSaveClicked : AddCalendarEventAction
}


class AddCalendarEventViewModel : ViewModel() {

    var state by mutableStateOf(AddCalendarEventState())
        private set

    fun onAction(action: AddCalendarEventAction, onBack: () -> Unit = {}) {
        when (action) {
            is AddCalendarEventAction.OnTitleChanged -> {
                state = state.copy(title = action.value)
            }
            is AddCalendarEventAction.OnLocationChanged -> {
                state = state.copy(location = action.value)
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
            is AddCalendarEventAction.ToggleDatePicker -> {
                state = state.copy(showDatePicker = action.show)
            }
            is AddCalendarEventAction.ToggleTimePicker -> {
                state = state.copy(showTimePicker = action.show)
            }
            AddCalendarEventAction.OnSaveClicked -> {
                onBack()
            }
        }
    }
}