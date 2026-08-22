package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.ChannelEntity
import com.example.model.MovieEntity
import com.example.model.SeriesEntity
import com.example.model.WatchHistoryEntity
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.RatingGold

@Composable
fun getAdaptiveMovieCardSize(): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    return when {
        screenWidth >= 960 -> Pair(190.dp, 280.dp) // TV / Extra Large screen
        screenWidth >= 600 -> Pair(160.dp, 240.dp) // Tablet / Medium screen
        else -> Pair(135.dp, 200.dp) // Phone / Compact screen
    }
}

@Composable
fun getAdaptiveChannelCardSize(): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    return when {
        screenWidth >= 960 -> Pair(240.dp, 160.dp) // TV / Extra Large screen
        screenWidth >= 600 -> Pair(200.dp, 135.dp) // Tablet / Medium screen
        else -> Pair(160.dp, 115.dp) // Phone / Compact screen
    }
}

@Composable
fun getAdaptiveContentPadding(): PaddingValues {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val padding = when {
        screenWidth >= 960 -> 32.dp
        screenWidth >= 600 -> 24.dp
        else -> 16.dp
    }
    return PaddingValues(horizontal = padding, vertical = 8.dp)
}

@Composable
fun getAdaptiveCardSpacing(): androidx.compose.ui.unit.Dp {
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    return when {
        screenWidth >= 960 -> 16.dp
        screenWidth >= 600 -> 12.dp
        else -> 10.dp
    }
}

// صورة ملصق مع عنصر نائب واضح (أيقونة + خلفية) بدل مربع أسود فارغ عند فشل تحميل الصورة أو عدم توفرها من السيرفر
@Composable
fun PosterImage(url: String, contentDescription: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF23222E)), contentAlignment = Alignment.Center) {
        if (url.isBlank()) {
            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(36.dp)
            )
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    )
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun MediaRowTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun MovieCard(
    movie: MovieEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardSize = getAdaptiveMovieCardSize()
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .width(cardSize.first)
            .height(cardSize.second)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1.0f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
        ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.DarkCardBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PosterImage(
                url = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize()
            )

            // Rating Badge on top left
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 12.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "★", color = RatingGold, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = movie.rating, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Favorite Button on top right
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (movie.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (movie.isFavorite) NetflixRed else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom title overlay gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun SeriesCard(
    series: SeriesEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardSize = getAdaptiveMovieCardSize()
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .width(cardSize.first)
            .height(cardSize.second)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1.0f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
        ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.DarkCardBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PosterImage(
                url = series.posterUrl,
                contentDescription = series.title,
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                color = NetflixRed,
                shape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 12.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "Series",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (series.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (series.isFavorite) NetflixRed else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            Text(
                text = series.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun ChannelCard(
    channel: ChannelEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardSize = getAdaptiveChannelCardSize()
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .width(cardSize.first)
            .height(cardSize.second)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1.0f)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
        ),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.DarkCardBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PosterImage(
                url = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Live indicator badge
            Surface(
                color = LiveRed,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (channel.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (channel.isFavorite) RatingGold else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = channel.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (channel.epgNow.isNotBlank()) {
                    Text(
                        text = channel.epgNow,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    history: WatchHistoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardSize = getAdaptiveChannelCardSize()
    var isFocused by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .width(cardSize.first)
            .height(cardSize.second)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(if (isFocused) 1.05f else 1.0f)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(history.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = history.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NetflixRed)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Continue Playing",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Progress bar
            if (history.totalMs > 0) {
                val progress = (history.progressMs.toFloat() / history.totalMs.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = NetflixRed,
                    trackColor = Color.Gray.copy(alpha = 0.5f)
                )
            }

            Text(
                text = history.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
fun MovieHorizontalList(
    title: String,
    movies: List<MovieEntity>,
    onMovieClick: (MovieEntity) -> Unit,
    onFavoriteToggle: (MovieEntity) -> Unit
) {
    if (movies.isEmpty()) return
    Column {
        MediaRowTitle(title)
        LazyRow(
            contentPadding = getAdaptiveContentPadding(),
            horizontalArrangement = Arrangement.spacedBy(getAdaptiveCardSpacing())
        ) {
            items(movies, key = { it.id }) { item ->
                MovieCard(
                    movie = item,
                    onClick = { onMovieClick(item) },
                    onFavoriteToggle = { onFavoriteToggle(item) }
                )
            }
        }
    }
}

@Composable
fun SeriesHorizontalList(
    title: String,
    seriesList: List<SeriesEntity>,
    onSeriesClick: (SeriesEntity) -> Unit,
    onFavoriteToggle: (SeriesEntity) -> Unit
) {
    if (seriesList.isEmpty()) return
    Column {
        MediaRowTitle(title)
        LazyRow(
            contentPadding = getAdaptiveContentPadding(),
            horizontalArrangement = Arrangement.spacedBy(getAdaptiveCardSpacing())
        ) {
            items(seriesList, key = { it.id }) { item ->
                SeriesCard(
                    series = item,
                    onClick = { onSeriesClick(item) },
                    onFavoriteToggle = { onFavoriteToggle(item) }
                )
            }
        }
    }
}

@Composable
fun ChannelHorizontalList(
    title: String,
    channels: List<ChannelEntity>,
    onChannelClick: (ChannelEntity) -> Unit,
    onFavoriteToggle: (ChannelEntity) -> Unit
) {
    if (channels.isEmpty()) return
    Column {
        MediaRowTitle(title)
        LazyRow(
            contentPadding = getAdaptiveContentPadding(),
            horizontalArrangement = Arrangement.spacedBy(getAdaptiveCardSpacing())
        ) {
            items(channels, key = { it.id }) { item ->
                ChannelCard(
                    channel = item,
                    onClick = { onChannelClick(item) },
                    onFavoriteToggle = { onFavoriteToggle(item) }
                )
            }
        }
    }
}

@Composable
fun HistoryHorizontalList(
    title: String,
    historyList: List<WatchHistoryEntity>,
    onHistoryClick: (WatchHistoryEntity) -> Unit
) {
    if (historyList.isEmpty()) return
    Column {
        MediaRowTitle(title)
        LazyRow(
            contentPadding = getAdaptiveContentPadding(),
            horizontalArrangement = Arrangement.spacedBy(getAdaptiveCardSpacing())
        ) {
            items(historyList, key = { it.itemId }) { item ->
                HistoryCard(
                    history = item,
                    onClick = { onHistoryClick(item) }
                )
            }
        }
    }
}
