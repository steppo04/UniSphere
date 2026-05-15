package com.example.unisphere.db.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "calendar_events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userUid: String, // Collega l'evento all'utente loggato
    val title: String,
    val description: String,
    val location: String,
    val date: LocalDate,
    val startTime: LocalTime, // Nuovo
    val endTime: LocalTime,   // Nuovo
    val calendar: Int
)