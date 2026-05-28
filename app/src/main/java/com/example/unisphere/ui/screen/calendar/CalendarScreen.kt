package com.example.unisphere.ui.screen.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.unisphere.db.local.entity.EventEntity
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.composables.UniSphereEmptyState
import com.example.unisphere.ui.composables.UniSphereListItem
import com.example.unisphere.ui.composables.UniSphereSectionHeader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState = viewModel.state
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = { AppBar(title = "UniCalendar", navController) },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(NavigationRoute.AddCalendarEvent) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi", tint = Color.White)
            }
        }
    ) { padding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    CustomCalendarCard(
                        currentMonth = uiState.currentMonth,
                        gridDays = uiState.gridDays,
                        selectedDate = uiState.selectedDate,
                        onMonthChange = { viewModel.onAction(CalendarAction.OnMonthChanged(it)) },
                        onDateSelected = { viewModel.onAction(CalendarAction.OnDateSelected(it)) }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    EventListSection(uiState, navController)
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                CustomCalendarCard(
                    currentMonth = uiState.currentMonth,
                    gridDays = uiState.gridDays,
                    selectedDate = uiState.selectedDate,
                    onMonthChange = { viewModel.onAction(CalendarAction.OnMonthChanged(it)) },
                    onDateSelected = { viewModel.onAction(CalendarAction.OnDateSelected(it)) }
                )
                EventListSection(uiState, navController)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
//card calendario
@Composable
private fun CustomCalendarCard(
    currentMonth: YearMonth,
    gridDays: List<LocalDate?>,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header di navigazione mesi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALY).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mese precedente")
                    }
                    IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mese successivo")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Giorni della settimana fissi
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                daysOfWeek.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, Locale.ITALY).uppercase(),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rendering passivo delle righe tramite i blocchi calcolati nel ViewModel
            val chunks = gridDays.chunked(7)
            chunks.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    week.forEach { date ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (week.size < 7) {
                        Spacer(modifier = Modifier.weight((7 - week.size).toFloat()))
                    }
                }
            }
        }
    }
}

//Sezione eventi
@Composable
private fun EventListSection(uiState: CalendarState, navController: NavHostController) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

        UniSphereSectionHeader(
            title = "Eventi del ${uiState.selectedDate.dayOfMonth} ${uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.ITALY)}"
        )

        if (uiState.events.isEmpty()) {
            UniSphereEmptyState(
                icon = Icons.Default.EventNote,
                title = "Nessun impegno",
                description = "Goditi il tempo libero! Non ci sono eventi o turni registrati per questa giornata."
            )
        } else {
            uiState.events.forEachIndexed { index, evento ->
                val matchedCalendar = uiState.calendars.find { it.id == evento.calendar }
                val calendarColorHex = matchedCalendar?.color ?: "#8E8E93"

                CalendarEventItem(event = evento, calendarColorHex = calendarColorHex, navController = navController)

                if (index < uiState.events.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// singolo evento
@Composable
private fun CalendarEventItem(event: EventEntity, calendarColorHex: String, navController: NavHostController) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val barColor = remember(calendarColorHex) {
        try {
            Color(calendarColorHex.toColorInt())
        } catch (_: Exception) {
            Color.Gray
        }
    }

    UniSphereListItem(
        headlineText = event.title,
        supportingText = event.location.ifBlank { null },
        leadingBarColor = barColor,
        onClick = { navController.navigate(NavigationRoute.EventDetailScreen(eventId = event.id)) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = event.startTime.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = event.endTime.format(timeFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    )
}