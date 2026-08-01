package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Episode
import com.example.model.MovieEntity
import com.example.model.SeriesEntity
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.RatingGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailSheet(
    movie: MovieEntity?,
    series: SeriesEntity?,
    episodes: List<Episode>,
    onDismiss: () -> Unit,
    onPlayMovie: (MovieEntity) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onToggleFavoriteMovie: (MovieEntity) -> Unit,
    onToggleFavoriteSeries: (SeriesEntity) -> Unit,
    onDownloadMovie: ((MovieEntity) -> Unit)? = null
) {
    val availableSeasons = remember(episodes) {
        episodes.map { it.seasonNum }.distinct().sorted().ifEmpty { listOf(1) }
    }
    var selectedSeason by remember { mutableIntStateOf(availableSeasons.firstOrNull() ?: 1) }

    val filteredEpisodes = remember(episodes, selectedSeason) {
        val matches = episodes.filter { it.seasonNum == selectedSeason }
        if (matches.isNotEmpty()) matches else episodes
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141414),
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.Gray)
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                ) {
                    val backdropUrl = movie?.backdropUrl?.ifEmpty { movie.posterUrl }
                        ?: series?.backdropUrl?.ifEmpty { series.posterUrl } ?: ""

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(backdropUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = movie?.title ?: series?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color(0xFF141414).copy(alpha = 0.8f),
                                        Color(0xFF141414)
                                    )
                                )
                            )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(0.6f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = NetflixRed,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (movie != null) "فيلم" else "مسلسل VIP",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "★ ${movie?.rating ?: series?.rating ?: "8.5"}",
                                color = RatingGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = movie?.releaseYear ?: series?.releaseYear ?: "2024",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = movie?.title ?: series?.title ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    val plotText = movie?.plot ?: series?.plot ?: ""
                    if (plotText.isNotBlank()) {
                        Text(
                            text = plotText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Play & Add List Buttons for Movies
                    if (movie != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onPlayMovie(movie) },
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تشغيل", fontWeight = FontWeight.Bold)
                            }

                            if (onDownloadMovie != null) {
                                OutlinedButton(
                                    onClick = { onDownloadMovie(movie) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تحميل")
                                }
                            }

                            OutlinedButton(
                                onClick = { onToggleFavoriteMovie(movie) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(
                                    imageVector = if (movie.isFavorite) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (movie.isFavorite) "المفضلة" else "المفضلة")
                            }
                        }
                    }

                    // Series Header & Season Selector
                    if (series != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اختيار الموسم والحلقة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            TextButton(onClick = { onToggleFavoriteSeries(series) }) {
                                Icon(
                                    imageVector = if (series.isFavorite) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = NetflixRed
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (series.isFavorite) "في المفضلة" else "إضافة للمفضلة",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Season Filter Chips
                        if (availableSeasons.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (season in availableSeasons) {
                                    val isSelected = season == selectedSeason
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedSeason = season },
                                        label = {
                                            Text(
                                                text = "الموسم $season",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else Color.LightGray
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NetflixRed,
                                            containerColor = Color(0xFF262626)
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Episodes List for Selected Season
            if (series != null && filteredEpisodes.isNotEmpty()) {
                items(filteredEpisodes, key = { it.id }) { episode ->
                    Card(
                        onClick = { onPlayEpisode(episode) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(NetflixRed)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل",
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = episode.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${episode.duration} • اضغط للتشغيل الفوري",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
