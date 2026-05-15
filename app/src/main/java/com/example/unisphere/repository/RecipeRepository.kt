package com.example.unisphere.repository

import com.example.unisphere.db.local.dao.RecipeDao
import com.example.unisphere.db.local.entity.FavoriteRecipeEntity
import com.example.unisphere.ui.screen.cook.RecipeDetail
import com.example.unisphere.ui.screen.cook.RecipeResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val client: HttpClient,
    private val recipeDao: RecipeDao
) {
    private val apiKey = "e09429ff9b1c4c3ca2e6e4318890b313"
    private val baseUrl = "https://api.spoonacular.com/recipes"

    // --- CHIAMATE DI RETE (API) ---
    suspend fun searchRecipes(query: String): RecipeResponse {
        return client.get("$baseUrl/complexSearch") {
            parameter("apiKey", apiKey)
            parameter("query", query)
            parameter("number", 20)
            parameter("addRecipeInformation", true)
        }.body()
    }

    suspend fun getRecipeDetails(id: Int): RecipeDetail {
        return client.get("$baseUrl/$id/information") {
            parameter("apiKey", apiKey)
        }.body()
    }

    // --- DATABASE LOCALE (ROOM) ---
    fun getFavorites(userId: String): Flow<List<FavoriteRecipeEntity>> = recipeDao.getFavoritesForUser(userId)

    fun isFavorite(recipeId: Int, userId: String): Flow<Boolean> = recipeDao.isRecipeFavorite(recipeId, userId)

    suspend fun saveFavorite(recipe: FavoriteRecipeEntity) = recipeDao.insertFavorite(recipe)

    suspend fun removeFavorite(recipe: FavoriteRecipeEntity) = recipeDao.deleteFavorite(recipe)
}