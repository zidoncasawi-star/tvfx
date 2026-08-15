package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.example.model.ChannelEntity
import com.example.model.MovieEntity
import com.example.model.SeriesEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite

/**
 * نتائج البحث الشامل — تجميع مطابقات القنوات والأفلام والمسلسلات معاً، مطابق لـ #view-search
 * في سطح المكتب (بحث حقيقي عبر كل الأقسام دفعة واحدة، وليس فلترة القسم المفتوح فقط).
 */
@Composable
fun TvSearchResultsScreen(
    query: String,
    channels: List<ChannelEntity>,
    movies: List<MovieEntity>,
    series: List<SeriesEntity>,
    onPlayChannel: (ChannelEntity) -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit
) {
    val q = query.trim()
    val matchedChannels = remember(channels, q) { channels.filter { it.name.contains(q, ignoreCase = true) } }
    val matchedMovies = remember(movies, q) { movies.filter { it.title.contains(q, ignoreCase = true) } }
    val matchedSeries = remember(series, q) { series.filter { it.title.contains(q, ignoreCase = true) } }
    val totalCount = matchedChannels.size + matchedMovies.size + matchedSeries.size

    LazyColumn(modifier = Modifier.fillMaxSize().background(TvBg)) {
        item {
            Text(
                text = "Search results for \"$q\" ($totalCount)",
                color = TvTextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 40.dp, top = 24.dp, bottom = 10.dp)
            )
        }

        if (totalCount == 0) {
            item {
                Box(modifier = Modifier.fillMaxSize().padding(top = 60.dp), contentAlignment = Alignment.TopCenter) {
                    Text("No matches found.", color = TvTextGray, fontSize = 14.sp)
                }
            }
        } else {
            if (matchedChannels.isNotEmpty()) {
                item {
                    SearchResultRow(title = "Live TV") {
                        items(matchedChannels) { ch ->
                            TvPosterCard(title = ch.name, imageUrl = ch.logoUrl, isChannel = true) { onPlayChannel(ch) }
                        }
                    }
                }
            }
            if (matchedMovies.isNotEmpty()) {
                item {
                    SearchResultRow(title = "Movies") {
                        items(matchedMovies) { m ->
                            TvPosterCard(title = m.title, imageUrl = m.posterUrl) { onMovieClick(m) }
                        }
                    }
                }
            }
            if (matchedSeries.isNotEmpty()) {
                item {
                    SearchResultRow(title = "Series") {
                        items(matchedSeries) { s ->
                            TvPosterCard(title = s.title, imageUrl = s.posterUrl) { onSeriesClick(s) }
                        }
                    }
                }
            }

            item { androidx.compose.foundation.layout.Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun SearchResultRow(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            title,
            color = TvTextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
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
