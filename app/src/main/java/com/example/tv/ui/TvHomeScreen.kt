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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.example.model.XtreamCategoryEntity
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
    movieCategories: List<XtreamCategoryEntity>,
    seriesCategories: List<XtreamCategoryEntity>,
    watchHistory: List<WatchHistoryEntity>,
    onLoadCategoryStreams: (categoryId: String, type: String) -> Unit,
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

    // نفس نظام سطح المكتب تماماً: التصنيفات لا تُحمَّل كلها دفعة واحدة (قد يُجمِّد التطبيق)، بل
    // تصنيف واحد في كل مرة، أول تصنيف أفلام + أول تصنيف مسلسلات عند فتح الرئيسية، ثم تصنيف واحد
    // إضافي في كل مرة يقترب فيها المستخدم من أسفل القائمة (بدل زر "تحميل المزيد" اليدوي). المسلسلات
    // لها الأولوية حتى تنفد تصنيفاتها، تماماً مثل loadNextHomeCategory() في app.js
    var revealedMovieCatCount by remember { mutableStateOf(0) }
    var revealedSeriesCatCount by remember { mutableStateOf(0) }

    LaunchedEffect(movieCategories, seriesCategories) {
        if (revealedMovieCatCount == 0 && movieCategories.isNotEmpty()) {
            onLoadCategoryStreams(movieCategories[0].id, "vod")
            revealedMovieCatCount = 1
        }
        if (revealedSeriesCatCount == 0 && seriesCategories.isNotEmpty()) {
            onLoadCategoryStreams(seriesCategories[0].id, "series")
            revealedSeriesCatCount = 1
        }
    }

    fun revealNextCategory() {
        if (revealedSeriesCatCount < seriesCategories.size) {
            onLoadCategoryStreams(seriesCategories[revealedSeriesCatCount].id, "series")
            revealedSeriesCatCount++
        } else if (revealedMovieCatCount < movieCategories.size) {
            onLoadCategoryStreams(movieCategories[revealedMovieCatCount].id, "vod")
            revealedMovieCatCount++
        }
    }

    // صف واحد لكل تصنيف مكشوف حتى الآن وله محتوى فعلاً محمَّل — بعنوان التصنيف الحقيقي (وليس
    // معرِّفه الخام كما كان سابقاً)
    val movieCategoryRows = remember(movies, movieCategories, revealedMovieCatCount) {
        movieCategories.take(revealedMovieCatCount).mapNotNull { cat ->
            val items = movies.filter { it.category == cat.id }
            if (items.isNotEmpty()) cat.name to items else null
        }
    }
    val seriesCategoryRows = remember(series, seriesCategories, revealedSeriesCatCount) {
        seriesCategories.take(revealedSeriesCatCount).mapNotNull { cat ->
            val items = series.filter { it.category == cat.id }
            if (items.isNotEmpty()) cat.name to items else null
        }
    }
    val hasMoreCategories = revealedSeriesCatCount < seriesCategories.size || revealedMovieCatCount < movieCategories.size

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // معادِل IntersectionObserver (rootMargin: 200px) في سطح المكتب — يكتشف الاقتراب من أسفل
    // القائمة ويُحمِّل التصنيف التالي تلقائياً بدل انتظار ضغطة زر يدوية
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore, hasMoreCategories) {
        if (shouldLoadMore && hasMoreCategories) revealNextCategory()
    }

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
        movieCategoryRows.forEach { (categoryName, categoryItems) ->
            item {
                TvContentRow(title = categoryName) {
                    items(categoryItems) { m ->
                        TvPosterCard(title = m.title, imageUrl = m.posterUrl) { onMovieClick(m) }
                    }
                }
            }
        }
        seriesCategoryRows.forEach { (categoryName, categoryItems) ->
            item {
                TvContentRow(title = categoryName) {
                    items(categoryItems) { s ->
                        TvPosterCard(title = s.title, imageUrl = s.posterUrl) { onSeriesClick(s) }
                    }
                }
            }
        }

        if (hasMoreCategories) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = TvRed,
                        strokeWidth = 2.dp
                    )
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
