package com.example.unisphere.ui.screen.cook

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unisphere.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject


// Modello dati per l'oggetto Ricetta Spoonacular
@Serializable
data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int = 0,
    val servings: Int = 0
)

// Wrapper per mappare l'array JSON di risposta
@Serializable
data class RecipeResponse(
    val results: List<Recipe>
)

// Stato della schermata Cook osservato dalla UI
data class CookState(
    val recipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface CookAction {
    data class OnSearchQueryChanged(val query: String) : CookAction
    data object OnRetryClicked : CookAction
}

@HiltViewModel
class CookViewModel @Inject constructor(
    application: Application,
    private val repository: RecipeRepository
) : AndroidViewModel(application) {

    var state by mutableStateOf(CookState())
        private set

    private var searchJob: Job? = null

    init {
        fetchRecipes()
    }

    fun onAction(action: CookAction) {
        when (action) {
            is CookAction.OnSearchQueryChanged -> {
                state = state.copy(searchQuery = action.query)
                debouncedSearch(action.query)
            }
            CookAction.OnRetryClicked -> {
                fetchRecipes(state.searchQuery)
            }
        }
    }

    private fun debouncedSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            fetchRecipes(query)
        }
    }

    private fun fetchRecipes(query: String = "") {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val response = repository.searchRecipes(query)
                state = state.copy(recipes = response.results, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = "Errore durante il caricamento delle ricette")
                e.printStackTrace()
            }
        }
    }
}