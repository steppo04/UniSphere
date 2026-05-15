package com.example.unisphere.db.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_recipes")
data class FavoriteRecipeEntity(
    @PrimaryKey val id: Int, // Usiamo l'id di Spoonacular come chiave primaria
    val userUid: String,     // Associa il preferito all'utente corrente di Supabase
    val title: String,
    val image: String,
    val readyInMinutes: Int,
    val servings: Int
)