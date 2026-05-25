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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModelgit 
import androidx.navigation.NavHostController
import com.example.unisphere.db.local.entity.EventEntity
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import com.example.unisphere.ui.composables.NavigationRoute
import com.example.unisphere.ui.composables.UniSphereEmptyState
import com.example.unisphere.ui.composables.UniSphereListItem
import com.example.unisphere.ui.composables.UniSphereSectionHeader
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.CalendarState as LibCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState = viewModel.state
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(50) }
    val endMonth = remember { currentMonth.plusMonths(50) }
    val daysOfWeek = remember { daysOfWeek() }
    val calendarState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

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
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    CalendarHeaderSection(calendarState.firstVisibleMonth.yearMonth)
                    CalendarGridCard(calendarState, daysOfWeek, uiState, viewModel)
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
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                CalendarHeaderSection(calendarState.firstVisibleMonth.yearMonth)
                CalendarGridCard(calendarState, daysOfWeek, uiState, viewModel)
                EventListSection(uiState, navController)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun EventListSection(uiState: CalendarState, navController: NavHostController) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {

        // REFACTOR: Intestazione unificata tramite UniSphereSectionHeader
        UniSphereSectionHeader(
            title = "Eventi del ${uiState.selectedDate.dayOfMonth} ${uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.ITALY)}"
        )

        if (uiState.events.isEmpty()) {
            // REFACTOR: Gestione dell'Empty State centralizzata e grafica avanzata
            UniSphereEmptyState(
                icon = Icons.Default.EventNote,
                title = "Nessun impegno",
                description = "Goditi il tempo libero! Non ci sono eventi o turni registrati per questa giornata."
            )
        } else {
            uiState.events.forEachIndexed { index, evento ->
                val matchedCalendar = uiState.calendars.find { it.id == evento.calendar }
                val calendarColorHex = matchedCalendar?.color ?: "#8E8E93"

                EventCard(event = evento, calendarColorHex = calendarColorHex, navController = navController)

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

@Composable
fun EventCard(event: EventEntity, calendarColorHex: String, navController: NavHostController) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val barColor = remember(calendarColorHex) {
        try {
            Color(android.graphics.Color.parseColor(calendarColorHex))
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

@Composable
fun DayElement(day: CalendarDay, isSelected: Boolean, isToday: Boolean, onClick: (CalendarDay) -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> Color.White
                day.position != DayPosition.MonthDate -> Color.LightGray
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALY).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun CalendarHeaderSection(visibleMonth: YearMonth) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALY).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = visibleMonth.year.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun CalendarGridCard(
    state: LibCalendarState,
    daysOfWeek: List<DayOfWeek>,
    uiState: CalendarState,
    viewModel: CalendarViewModel
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            DaysOfWeekTitle(daysOfWeek = daysOfWeek)
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    DayElement(
                        day = day,
                        isSelected = uiState.selectedDate == day.date,
                        isToday = day.date == LocalDate.now(),
                        onClick = { viewModel.onAction(CalendarAction.OnDateSelected(it.date)) }
                    )
                }
            )
        }
    }
}