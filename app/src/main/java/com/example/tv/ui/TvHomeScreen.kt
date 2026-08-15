package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.model.ChannelEntity
import com.example.model.MovieEntity
import com.example.model.SeriesEntity
import com.example.model.WatchHistoryEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvFocusable
import com.example.tv.ui.components.TvOutlineButton
import com.example.tv.ui.components.TvPrimaryButton
import kotlinx.coroutines.delay

private data class HeroItem(
    val title: String,
    val meta: String,
    val plot: String,
    val backdrop: String,
    val movie: MovieEntity? = null,
    val series: SeriesEntity? = null
)

/**
 * الشاشة الرئيسية — هيرو بانر دوّار (نفس .hero-banner) + صفوف أفقية للمحتوى (نفس .content-row).
 */
@Composable
fun TvHomeScreen(
    channels: List<ChannelEntity>,
    movies: List<MovieEntity>,
    series: List<SeriesEntity>,
    watchHistory: List<WatchHistoryEntity>,
    onPlayChannel: (ChannelEntity) -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onResumeHistory: (WatchHistoryEntity) -> Unit,
    onPlayMovie: (MovieEntity) -> Unit = onMovieClick
) {
    val heroSlides = remember(movies, series) {
        val fromMovies = movies.take(15).shuffled().take(3)
            .map { HeroItem(it.title, "${it.releaseYear} · ${it.rating}★", it.plot, it.backdropUrl.ifBlank { it.posterUrl }, movie = it) }
        val fromSeries = series.take(10).shuffled().take(2)
            .map { HeroItem(it.title, "${it.releaseYear} · ${it.rating}★", it.plot, it.backdropUrl.ifBlank { it.posterUrl }, series = it) }
        (fromMovies + fromSeries).ifEmpty { listOf(HeroItem("FXTV PLAYER", "", "Connect a playlist to get started.", "")) }
    }
    var heroIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(heroSlides.size) {
        while (heroSlides.size > 1) {
            delay(6000)
            heroIndex = (heroIndex + 1) % heroSlides.size
        }
    }

    // صفوف الأفلام والمسلسلات مُجمَّعة حسب التصنيف الحقيقي (وليس صفاً مسطحاً واحداً) — نفس أسلوب
    // سطح المكتب في عرض الرئيسية؛ نعرض عدداً محدوداً من التصنيفات أولاً مع زر "تحميل المزيد"
    val movieCategoryRows = remember(movies) {
        movies.groupBy { it.category.ifBlank { "Movies" } }.toList()
    }
    val seriesCategoryRows = remember(series) {
        series.groupBy { it.category.ifBlank { "Series" } }.toList()
    }
    var visibleCategoryCount by remember { mutableStateOf(4) }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(TvBg)
    ) {
        item {
            TvHeroBanner(
                item = heroSlides[heroIndex],
                dotsCount = heroSlides.size,
                activeDot = heroIndex,
                onPlay = {
                    val slide = heroSlides[heroIndex]
                    slide.movie?.let { onPlayMovie(it) } ?: slide.series?.let { onSeriesClick(it) }
                },
                onMoreInfo = {
                    val slide = heroSlides[heroIndex]
                    slide.movie?.let { onMovieClick(it) } ?: slide.series?.let { onSeriesClick(it) }
                }
            )
        }

        if (watchHistory.isNotEmpty()) {
            item {
                TvContentRow(title = "Continue Watching") {
                    items(watchHistory.take(20)) { h ->
                        TvHistoryCard(h) { onResumeHistory(h) }
                    }
                }
            }
        }

        // القنوات المباشرة عمداً لا تظهر في الصفحة الرئيسية — نفس سلوك سطح المكتب تماماً؛
        // تبقى قابلة للوصول فقط من تبويب "Live TV" نفسه أو من نتائج البحث الشامل
        val allCategoryRows = movieCategoryRows.map { it to true } + seriesCategoryRows.map { it to false }
        allCategoryRows.take(visibleCategoryCount).forEach { (entry, isMovie) ->
            val (categoryName, categoryItems) = entry
            item {
                TvContentRow(title = categoryName) {
                    items(categoryItems.take(30)) { it2 ->
                        if (isMovie) {
                            @Suppress("UNCHECKED_CAST")
                            TvPosterCard(title = (it2 as MovieEntity).title, imageUrl = it2.posterUrl) { onMovieClick(it2) }
                        } else {
                            @Suppress("UNCHECKED_CAST")
                            TvPosterCard(title = (it2 as SeriesEntity).title, imageUrl = it2.posterUrl) { onSeriesClick(it2) }
                        }
                    }
                }
            }
        }

        if (visibleCategoryCount < allCategoryRows.size) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 10.dp, bottom = 30.dp)) {
                    TvOutlineButton(text = "Load More Categories", onClick = { visibleCategoryCount += 4 })
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun TvHeroBanner(item: HeroItem, dotsCount: Int, activeDot: Int, onPlay: () -> Unit, onMoreInfo: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        if (item.backdrop.isNotBlank()) {
            AsyncImage(
                model = item.backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(TvCard))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, TvBg.copy(alpha = 0.55f), TvBg)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(TvBg.copy(alpha = 0.85f), Color.Transparent),
                        endX = 900f
                    )
                )
        )

        if (dotsCount > 1) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(dotsCount) { i ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (i == activeDot) 22.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i == activeDot) TvRed else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 40.dp, bottom = 32.dp, end = 200.dp)
        ) {
            Text(item.title, color = TvTextWhite, fontWeight = FontWeight.Black, fontSize = 34.sp)
            if (item.meta.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(item.meta, color = TvTextGray, fontSize = 13.sp)
            }
            if (item.plot.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(item.plot, color = Color(0xFFDDDDDD), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvPrimaryButton(text = "▶ Watch Now", onClick = onPlay)
                TvOutlineButton(text = "ⓘ More Info", onClick = onMoreInfo)
            }
        }
    }
}

@Composable
private fun TvContentRow(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 22.dp)) {
        Text(
            title,
            color = TvTextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 40.dp, bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 40.dp)
        ) {
            content()
        }
    }
}

@Composable
fun TvPosterCard(title: String, imageUrl: String, isChannel: Boolean = false, onClick: () -> Unit) {
    TvFocusable(
        modifier = Modifier.width(150.dp),
        onClick = onClick
    ) { _ ->
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isChannel) 84.dp else 225.dp)
                    .background(TvCard)
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = if (isChannel) ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isChannel) 10.dp else 0.dp)
                    )
                }
            }
            Text(
                title,
                color = Color(0xFFEEEEEE),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun TvHistoryCard(history: WatchHistoryEntity, onClick: () -> Unit) {
    val progress = if (history.totalMs > 0) (history.progressMs.toFloat() / history.totalMs).coerceIn(0f, 1f) else 0f
    TvFocusable(modifier = Modifier.width(200.dp), onClick = onClick) { _ ->
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(TvCard)
            ) {
                if (history.posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = history.posterUrl,
                        contentDescription = history.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(TvRed)
                    )
                }
            }
            Text(
                history.title,
                color = Color(0xFFEEEEEE),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
