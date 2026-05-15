package com.example.unisphere.db.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unisphere.db.local.entity.FavoriteRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(recipe: FavoriteRecipeEntity)

    @Delete
    suspend fun deleteFavorite(recipe: FavoriteRecipeEntity)

    @Query("SELECT * FROM favorite_recipes WHERE userUid = :userId")
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteRecipeEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_recipes WHERE id = :recipeId AND userUid = :userId)")
    fun isRecipeFavorite(recipeId: Int, userId: String): Flow<Boolean>
}