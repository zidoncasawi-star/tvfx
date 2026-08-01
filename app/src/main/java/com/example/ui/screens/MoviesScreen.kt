package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.MovieEntity
import com.example.ui.components.CategoryFilterChips
import com.example.ui.components.MovieCard
import com.example.ui.theme.NetflixRed

@Composable
fun MoviesScreen(
    movies: List<MovieEntity>,
    xtreamCategories: List<com.example.model.XtreamCategoryEntity>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onToggleFavorite: (MovieEntity) -> Unit,
    onLoadCategoryStreams: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val categories = remember(movies, xtreamCategories) {
        val list = mutableListOf("الكل")
        if (xtreamCategories.isNotEmpty()) {
            list.addAll(xtreamCategories.map { it.id })
        } else {
            list.addAll(movies.map { it.category }.distinct())
        }
        list.distinct()
    }

    val categoryNames = remember(movies, xtreamCategories) {
        val map = mutableMapOf<String, String>()
        map["الكل"] = "الكل"
        xtreamCategories.forEach { map[it.id] = it.name }
        movies.forEach { if (!map.containsKey(it.category)) map[it.category] = it.category }
        map
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "الكل" && xtreamCategories.any { it.id == selectedCategory }) {
            val hasMovies = movies.any { it.category == selectedCategory }
            if (!hasMovies) {
                onLoadCategoryStreams(selectedCategory)
            }
        }
    }

    val filteredMovies = movies.filter {
        (selectedCategory == "الكل" || it.category == selectedCategory) &&
                (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("بحث في مكتبة الأفلام...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetflixRed) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NetflixRed,
                unfocusedBorderColor = Color(0xFF333333),
                focusedContainerColor = Color(0xFF1B1B1B),
                unfocusedContainerColor = Color(0xFF1B1B1B)
            )
        )

        CategoryFilterChips(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelect = onCategorySelect,
            categoryNames = categoryNames
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredMovies, key = { it.id }) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieClick(movie) },
                    onFavoriteToggle = { onToggleFavorite(movie) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
