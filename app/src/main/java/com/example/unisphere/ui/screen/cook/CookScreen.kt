package com.example.unisphere.ui.screen.cook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.unisphere.ui.composables.UniSphereEmptyState
import com.example.unisphere.ui.composables.UniSphereTextField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

// --- MODELLI DATI ---
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

// --- VIEWMODEL ---
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
@Composable
fun CookScreen(
    navController: NavHostController,
    viewModel: CookViewModel = hiltViewModel()
) {
    val state = viewModel.state

    Scaffold(
        topBar = { AppBar(title = "UniChef", navController = navController) },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {

            // Barra di ricerca centralizzata UniSphereTextField
            UniSphereTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onAction(CookAction.OnSearchQueryChanged(it)) },
                label = "Cerca ingredienti o ricette...",
                leadingIcon = Icons.Default.Search,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                trailingIcon = {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Bottone Ricette Preferite Widget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { navController.navigate(NavigationRoute.FavoriteRecipesScreen) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFF7A7A)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ricette Preferite", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Sfoglia i tuoi piatti salvati offline", fontSize = 12.sp, color = Color.Gray)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }

            // --- CONTROLLO STATO LISTA RICETTE ---
            if (state.recipes.isEmpty() && !state.isLoading) {
                // REFACTOR COMPLETATO: Sostituito il Box/Column manuale con UniSphereEmptyState globale
                UniSphereEmptyState(
                    icon = Icons.Default.Restaurant,
                    title = "Esplora Nuovi Sapori",
                    description = "Scrivi un ingrediente o un piatto sopra per visualizzare subito la preparazione passo-passo.",
                    modifier = Modifier.weight(1f) // Occupa lo spazio rimanente centrando il contenuto
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column {
            AsyncImage(
                model = recipe.image,
                contentDescription = recipe.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = recipe.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${recipe.readyInMinutes} min",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "•  ${recipe.servings} porzioni",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}