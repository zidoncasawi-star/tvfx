package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.NetflixRed
import com.example.ui.viewmodel.PlayingMedia

@Composable
fun HomeScreen(
    channels: List<ChannelEntity>,
    movies: List<MovieEntity>,
    series: List<SeriesEntity>,
    watchHistory: List<WatchHistoryEntity>,
    activePlaylist: PlaylistEntity?,
    selectedCategory: String,
    searchQuery: String,
    onCategorySelect: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlayMedia: (PlayingMedia) -> Unit,
    onMovieClick: (MovieEntity) -> Unit,
    onSeriesClick: (SeriesEntity) -> Unit,
    onToggleChannelFav: (ChannelEntity) -> Unit,
    onToggleMovieFav: (MovieEntity) -> Unit,
    onToggleSeriesFav: (SeriesEntity) -> Unit,
    liveCategories: List<com.example.model.XtreamCategoryEntity> = emptyList(),
    vodCategories: List<com.example.model.XtreamCategoryEntity> = emptyList(),
    seriesCategories: List<com.example.model.XtreamCategoryEntity> = emptyList(),
    onLoadCategoryStreams: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedCountryCode by rememberSaveable { mutableStateOf("all") }

    // Category ID to Name mapping helper
    val categoryIdToNameMap = remember(liveCategories, vodCategories, seriesCategories) {
        val map = mutableMapOf<String, String>()
        liveCategories.forEach { map[it.id] = it.name }
        vodCategories.forEach { map[it.id] = it.name }
        seriesCategories.forEach { map[it.id] = it.name }
        map
    }

    fun getCategoryName(idOrName: String): String {
        return categoryIdToNameMap[idOrName] ?: idOrName
    }

    // Pre-collect all available actual category NAMES from complete server categories list
    val allCategoryNames = remember(liveCategories, vodCategories, seriesCategories) {
        (liveCategories.map { it.name } + 
         vodCategories.map { it.name } + 
         seriesCategories.map { it.name })
            .distinct()
            .filter { it.isNotBlank() }
    }

    // Dynamically filter countriesList to show only those present in active subscription
    val availableCountries = remember(allCategoryNames) {
        countriesList.filter { country ->
            if (country.code == "all") {
                true
            } else {
                allCategoryNames.any { categoryName ->
                    country.keywords.any { keyword ->
                        categoryName.lowercase().contains(keyword)
                    }
                }
            }
        }
    }

    // يمنع تكرار طلب نفس التصنيف عدة مرات أثناء انتظار وصول أول رد شبكة له
    val requestedCategoryIds = remember { mutableSetOf<String>() }
    var isLoadingMore by remember { mutableStateOf(false) }

    // لا يوجد تحميل تلقائي لكل التصنيفات في الخلفية — بالضبط كما تعمل تطبيقات IPTV الاحترافية
    // (مثل IPTV Smarters): تُعرض فقط التصنيفات التي وصل محتواها فعلياً من الاستيراد الأولي،
    // والمزيد يُحمَّل فقط عند ضغط المستخدم على زر "تحميل المزيد" بنفسه — طلب واحد في كل مرة، بلا زحف تلقائي.
    val hasMoreToLoad = remember(vodCategories, seriesCategories, movies, series) {
        seriesCategories.any { cat -> series.none { it.category == cat.id } } ||
            vodCategories.any { cat -> movies.none { it.category == cat.id } }
    }

    fun loadNextCategoryManually() {
        if (isLoadingMore) return
        val nextSeriesCat = seriesCategories.firstOrNull { cat ->
            series.none { it.category == cat.id } && !requestedCategoryIds.contains("series_${cat.id}")
        }
        if (nextSeriesCat != null) {
            requestedCategoryIds.add("series_${nextSeriesCat.id}")
            isLoadingMore = true
            onLoadCategoryStreams(nextSeriesCat.id, "series")
            return
        }
        val nextVodCat = vodCategories.firstOrNull { cat ->
            movies.none { it.category == cat.id } && !requestedCategoryIds.contains("vod_${cat.id}")
        }
        if (nextVodCat != null) {
            requestedCategoryIds.add("vod_${nextVodCat.id}")
            isLoadingMore = true
            onLoadCategoryStreams(nextVodCat.id, "vod")
        }
    }

    // بمجرد وصول محتوى جديد فعلياً، أوقف مؤشر "جاري التحميل" لتفعيل الزر مجدداً
    LaunchedEffect(movies, series) {
        isLoadingMore = false
    }

    // On-demand load streams of matched categories for selected country if not loaded yet
    LaunchedEffect(selectedCountryCode, vodCategories, seriesCategories, movies, series) {
        if (selectedCountryCode != "all") {
            val selectedCountry = countriesList.find { it.code == selectedCountryCode }
            if (selectedCountry != null) {
                // Check and load VOD categories matching country keywords
                vodCategories.forEach { cat ->
                    val matches = selectedCountry.keywords.any { keyword ->
                        cat.name.lowercase().contains(keyword)
                    }
                    if (matches) {
                        val isLoaded = movies.any { it.category == cat.id }
                        if (!isLoaded) {
                            onLoadCategoryStreams(cat.id, "vod")
                        }
                    }
                }

                // Check and load Series categories matching country keywords
                seriesCategories.forEach { cat ->
                    val matches = selectedCountry.keywords.any { keyword ->
                        cat.name.lowercase().contains(keyword)
                    }
                    if (matches) {
                        val isLoaded = series.any { it.category == cat.id }
                        if (!isLoaded) {
                            onLoadCategoryStreams(cat.id, "series")
                        }
                    }
                }
            }
        }
    }

    if (channels.isEmpty() && movies.isEmpty() && series.isEmpty() && watchHistory.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NetflixRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (activePlaylist != null) "الاشتراك نشط! يرجى تصفح القنوات أو الأفلام من القائمة السفلية لتحميل المحتوى" else "لا يوجد محتوى بعد — يرجى تفعيل اشتراكك واستيراد المحتوى من شاشة حسابي",
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
        return
    }

    // Pre-group movies and series by category name for extremely fast O(1) lookups during scroll
    val moviesByCategoryName = remember(movies, categoryIdToNameMap) {
        movies.groupBy { getCategoryName(it.category) }
    }

    val seriesByCategoryName = remember(series, categoryIdToNameMap) {
        series.groupBy { getCategoryName(it.category) }
    }

    val continueWatchingList = remember(watchHistory) {
        watchHistory.filter { it.itemType == "MOVIE" || it.itemType == "EPISODE" }
    }

    val filteredMovies = remember(movies, selectedCategory, searchQuery) {
        movies.filter {
            (selectedCategory == "الكل" || it.category.contains(selectedCategory)) &&
                    (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
        }
    }

    val filteredChannels = remember(channels, selectedCategory, searchQuery) {
        channels.filter {
            (selectedCategory == "الكل" || it.category.contains(selectedCategory)) &&
                    (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
        }
    }

    val filteredSeries = remember(series, selectedCategory, searchQuery) {
        series.filter {
            (selectedCategory == "الكل" || it.category.contains(selectedCategory)) &&
                    (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true))
        }
    }

    val matchedCategories = remember(allCategoryNames, selectedCountryCode, selectedCategory) {
        val selectedCountry = countriesList.find { it.code == selectedCountryCode }
        if (selectedCountry == null || selectedCountry.code == "all") {
            emptyList()
        } else {
            allCategoryNames.filter { categoryName ->
                val matchesCountry = selectedCountry.keywords.any { keyword ->
                    categoryName.lowercase().contains(keyword)
                }
                val matchesCategory = selectedCategory == "الكل" || categoryName == selectedCategory
                matchesCountry && matchesCategory
            }
        }
    }

    val featuredList = remember(movies, series, selectedCountryCode, categoryIdToNameMap) {
        val yearRegex = Regex("\\b(19\\d\\d|20[0-2]\\d)\\b")
        fun extractYear(title: String, default: String): Int {
            val match = yearRegex.find(title)
            return match?.value?.toIntOrNull() ?: default.toIntOrNull() ?: 2024
        }

        val selectedCountry = countriesList.find { it.code == selectedCountryCode }
        val filteredMov = if (selectedCountryCode == "all" || selectedCountry == null) {
            movies
        } else {
            movies.filter { m -> selectedCountry.keywords.any { k -> getCategoryName(m.category).lowercase().contains(k) } }
        }
        val filteredSer = if (selectedCountryCode == "all" || selectedCountry == null) {
            series
        } else {
            series.filter { s -> selectedCountry.keywords.any { k -> getCategoryName(s.category).lowercase().contains(k) } }
        }

        val convertedMovies = filteredMov.map { m ->
            val year = extractYear(m.title, m.releaseYear)
            MovieEntity(
                id = m.id,
                playlistId = m.playlistId,
                title = m.title,
                streamUrl = m.streamUrl,
                posterUrl = m.posterUrl,
                backdropUrl = m.backdropUrl,
                category = m.category,
                rating = m.rating,
                releaseYear = year.toString(),
                plot = m.plot,
                isFavorite = m.isFavorite
            )
        }
        val convertedSeries = filteredSer.map { s ->
            val year = extractYear(s.title, s.releaseYear)
            MovieEntity(
                id = s.id,
                playlistId = s.playlistId,
                title = s.title,
                streamUrl = "", // clicking play opens detail for series
                posterUrl = s.posterUrl,
                backdropUrl = s.backdropUrl,
                category = s.category,
                rating = s.rating,
                releaseYear = year.toString(),
                plot = s.plot,
                isFavorite = s.isFavorite
            )
        }
        val combined = (convertedMovies + convertedSeries)
        if (combined.isNotEmpty()) {
            combined.sortedWith(
                compareByDescending<MovieEntity> { m ->
                    m.releaseYear.toIntOrNull() ?: 0
                }.thenByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
            ).take(6)
        } else {
            emptyList()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Hero Banner (Featured Auto-cycling Content)
        if (searchQuery.isEmpty() && featuredList.isNotEmpty()) {
            item {
                AutoCyclingHeroBanner(
                    featuredList = featuredList,
                        onPlayClick = { selectedMovie ->
                            val ch = channels.find { it.id == selectedMovie.id }
                            val ser = series.find { it.id == selectedMovie.id }
                            if (ch != null) {
                                onPlayMedia(
                                    PlayingMedia(
                                        id = ch.id,
                                        title = ch.name,
                                        streamUrl = ch.streamUrl,
                                        posterUrl = ch.logoUrl,
                                        category = ch.category,
                                        type = "LIVE",
                                        channelList = channels
                                    )
                                )
                            } else if (ser != null) {
                                onSeriesClick(ser)
                            } else {
                                onPlayMedia(
                                    PlayingMedia(
                                        id = selectedMovie.id,
                                        title = selectedMovie.title,
                                        streamUrl = selectedMovie.streamUrl,
                                        posterUrl = selectedMovie.posterUrl,
                                        category = selectedMovie.category,
                                        type = "MOVIE"
                                    )
                                )
                            }
                        },
                        onDetailClick = { selectedMovie ->
                            val ser = series.find { it.id == selectedMovie.id }
                            if (ser != null) {
                                onSeriesClick(ser)
                            } else {
                                val mov = movies.find { it.id == selectedMovie.id }
                                if (mov != null) {
                                    onMovieClick(mov)
                                }
                            }
                        },
                        onToggleFavorite = { selectedMovie ->
                            val ch = channels.find { it.id == selectedMovie.id }
                            if (ch != null) {
                                onToggleChannelFav(ch)
                            } else {
                                val ser = series.find { it.id == selectedMovie.id }
                                if (ser != null) {
                                    onToggleSeriesFav(ser)
                                } else {
                                    val mov = movies.find { it.id == selectedMovie.id }
                                    if (mov != null) {
                                        onToggleMovieFav(mov)
                                    }
                                }
                            }
                        }
                    )
                }
            }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("ابحث عن القنوات، الأفلام، أو المسلسلات...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NetflixRed) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NetflixRed,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedContainerColor = com.example.ui.theme.DarkCardBg,
                    unfocusedContainerColor = com.example.ui.theme.DarkCardBg
                )
            )
        }

        // Country Flags Filter Bar
        item {
            CountryFlagsFilterBar(
                selectedCountryCode = selectedCountryCode,
                onCountrySelect = { code ->
                    selectedCountryCode = code
                    onCategorySelect("الكل") // Reset specific category chip on country change
                },
                availableCountries = availableCountries,
                modifier = Modifier.fillMaxWidth()
            )
        }



        if (selectedCountryCode == "all") {
            // Continue Watching Row
            if (continueWatchingList.isNotEmpty() && searchQuery.isEmpty()) {
                item {
                    HistoryHorizontalList(
                        title = "متابعة المشاهدة",
                        historyList = continueWatchingList,
                        onHistoryClick = { item ->
                            onPlayMedia(
                                PlayingMedia(
                                    id = item.itemId,
                                    title = item.title,
                                    streamUrl = item.streamUrl,
                                    posterUrl = item.posterUrl,
                                    type = item.itemType,
                                    startPositionMs = item.progressMs
                                )
                            )
                        }
                    )
                }
            }

            // Movies Row (top-rated mix, kept as a quick-access shelf)
            if (filteredMovies.isNotEmpty()) {
                item {
                    MovieHorizontalList(
                        title = "الأفلام الأكثر مشاهدة 🎬",
                        movies = filteredMovies,
                        onMovieClick = onMovieClick,
                        onFavoriteToggle = onToggleMovieFav
                    )
                }
            }

            // Series Row (top-rated mix, kept as a quick-access shelf)
            if (filteredSeries.isNotEmpty()) {
                item {
                    SeriesHorizontalList(
                        title = "المسلسلات الحصرية 📺",
                        seriesList = filteredSeries,
                        onSeriesClick = onSeriesClick,
                        onFavoriteToggle = onToggleSeriesFav
                    )
                }
            }

            // Real per-category rows: كل تصنيف كما هو مُعرَّف فعلياً في لوحة التحكم/السيرفر،
            // بنفس ترتيبه هناك، بدل تجميع كل الأفلام/المسلسلات في سلة واحدة عامة
            seriesCategories.forEach { cat ->
                val catSeries = seriesByCategoryName[cat.name] ?: emptyList()
                val filteredCatSeries = if (searchQuery.isEmpty()) catSeries else catSeries.filter { it.title.contains(searchQuery, ignoreCase = true) }
                if (filteredCatSeries.isNotEmpty()) {
                    item(key = "cat_ser_${cat.id}") {
                        SeriesHorizontalList(
                            title = cat.name,
                            seriesList = filteredCatSeries,
                            onSeriesClick = onSeriesClick,
                            onFavoriteToggle = onToggleSeriesFav
                        )
                    }
                }
            }

            vodCategories.forEach { cat ->
                val catMovies = moviesByCategoryName[cat.name] ?: emptyList()
                val filteredCatMovies = if (searchQuery.isEmpty()) catMovies else catMovies.filter { it.title.contains(searchQuery, ignoreCase = true) }
                if (filteredCatMovies.isNotEmpty()) {
                    item(key = "cat_mov_${cat.id}") {
                        MovieHorizontalList(
                            title = cat.name,
                            movies = filteredCatMovies,
                            onMovieClick = onMovieClick,
                            onFavoriteToggle = onToggleMovieFav
                        )
                    }
                }
            }

            // زر تحميل المزيد: تصفّح احترافي بلا زحف تلقائي في الخلفية — تماماً كما في تطبيقات IPTV المعروفة
            if (hasMoreToLoad && searchQuery.isEmpty()) {
                item(key = "load_more_categories") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(color = NetflixRed, modifier = Modifier.size(28.dp))
                        } else {
                            OutlinedButton(onClick = { loadNextCategoryManually() }) {
                                Text("تحميل المزيد من التصنيفات")
                            }
                        }
                    }
                }
            }
        } else {
            // Grouped Country Layout (Sliders segmented dynamically by matched category name)
            var hasAnyContent = false
            matchedCategories.forEach { category ->
                val catMovies = moviesByCategoryName[category] ?: emptyList()
                val catSeries = seriesByCategoryName[category] ?: emptyList()

                val filteredCatMovies = if (searchQuery.isEmpty()) catMovies else catMovies.filter { it.title.contains(searchQuery, ignoreCase = true) }
                val filteredCatSeries = if (searchQuery.isEmpty()) catSeries else catSeries.filter { it.title.contains(searchQuery, ignoreCase = true) }

                if (filteredCatMovies.isNotEmpty() || filteredCatSeries.isNotEmpty()) {
                    hasAnyContent = true
                }

                if (filteredCatMovies.isNotEmpty()) {
                    item(key = "country_mov_$category") {
                        MovieHorizontalList(
                            title = category,
                            movies = filteredCatMovies,
                            onMovieClick = onMovieClick,
                            onFavoriteToggle = onToggleMovieFav
                        )
                    }
                }

                if (filteredCatSeries.isNotEmpty()) {
                    item(key = "country_ser_$category") {
                        SeriesHorizontalList(
                            title = category,
                            seriesList = filteredCatSeries,
                            onSeriesClick = onSeriesClick,
                            onFavoriteToggle = onToggleSeriesFav
                        )
                    }
                }
            }

            if (!hasAnyContent) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 48.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نتائج مطابقة لهذه الدولة في اشتراكك الحالي",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
