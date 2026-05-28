package com.example.unisphere.ui.screen.cook

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.db.SupabaseClient
import com.example.unisphere.db.local.entity.FavoriteRecipeEntity
import com.example.unisphere.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoriteRecipesState(
    val favorites: List<FavoriteRecipeEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoriteRecipesViewModel @Inject constructor(
    application: Application,
    private val repository: RecipeRepository
) : AndroidViewModel(application) {

    var state by mutableStateOf(FavoriteRecipesState())
        private set

    init {
        loadFavoriteRecipes()
    }

    private fun loadFavoriteRecipes() {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: "default_user"
        viewModelScope.launch {
                       repository.getFavorites(uid).collectLatest { list ->
                state = state.copy(favorites = list, isLoading = false)
            }
        }
    }
}