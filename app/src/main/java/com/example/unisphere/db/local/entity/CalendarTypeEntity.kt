package com.example.unisphere.db.local.entity


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendars")
data class CalendarTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String,
    val userId: String
)