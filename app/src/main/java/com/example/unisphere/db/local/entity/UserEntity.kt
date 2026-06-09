package com.example.unisphere.db.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = false)
    val uid: String,
    val email: String,
    val name: String,
    val surname: String,
    val username: String,
    val profilePictureUri: String? = null, // Opzionale, l'utente potrebbe non averla
    val currentTheme: String = "Default" // Theme corrente dell'utente"

)