package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.model.Episode
import com.example.model.MovieEntity
import com.example.model.SeriesEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvFocusable
import com.example.tv.ui.components.TvOutlineButton
import com.example.tv.ui.components.TvPrimaryButton

/** شاشة التفاصيل — نفس بنية #detailOverlay: خلفية باهتة، معلومات، وحلقات لكل موسم للمسلسلات. */
@Composable
fun TvDetailScreen(
    movie: MovieEntity?,
    series: SeriesEntity?,
    episodes: List<Episode>,
    isFavorite: Boolean,
    onClose: () -> Unit,
    onPlayMovie: (MovieEntity) -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val title = movie?.title ?: series?.title ?: ""
    val backdrop = movie?.backdropUrl?.ifBlank { movie.posterUrl } ?: series?.backdropUrl?.ifBlank { series.posterUrl } ?: ""
    val plot = movie?.plot ?: series?.plot ?: ""
    val meta = movie?.let { "${it.releaseYear} · ${it.duration} · ${it.rating}★" }
        ?: series?.let { "${it.releaseYear} · ${it.rating}★" } ?: ""
    val category = movie?.category ?: series?.category ?: ""

    val seasons = remember(episodes) { episodes.map { it.seasonNum }.distinct().sorted() }
    var selectedSeason by remember(seasons) { mutableStateOf(seasons.firstOrNull() ?: 1) }

    Box(modifier = Modifier.fillMaxSize().background(TvBg)) {
        if (backdrop.isNotBlank()) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(500.dp).align(Alignment.TopCenter)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, TvBg.copy(alpha = 0.4f), TvBg)))
        )

        TvOutlineButton(
            text = "✕ Close",
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 220.dp, start = 40.dp, end = 40.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TvTextWhite, fontWeight = FontWeight.Black, fontSize = 30.sp)
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(meta, color = TvTextGray, fontSize = 13.sp)
                }
                if (category.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(category, color = Color(0xFFDDDDDD), fontSize = 12.sp)
                    }
                }
                if (plot.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(plot, color = Color(0xFFDDDDDD), fontSize = 14.sp, lineHeight = 20.sp)
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (movie != null) {
                        TvPrimaryButton(text = "▶ Play", onClick = { onPlayMovie(movie) })
                    }
                    TvOutlineButton(
                        text = if (isFavorite) "♥ In Favorites" else "♡ Add to Favorites",
                        onClick = onToggleFavorite
                    )
                }
            }

            if (series != null) {
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .background(TvCard.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    if (seasons.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(seasons) { s ->
                                val active = s == selectedSeason
                                TvFocusable(
                                    shape = RoundedCornerShape(18.dp),
                                    onClick = { selectedSeason = s }
                                ) { _ ->
                                    Box(
                                        modifier = Modifier
                                            .background(if (active) TvRed else TvCard, RoundedCornerShape(18.dp))
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Season $s", color = if (active) TvTextWhite else TvTextGray, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(episodes.filter { it.seasonNum == selectedSeason }) { ep ->
                            TvFocusable(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                onClick = { onPlayEpisode(ep) }
                            ) { _ ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${ep.episodeNum}",
                                        color = TvTextGray,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.width(30.dp)
                                    )
                                    Text(
                                        ep.title,
                                        color = Color(0xFFEEEEEE),
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
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
