package com.example.unisphere.ui.screen.cook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.unisphere.repository.RecipeRepository
import com.example.unisphere.ui.composables.AppBar
import com.example.unisphere.ui.composables.BottomNavigationBar
import com.example.unisphere.ui.composables.NavigationRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

// --- MODELLI DATI PER LA RICERCA ---
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

// --- STATO E AZIONI ---
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

// --- VIEWMODEL AGGIORNATO CON REPOSITORY ---
@HiltViewModel
class CookViewModel @Inject constructor(
    application: android.app.Application,
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

// --- INTERFACCIA GRAFICA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookScreen(
    navController: NavHostController,
    viewModel: CookViewModel = hiltViewModel() // Sostituito con hiltViewModel() obbligatorio
) {
    val state = viewModel.state

    Scaffold(
        topBar = { AppBar(title = "UniChef", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onAction(CookAction.OnSearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                placeholder = { Text("Cerca una ricetta...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                trailingIcon = {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            )

            if (state.recipes.isEmpty() && !state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Cerca qualcosa di buono da cucinare!", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.recipes) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = {
                                navController.navigate(NavigationRoute.RecipeDetailScreen(recipe.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            AsyncImage(
                model = recipe.image,
                contentDescription = recipe.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Pronta in ${recipe.readyInMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "• ${recipe.servings} porzioni",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}