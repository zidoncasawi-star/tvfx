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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
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
import com.example.tv.ui.components.requestFocusWithRetry

/** شاشة التفاصيل — نفس بنية #detailOverlay: خلفية باهتة، معلومات، وحلقات لكل موسم للمسلسلات. */
@Composable
fun TvDetailScreen(
    movie: MovieEntity?,
    series: SeriesEntity?,
    episodes: List<Episode>,
    isFavorite: Boolean,
    genre: String = "",
    cast: String = "",
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

    // زر "تشغيل" للمسلسل يشغّل أول حلقة في الموسم المحدَّد حالياً (أو أول حلقة متاحة إن لم توجد
    // حلقات في هذا الموسم تحديداً) — تماماً مثل زر Play في صفحة تفاصيل الفيلم
    val firstPlayableEpisode = remember(episodes, selectedSeason) {
        episodes.firstOrNull { it.seasonNum == selectedSeason } ?: episodes.firstOrNull()
    }

    // بدون تركيز أوّلي صريح، لا يوجد أي عنصر مركَّز عند ظهور الشاشة فتضيع كل ضغطات D-pad
    // (الأسهم لا تحرّك شيئاً وزر OK لا يفعل شيئاً) — نفس السبب الجذري الذي عولج سابقاً في
    // مشغّل التلفاز. نُركّز زر "▶ Play" دائماً إن أمكن تشغيل شيء (فيلم أو أول حلقة مسلسل)،
    // وإلا نتراجع لزر الإغلاق حتى لا تبقى الشاشة بلا أي عنصر قابل للتركيز مطلقاً
    val canPlay = movie != null || firstPlayableEpisode != null
    val initialFocusRequester = remember { FocusRequester() }
    LaunchedEffect(movie?.id, series?.id) {
        initialFocusRequester.requestFocusWithRetry()
    }

    // كان الانتقال يميناً من زرّي Play/Favorites (أسفل العمود الأيسر) نحو عمود الحلقات (الذي يبدأ
    // من أعلى العمود الأيمن) يفشل غالباً — فجوة رأسية كبيرة تمنع البحث المكاني التلقائي من إيجاد
    // مسار. نُحدِّد وجهة الانتقال يميناً/يساراً صراحة بدل الاعتماد على الهندسة المكانية فقط
    val favoritesFocusRequester = remember { FocusRequester() }
    val episodesEntryFocusRequester = remember { FocusRequester() }

    // نقطة الدخول الوحيدة إلى عمود الحلقات: زر الموسم الأول إن وُجدت مواسم، وإلا أول صف حلقة.
    // إسناد نفس FocusRequester لعنصرين مختلفين في آنٍ واحد غير مسموح في Compose (يفشل عند استدعاء
    // requestFocus() لاحقاً) — وكان هذا يحدث فعلياً لأي مسلسل بموسم واحد فقط: كان كل من أول رقاقة
    // موسم (شرطها seasons.isNotEmpty()) وأول صف حلقة (شرطها كان seasons.size <= 1) يحاولان
    // استخدام نفس FocusRequester معاً، فيفشل التنقل إلى الحلقات تماماً ولا يمكن تشغيل أي حلقة
    val episodesEntryIsSeasonChip = seasons.isNotEmpty()

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
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 36.dp)
                .then(if (!canPlay) Modifier.focusRequester(initialFocusRequester) else Modifier)
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
                val genreTags = remember(genre, category) {
                    genre.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        .ifEmpty { listOfNotNull(category.takeIf { it.isNotBlank() }) }
                }
                if (genreTags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        genreTags.take(5).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(tag, color = Color(0xFFDDDDDD), fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (plot.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(plot, color = Color(0xFFDDDDDD), fontSize = 14.sp, lineHeight = 20.sp)
                }
                if (cast.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Cast: $cast", color = TvTextGray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // كان هذا الانتقال الصريح إلى عمود الحلقات مُطبَّقاً على زرّي Play و Favorites معاً،
                    // فكان الضغط يميناً من Play يقفز مباشرة إلى الحلقات متجاوزاً زر Favorites تماماً —
                    // ما جعله غير قابل للوصول إليه إطلاقاً عبر التنقل. الآن يُطبَّق فقط على Favorites
                    // (آخر عنصر فعلياً قبل عمود الحلقات)، فيصل الضغط يميناً من Play إلى Favorites
                    // بالبحث المكاني الطبيعي أولاً كما هو متوقَّع
                    val jumpToEpisodesModifier = if (series != null) {
                        Modifier.focusProperties { right = episodesEntryFocusRequester }
                    } else Modifier
                    if (movie != null) {
                        TvPrimaryButton(
                            text = "▶ Play",
                            onClick = { onPlayMovie(movie) },
                            modifier = Modifier.focusRequester(initialFocusRequester)
                        )
                    } else if (firstPlayableEpisode != null) {
                        TvPrimaryButton(
                            text = "▶ Play",
                            onClick = { onPlayEpisode(firstPlayableEpisode) },
                            modifier = Modifier.focusRequester(initialFocusRequester)
                        )
                    }
                    TvOutlineButton(
                        text = if (isFavorite) "♥ In Favorites" else "♡ Add to Favorites",
                        onClick = onToggleFavorite,
                        modifier = Modifier.focusRequester(favoritesFocusRequester).then(jumpToEpisodesModifier)
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
                    Text(
                        "Episodes",
                        color = TvTextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    if (seasons.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.focusRestorer()
                        ) {
                            itemsIndexed(seasons, key = { _, s -> s }) { index, s ->
                                val active = s == selectedSeason
                                TvFocusable(
                                    modifier = if (index == 0 && episodesEntryIsSeasonChip) {
                                        Modifier
                                            .focusRequester(episodesEntryFocusRequester)
                                            .focusProperties { left = favoritesFocusRequester }
                                    } else Modifier,
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
                    val seasonEpisodes = episodes.filter { it.seasonNum == selectedSeason }
                    if (seasonEpisodes.isEmpty()) {
                        Text("No episodes found for this season.", color = TvTextGray, fontSize = 13.sp)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.focusRestorer()
                        ) {
                            itemsIndexed(seasonEpisodes, key = { _, ep -> ep.id }) { index, ep ->
                                TvEpisodeRow(
                                    episode = ep,
                                    onClick = { onPlayEpisode(ep) },
                                    modifier = if (index == 0 && !episodesEntryIsSeasonChip) {
                                        Modifier
                                            .focusRequester(episodesEntryFocusRequester)
                                            .focusProperties { left = favoritesFocusRequester }
                                    } else Modifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * صف حلقة — كان الصف بأكمله قابلاً للضغط ضمنياً (زر OK يشغّل الحلقة) دون أي أيقونة أو مؤشر
 * بصري يدل على ذلك، فبدا للمستخدم وكأنه لا يوجد "زر تشغيل" للحلقات إطلاقاً. أضفنا أيقونة تشغيل
 * دائرية صريحة في نهاية كل صف، تتحول للأحمر عند التركيز عليها لتأكيد إمكانية الضغط بوضوح.
 */
@Composable
private fun TvEpisodeRow(episode: Episode, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TvFocusable(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) { focused ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${episode.episodeNum}",
                color = TvTextGray,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.width(30.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    episode.title,
                    color = Color(0xFFEEEEEE),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (episode.duration.isNotBlank()) {
                    Text(episode.duration, color = TvTextGray, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(if (focused) TvRed else Color.White.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play episode",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
