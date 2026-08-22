package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.example.model.SeriesEntity
import com.example.ui.components.DropdownFilterBar
import com.example.ui.components.SeriesCard
import com.example.ui.components.SortOption
import com.example.ui.theme.NetflixRed

@Composable
fun SeriesScreen(
    seriesList: List<SeriesEntity>,
    xtreamCategories: List<com.example.model.XtreamCategoryEntity>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onToggleFavorite: (SeriesEntity) -> Unit,
    onLoadCategoryStreams: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SortOption.DEFAULT) }

    val categories = remember(seriesList, xtreamCategories) {
        val list = mutableListOf("All")
        if (xtreamCategories.isNotEmpty()) {
            list.addAll(xtreamCategories.map { it.id })
        } else {
            list.addAll(seriesList.map { it.category }.distinct())
        }
        list.distinct()
    }

    val categoryNames = remember(seriesList, xtreamCategories) {
        val map = mutableMapOf<String, String>()
        map["All"] = "All"
        xtreamCategories.forEach { map[it.id] = it.name }
        seriesList.forEach { if (!map.containsKey(it.category)) map[it.category] = it.category }
        map
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "All" && xtreamCategories.any { it.id == selectedCategory }) {
            val hasSeries = seriesList.any { it.category == selectedCategory }
            if (!hasSeries) {
                onLoadCategoryStreams(selectedCategory)
            }
        }
    }

    // زر "تحميل المزيد" بدل الزحف التلقائي: يجلب أول تصنيف غير محمَّل بعد عند الضغط فقط
    var isLoadingMore by remember { mutableStateOf(false) }
    LaunchedEffect(seriesList) { isLoadingMore = false }
    val nextUnloadedCategory = remember(seriesList, xtreamCategories) {
        xtreamCategories.firstOrNull { cat -> seriesList.none { it.category == cat.id } }
    }

    val filteredSeries = remember(seriesList, selectedCategory, searchQuery, sortOption) {
        val filtered = seriesList.filter {
            (selectedCategory == "All" || it.category == selectedCategory) &&
                    (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
        }
        when (sortOption) {
            SortOption.DEFAULT -> filtered
            SortOption.RATING -> filtered.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            SortOption.YEAR -> filtered.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
            SortOption.NAME -> filtered.sortedBy { it.title }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search series...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetflixRed) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
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

        DropdownFilterBar(
            categories = categories,
            selectedCategory = selectedCategory,
            categoryNames = categoryNames,
            onCategorySelect = onCategorySelect,
            sortOption = sortOption,
            onSortSelect = { sortOption = it }
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredSeries, key = { it.id }) { seriesItem ->
                SeriesCard(
                    series = seriesItem,
                    onClick = { onSeriesClick(seriesItem) },
                    onFavoriteToggle = { onToggleFavorite(seriesItem) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (selectedCategory == "All" && searchQuery.isEmpty() && nextUnloadedCategory != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(28.dp))
                        } else {
                            OutlinedButton(onClick = {
                                isLoadingMore = true
                                onLoadCategoryStreams(nextUnloadedCategory.id)
                            }) {
                                Text("Load More Categories")
                            }
                        }
                    }
                }
            }
        }
    }
}
