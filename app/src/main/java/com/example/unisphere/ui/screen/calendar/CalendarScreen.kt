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
import androidx.navigation.NavHostController
import com.example.unisphere.ui.composables.NavigationRoute
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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
        topBar = {
            TopAppBar(
                title = { Text("UniCalendar", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(NavigationRoute.AddCalendarEvent) },
                containerColor = MaterialTheme.colorScheme.primary
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
                        .padding(8.dp)
                ) {
                    EventListSection(uiState)
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
                EventListSection(uiState)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
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
fun EventCard(title: String, time: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Spacer(Modifier.width(12.dp))
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(time, style = MaterialTheme.typography.labelMedium)
        }
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
    state: com.kizitonwose.calendar.compose.CalendarState,
    daysOfWeek: List<DayOfWeek>,
    uiState: CalendarState,
    viewModel: CalendarViewModel
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)
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

@Composable
fun EventListSection(uiState: CalendarState) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = "Eventi del ${uiState.selectedDate.dayOfMonth} ${uiState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.ITALY)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.events.isEmpty()) {
            Text("Nessun evento", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
        } else {
            uiState.events.forEach { (title, time) ->
                EventCard(title, time)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}