package com.example.unisphere.db.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "points_of_interest")
data class PointOfInterestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // ID incrementale automatico preferito da Room
    val userUid: String, // Collega il punto all'utente loggato su Supabase
    val name: String,
    val address: String,
    val notes: String,
    val latitude: Double,
    val longitude: Double
)