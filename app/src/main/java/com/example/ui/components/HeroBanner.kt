package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MovieEntity
import com.example.ui.theme.NetflixRed
import com.example.ui.theme.RatingGold

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import kotlinx.coroutines.delay

@Composable
fun AutoCyclingHeroBanner(
    featuredList: List<MovieEntity>,
    onPlayClick: (MovieEntity) -> Unit,
    onDetailClick: (MovieEntity) -> Unit,
    onToggleFavorite: (MovieEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (featuredList.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(featuredList.size) {
        if (featuredList.size > 1) {
            while (true) {
                delay(5000L)
                currentIndex = (currentIndex + 1) % featuredList.size
            }
        }
    }

    val featuredMovie = featuredList.getOrNull(currentIndex) ?: featuredList.first()

    val isWideScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
    val bannerHeight = if (isWideScreen) 480.dp else 360.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .padding(horizontal = if (isWideScreen) 32.dp else 16.dp, vertical = if (isWideScreen) 16.dp else 8.dp)
            .clickable { onDetailClick(featuredMovie) },
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.DarkCardBg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated Backdrop Image Crossfade
            Crossfade(targetState = featuredMovie, label = "HeroBannerCrossfade") { currentMovie ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentMovie.backdropUrl.ifEmpty { currentMovie.posterUrl })
                        .crossfade(true)
                        .build(),
                    contentDescription = currentMovie.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gradient overlay (Dark from bottom and top)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black
                            )
                        )
                    )
            )

            // Page Indicator Dots at Top End
            if (featuredList.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    featuredList.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (idx == currentIndex) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (idx == currentIndex) NetflixRed else Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // Content Info on bottom of banner
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp)
            ) {
                // Badges (Sleek pill badges)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = NetflixRed,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Trending 🔥",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = featuredMovie.category,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "★ ${featuredMovie.rating}",
                            color = RatingGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = featuredMovie.releaseYear,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Movie Title
                Text(
                    text = featuredMovie.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (featuredMovie.plot.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = featuredMovie.plot,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons ('Play Now' / 'شاهد الآن' + List + Details)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onPlayClick(featuredMovie) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .then(if (isWideScreen) Modifier.width(220.dp) else Modifier.weight(1f))
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Watch Now",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Watch Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(featuredMovie) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (featuredMovie.isFavorite) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = "My List",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDetailClick(featuredMovie) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
