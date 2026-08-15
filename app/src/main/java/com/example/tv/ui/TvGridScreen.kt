package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.model.MovieEntity
import com.example.model.SeriesEntity
import com.example.model.XtreamCategoryEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvPanel
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvFocusable

private enum class TvSortOption(val label: String) {
    DEFAULT("Default"),
    TOP_RATED("Top Rated"),
    NAME_AZ("Name A-Z")
}

/** شبكة عامة تُستخدم لكل من Movies وSeries — تصنيفات على اليسار + فرز/Reset أعلى شبكة الملصقات. */
@Composable
private fun <T> TvGridScreen(
    items: List<T>,
    categories: List<XtreamCategoryEntity>,
    onLoadCategoryStreams: (String) -> Unit,
    posterOf: (T) -> String,
    titleOf: (T) -> String,
    ratingOf: (T) -> String,
    onItemClick: (T) -> Unit
) {
    var selectedCategoryId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var sortOption by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(TvSortOption.DEFAULT) }

    val sortedItems = androidx.compose.runtime.remember(items, sortOption) {
        when (sortOption) {
            TvSortOption.DEFAULT -> items
            TvSortOption.TOP_RATED -> items.sortedByDescending { ratingOf(it).toDoubleOrNull() ?: 0.0 }
            TvSortOption.NAME_AZ -> items.sortedBy { titleOf(it).lowercase() }
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(TvBg)) {
        LazyColumn(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(TvPanel)
                .padding(vertical = 16.dp, horizontal = 10.dp)
        ) {
            items(categories) { cat ->
                val active = selectedCategoryId == cat.id
                TvFocusable(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        selectedCategoryId = cat.id
                        onLoadCategoryStreams(cat.id)
                    }
                ) { _ ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (active) TvRed else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Text(
                            cat.name,
                            color = if (active) TvTextWhite else TvTextGray,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
            ) {
                TvSortOption.values().forEach { option ->
                    val active = sortOption == option
                    TvFocusable(
                        onClick = { sortOption = option },
                        shape = RoundedCornerShape(14.dp)
                    ) { _ ->
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .background(if (active) TvRed else TvPanel, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(option.label, color = if (active) TvTextWhite else TvTextGray, fontSize = 12.sp)
                        }
                    }
                }
                if (selectedCategoryId != null || sortOption != TvSortOption.DEFAULT) {
                    TvFocusable(
                        onClick = {
                            selectedCategoryId = null
                            sortOption = TvSortOption.DEFAULT
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) { _ ->
                        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
                            Text("✕ Reset Filters", color = TvTextGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (sortedItems.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No content in this category", color = TvTextGray, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedItems) { it2 ->
                        TvPosterCard(title = titleOf(it2), imageUrl = posterOf(it2)) { onItemClick(it2) }
                    }
                }
            }
        }
    }
}

@Composable
fun TvMoviesScreen(
    movies: List<MovieEntity>,
    categories: List<XtreamCategoryEntity>,
    onLoadCategoryStreams: (String) -> Unit,
    onMovieClick: (MovieEntity) -> Unit
) {
    TvGridScreen(
        items = movies,
        categories = categories,
        onLoadCategoryStreams = onLoadCategoryStreams,
        posterOf = { it.posterUrl },
        titleOf = { it.title },
        ratingOf = { it.rating },
        onItemClick = onMovieClick
    )
}

@Composable
fun TvSeriesScreen(
    seriesList: List<SeriesEntity>,
    categories: List<XtreamCategoryEntity>,
    onLoadCategoryStreams: (String) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit
) {
    TvGridScreen(
        items = seriesList,
        categories = categories,
        onLoadCategoryStreams = onLoadCategoryStreams,
        posterOf = { it.posterUrl },
        titleOf = { it.title },
        ratingOf = { it.rating },
        onItemClick = onSeriesClick
    )
}

@Composable
fun TvFavoritesScreen(
    favMovies: List<MovieEntity>,
    favSeries: List<SeriesEntity>,
    favChannels: List<com.example.model.ChannelEntity>,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onPlayChannel: (com.example.model.ChannelEntity) -> Unit
) {
    if (favMovies.isEmpty() && favSeries.isEmpty() && favChannels.isEmpty()) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize().background(TvBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No favorites yet", color = TvTextGray, fontSize = 14.sp)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().background(TvBg)
    ) {
        items(favChannels) { ch -> TvPosterCard(title = ch.name, imageUrl = ch.logoUrl, isChannel = true) { onPlayChannel(ch) } }
        items(favMovies) { m -> TvPosterCard(title = m.title, imageUrl = m.posterUrl) { onMovieClick(m) } }
        items(favSeries) { s -> TvPosterCard(title = s.title, imageUrl = s.posterUrl) { onSeriesClick(s) } }
    }
}
