package com.example.unisphere.repository

import com.example.unisphere.db.local.dao.EventDao
import com.example.unisphere.db.local.dao.CalendarDao
import com.example.unisphere.db.local.entity.CalendarTypeEntity
import com.example.unisphere.db.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject


class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val calendarDao: CalendarDao
) {
    suspend fun saveEvent(event: EventEntity) = eventDao.insertEvent(event)

    fun getEventsByDate(uid: String, date: LocalDate): Flow<List<EventEntity>> =
        eventDao.getEventsForDate(uid, date)

    fun getEventById(id: Int): Flow<EventEntity?> = eventDao.getEventById(id)
    suspend fun deleteEvent(event: EventEntity) = eventDao.deleteEvent(event)

    // --- FUNZIONI PER CALENDARI ---

    fun getCalendarsForUser(userId: String) = calendarDao.getCalendarsForUser(userId)
    suspend fun saveCalendar(calendar: CalendarTypeEntity) = calendarDao.saveCalendar(calendar)
    suspend fun deleteCalendar(calendar: CalendarTypeEntity) = calendarDao.deleteCalendar(calendar)
}