package com.example.unisphere.ui.screen.cook

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.db.local.entity.FavoriteRecipeEntity
import com.example.unisphere.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class RecipeDetail(
    val id: Int,
    val title: String,
    val image: String,
    val summary: String = "",
    val instructions: String? = null,
    val readyInMinutes: Int = 0,
    val servings: Int = 0,
    val extendedIngredients: List<Ingredient> = emptyList()
)

@Serializable
data class Ingredient(
    val original: String
)

data class RecipeDetailState(
    val recipe: RecipeDetail? = null,
    val isLoading: Boolean = true,
    val isFavorite: Boolean = false,
    val error: String? = null
)

sealed interface RecipeDetailAction {
    object OnToggleFavorite : RecipeDetailAction
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    application: Application,
    private val repository: RecipeRepository,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    var state by mutableStateOf(RecipeDetailState())
        private set

    private val recipeId: Int = checkNotNull(savedStateHandle["recipeId"])
    private val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"

    init {
        fetchRecipeDetails()
        observeFavoriteStatus()
    }

    private fun fetchRecipeDetails() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val details = repository.getRecipeDetails(recipeId)
                state = state.copy(recipe = details, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = "Impossibile caricare i dettagli della ricetta")
                e.printStackTrace()
            }
        }
    }

    private fun observeFavoriteStatus() {
        viewModelScope.launch {
            repository.isFavorite(recipeId, currentUserId).collectLatest { favorite ->
                state = state.copy(isFavorite = favorite)
            }
        }
    }

    fun onAction(action: RecipeDetailAction) {
        when (action) {
            RecipeDetailAction.OnToggleFavorite -> toggleFavorite()
        }
    }

    private fun toggleFavorite() {
        val currentRecipe = state.recipe ?: return
        viewModelScope.launch {
            val favoriteEntity = FavoriteRecipeEntity(
                id = currentRecipe.id,
                userUid = currentUserId,
                title = currentRecipe.title,
                image = currentRecipe.image,
                readyInMinutes = currentRecipe.readyInMinutes,
                servings = currentRecipe.servings
            )

            if (state.isFavorite) {
                repository.removeFavorite(favoriteEntity)
            } else {
                repository.saveFavorite(favoriteEntity)
            }
        }
    }
}