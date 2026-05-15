package com.example.unisphere.db.local.dao

import androidx.room.*
import com.example.unisphere.db.local.entity.CalendarTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {
    // Prende tutti i calendari di uno specifico utente
    @Query("SELECT * FROM calendars WHERE userId = :userId")
    fun getCalendarsForUser(userId: String): Flow<List<CalendarTypeEntity>>

    @Upsert // Gestisce sia l'inserimento che la modifica (se l'ID esiste)
    suspend fun saveCalendar(calendar: CalendarTypeEntity)

    @Delete
    suspend fun deleteCalendar(calendar: CalendarTypeEntity)

    @Query("SELECT * FROM calendars WHERE id = :id")
    suspend fun getCalendarById(id: Int): CalendarTypeEntity?
}