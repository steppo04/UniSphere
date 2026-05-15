package com.example.unisphere.db.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.example.unisphere.db.local.entity.EventEntity // Import corretto
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate // IMPORT FONDAMENTALE

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Query("SELECT * FROM calendar_events WHERE userUid = :uid AND date = :date ORDER BY startTime ASC")
    fun getEventsForDate(uid: String, date: LocalDate): Flow<List<EventEntity>>

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    fun getEventById(id: Int): Flow<EventEntity?>

}