package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ChannelEntity
import com.example.model.MovieEntity
import com.example.model.SeriesEntity
import com.example.ui.components.ChannelHorizontalList
import com.example.ui.components.MovieHorizontalList
import com.example.ui.components.SeriesHorizontalList
import com.example.ui.theme.NetflixRed
import com.example.ui.viewmodel.PlayingMedia

@Composable
fun FavoritesScreen(
    favChannels: List<ChannelEntity>,
    favMovies: List<MovieEntity>,
    favSeries: List<SeriesEntity>,
    allChannels: List<ChannelEntity>,
    onPlayChannel: (PlayingMedia) -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onToggleChannelFav: (ChannelEntity) -> Unit,
    onToggleMovieFav: (MovieEntity) -> Unit,
    onToggleSeriesFav: (SeriesEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmpty = favChannels.isEmpty() && favMovies.isEmpty() && favSeries.isEmpty()

    if (isEmpty) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = "Your favorites list is currently empty\nAdd favorite channels or movies by tapping the heart icon ❤️",
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.Gray,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (favChannels.isNotEmpty()) {
                item {
                    ChannelHorizontalList(
                        title = "Favorite Channels ❤️",
                        channels = favChannels,
                        onChannelClick = { ch ->
                            onPlayChannel(
                                PlayingMedia(
                                    id = ch.id,
                                    title = ch.name,
                                    streamUrl = ch.streamUrl,
                                    posterUrl = ch.logoUrl,
                                    category = ch.category,
                                    type = "LIVE",
                                    channelList = allChannels
                                )
                            )
                        },
                        onFavoriteToggle = onToggleChannelFav
                    )
                }
            }

            if (favMovies.isNotEmpty()) {
                item {
                    MovieHorizontalList(
                        title = "Favorite Movies 🎬",
                        movies = favMovies,
                        onMovieClick = onMovieClick,
                        onFavoriteToggle = onToggleMovieFav
                    )
                }
            }

            if (favSeries.isNotEmpty()) {
                item {
                    SeriesHorizontalList(
                        title = "Favorite Series 📺",
                        seriesList = favSeries,
                        onSeriesClick = onSeriesClick,
                        onFavoriteToggle = onToggleSeriesFav
                    )
                }
            }
        }
    }
}
