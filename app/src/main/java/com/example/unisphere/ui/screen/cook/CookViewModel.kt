package com.example.unisphere.ui.screen.cook

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Recipe(
    val id: Int,
    val title: String,
    val image: String,
    val readyInMinutes: Int = 0,
    val servings: Int = 0
)

@Serializable
data class RecipeResponse(
    val results: List<Recipe>
)

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

class CookViewModel(application: Application) : AndroidViewModel(application) {
    var state by mutableStateOf(CookState())
        private set

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private val apiKey = "e09429ff9b1c4c3ca2e6e4318890b313"
    private var searchJob: Job? = null

    init {
        // Caricamento iniziale di alcune ricette popolari
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
                val response: RecipeResponse = client.get("https://api.spoonacular.com/recipes/complexSearch") {
                    parameter("apiKey", apiKey)
                    parameter("query", query)
                    parameter("number", 20)
                    parameter("addRecipeInformation", true)
                }.body()
                
                state = state.copy(recipes = response.results, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = "Errore durante il caricamento delle ricette")
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
