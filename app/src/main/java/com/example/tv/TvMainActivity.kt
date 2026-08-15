package com.example.tv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.Text
import com.example.model.MainTab
import com.example.tv.theme.FxTvTelevisionTheme
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.TvDetailScreen
import com.example.tv.ui.TvFavoritesScreen
import com.example.tv.ui.TvHomeScreen
import com.example.tv.ui.TvLiveTvScreen
import com.example.tv.ui.TvLoginScreen
import com.example.tv.ui.TvMoviesScreen
import com.example.tv.ui.TvProfileScreen
import com.example.tv.ui.TvRecordingsScreen
import com.example.tv.ui.TvSearchResultsScreen
import com.example.tv.ui.TvSeriesScreen
import com.example.tv.ui.TvSettingsScreen
import com.example.tv.ui.components.TvOutlineButton
import com.example.tv.ui.components.TvPrimaryButton
import com.example.tv.ui.components.TvPlayerView
import com.example.ui.screens.SplashScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayingMedia

private const val ADMIN_URL = "https://app.flixplayer.pro"

/**
 * نقطة الدخول لأجهزة تلفزيون Android — نفس بنية MainActivity (تُشارك نفس MainViewModel وقاعدة
 * البيانات والحساب)، لكن بواجهة مصممة لمسافة المشاهدة على التلفاز وتنقل D-pad، وبنفس تصميم
 * تطبيق سطح المكتب (شريط أيقونات جانبي، هيرو بانر، تشغيل ملء الشاشة دائماً).
 */
class TvMainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // يجب ضبط هذا قبل أي فحص لقفل الجهاز الواحد، وإلا يعتبر الـ ViewModel نفسه (المهيَّأ
        // افتراضياً كـ "phone") جهازاً غريباً فور تسجيل دخول التلفاز ويُعيده فوراً لشاشة الدخول
        viewModel.configureAsTvDevice()

        setContent {
            FxTvTelevisionTheme {
                val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
                if (showSplash) {
                    SplashScreen(onFinish = { viewModel.finishSplash() })
                    return@FxTvTelevisionTheme
                }

                val lockedByOtherDevice by viewModel.lockedByOtherDevice.collectAsStateWithLifecycle()
                LaunchedEffect(lockedByOtherDevice) {
                    // لا توجد شاشة قفل وسيطة على التلفاز أو سطح المكتب — عودة صامتة فورية لتسجيل الدخول
                    if (lockedByOtherDevice) viewModel.returnToLoginLocally()
                }

                val loggedInAccount by viewModel.loggedInAccount.collectAsStateWithLifecycle()
                val authError by viewModel.authError.collectAsStateWithLifecycle()
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()

                if (loggedInAccount == null) {
                    TvLoginScreen(
                        authError = authError,
                        isSyncing = isSyncing,
                        onLogin = { id, pass -> viewModel.loginUser(id, pass) },
                        onRegister = { email, username, pass, phone -> viewModel.registerUser(email, username, pass, phone) },
                        onPairingApproved = { session -> viewModel.completeTvPairingLogin(session, ADMIN_URL) }
                    )
                    return@FxTvTelevisionTheme
                }

                val account = loggedInAccount!!
                val playlists by viewModel.playlists.collectAsStateWithLifecycle()

                if (!account.isActivated) {
                    TvPendingActivationScreen(
                        activationCode = account.activationCode,
                        isSyncing = isSyncing,
                        onCheckActivation = { viewModel.checkSubscription() },
                        onLogout = { viewModel.logoutUser() }
                    )
                    return@FxTvTelevisionTheme
                }

                var selectedTab by remember { mutableStateOf(MainTab.HOME) }
                var showSettings by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }

                val channels by viewModel.channels.collectAsStateWithLifecycle()
                val movies by viewModel.movies.collectAsStateWithLifecycle()
                val series by viewModel.series.collectAsStateWithLifecycle()
                val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
                val liveCategories by viewModel.liveCategories.collectAsStateWithLifecycle()
                val vodCategories by viewModel.vodCategories.collectAsStateWithLifecycle()
                val seriesCategories by viewModel.seriesCategories.collectAsStateWithLifecycle()
                val favChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
                val favMovies by viewModel.favoriteMovies.collectAsStateWithLifecycle()
                val favSeries by viewModel.favoriteSeries.collectAsStateWithLifecycle()
                val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
                val currentlyPlaying by viewModel.currentlyPlaying.collectAsStateWithLifecycle()
                val mediaDetail by viewModel.mediaDetail.collectAsStateWithLifecycle()
                val recordings by viewModel.recordings.collectAsStateWithLifecycle()

                var autoplay by remember { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize().background(TvBg)) {
                    // ترتيب أولوية زر الرجوع بالريموت — خطوة بخطوة (نفس منطق الهاتف MainActivity.kt:331)،
                    // بدل السلوك الافتراضي الذي كان يُنهي التطبيق مباشرة من أي شاشة:
                    // مشغّل مفتوح -> يغلق المشغّل فقط | تفاصيل فيلم/مسلسل -> تُغلق | الإعدادات -> تُغلق
                    // | تبويب غير الرئيسية -> يعود للرئيسية | الرئيسية -> السلوك الافتراضي للنظام (خروج)
                    androidx.activity.compose.BackHandler(enabled = currentlyPlaying != null) {
                        viewModel.stopMedia()
                    }
                    androidx.activity.compose.BackHandler(enabled = currentlyPlaying == null && mediaDetail != null) {
                        viewModel.closeMediaDetail()
                    }
                    androidx.activity.compose.BackHandler(enabled = currentlyPlaying == null && mediaDetail == null && showSettings) {
                        showSettings = false
                    }
                    androidx.activity.compose.BackHandler(
                        enabled = currentlyPlaying == null && mediaDetail == null && !showSettings && searchQuery.isNotBlank()
                    ) {
                        searchQuery = ""
                    }
                    androidx.activity.compose.BackHandler(
                        enabled = currentlyPlaying == null && mediaDetail == null && !showSettings && searchQuery.isBlank() && selectedTab != MainTab.HOME
                    ) {
                        selectedTab = MainTab.HOME
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        com.example.tv.ui.components.TvNavRail(
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it },
                            onOpenSettings = { showSettings = true }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            com.example.tv.ui.components.TvTopBar(
                                adminUrl = ADMIN_URL,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onLogout = { viewModel.logoutUser() }
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isNotBlank()) {
                                    TvSearchResultsScreen(
                                        query = searchQuery,
                                        channels = channels,
                                        movies = movies,
                                        series = series,
                                        onPlayChannel = { ch ->
                                            viewModel.playMedia(
                                                PlayingMedia(
                                                    id = ch.id, title = ch.name, streamUrl = ch.streamUrl,
                                                    posterUrl = ch.logoUrl, category = ch.category, type = "LIVE",
                                                    channelList = channels
                                                )
                                            )
                                        },
                                        onMovieClick = { viewModel.openMovieDetail(it) },
                                        onSeriesClick = { viewModel.openSeriesDetail(it) }
                                    )
                                } else {
                            when (selectedTab) {
                                MainTab.HOME -> TvHomeScreen(
                                    channels = channels,
                                    movies = movies,
                                    series = series,
                                    movieCategories = vodCategories,
                                    seriesCategories = seriesCategories,
                                    watchHistory = watchHistory,
                                    onLoadCategoryStreams = { catId, type -> viewModel.loadStreamsByCategory(catId, type) },
                                    onPlayChannel = { ch ->
                                        viewModel.playMedia(
                                            PlayingMedia(
                                                id = ch.id, title = ch.name, streamUrl = ch.streamUrl,
                                                posterUrl = ch.logoUrl, category = ch.category, type = "LIVE",
                                                channelList = channels
                                            )
                                        )
                                    },
                                    onMovieClick = { viewModel.openMovieDetail(it) },
                                    onSeriesClick = { viewModel.openSeriesDetail(it) },
                                    onResumeHistory = { h ->
                                        viewModel.playMedia(
                                            PlayingMedia(
                                                id = h.itemId, title = h.title, streamUrl = h.streamUrl,
                                                posterUrl = h.posterUrl, type = h.itemType, startPositionMs = h.progressMs
                                            )
                                        )
                                    },
                                    onPlayMovie = { mov ->
                                        viewModel.playMedia(
                                            PlayingMedia(
                                                id = mov.id, title = mov.title, streamUrl = mov.streamUrl,
                                                posterUrl = mov.posterUrl, category = mov.category, type = "MOVIE"
                                            )
                                        )
                                    }
                                )

                                MainTab.LIVE_TV -> TvLiveTvScreen(
                                    channels = channels,
                                    categories = liveCategories,
                                    onLoadCategoryStreams = { viewModel.loadStreamsByCategory(it, "live") },
                                    onFetchEpg = { ch -> viewModel.fetchShortEpgForChannel(ch) },
                                    onPlayChannel = { ch ->
                                        viewModel.playMedia(
                                            PlayingMedia(
                                                id = ch.id, title = ch.name, streamUrl = ch.streamUrl,
                                                posterUrl = ch.logoUrl, category = ch.category, type = "LIVE",
                                                channelList = channels
                                            )
                                        )
                                    },
                                    isChannelRecording = { id -> viewModel.isChannelRecording(id) },
                                    onStartRecording = { ch, title, minutes -> viewModel.startRecording(ch, title, minutes) },
                                    onStopRecording = { ch ->
                                        viewModel.activeRecordingForChannel(ch.id)?.let { viewModel.stopRecording(it.id) }
                                    },
                                    isPreviewPaused = currentlyPlaying != null
                                )

                                MainTab.MOVIES -> TvMoviesScreen(
                                    movies = movies,
                                    categories = vodCategories,
                                    onLoadCategoryStreams = { viewModel.loadStreamsByCategory(it, "vod") },
                                    onMovieClick = { viewModel.openMovieDetail(it) }
                                )

                                MainTab.SERIES -> TvSeriesScreen(
                                    seriesList = series,
                                    categories = seriesCategories,
                                    onLoadCategoryStreams = { viewModel.loadStreamsByCategory(it, "series") },
                                    onSeriesClick = { viewModel.openSeriesDetail(it) }
                                )

                                MainTab.FAVORITES -> TvFavoritesScreen(
                                    favMovies = favMovies,
                                    favSeries = favSeries,
                                    favChannels = favChannels,
                                    onMovieClick = { viewModel.openMovieDetail(it) },
                                    onSeriesClick = { viewModel.openSeriesDetail(it) },
                                    onPlayChannel = { ch ->
                                        viewModel.playMedia(
                                            PlayingMedia(
                                                id = ch.id, title = ch.name, streamUrl = ch.streamUrl,
                                                posterUrl = ch.logoUrl, category = ch.category, type = "LIVE",
                                                channelList = channels
                                            )
                                        )
                                    }
                                )

                                MainTab.RECORDINGS -> TvRecordingsScreen(
                                    recordings = recordings,
                                    onPlayRecording = { rec ->
                                        viewModel.playMedia(
                                            PlayingMedia(
                                                id = "rec_${rec.id}",
                                                title = rec.programTitle.ifBlank { rec.channelName },
                                                streamUrl = "file://${rec.filePath}",
                                                type = "MOVIE"
                                            )
                                        )
                                    },
                                    onStopRecording = { viewModel.stopRecording(it.id) },
                                    onDeleteRecording = { viewModel.deleteRecording(it) }
                                )

                                MainTab.USER_ACCOUNT -> TvProfileScreen(
                                    account = account,
                                    playlists = playlists,
                                    onSelectPlaylist = { viewModel.selectPlaylist(it) },
                                    onCheckActivation = { viewModel.checkSubscription() },
                                    onContentUpdate = { viewModel.importAdminXtreamSubscription() },
                                    onLogout = { viewModel.logoutUser() }
                                )
                            }
                                }
                            }
                        }
                    }

                    mediaDetail?.let { detail ->
                        val isFav = detail.movie?.isFavorite ?: detail.series?.isFavorite ?: false
                        TvDetailScreen(
                            movie = detail.movie,
                            series = detail.series,
                            episodes = detail.episodes,
                            isFavorite = isFav,
                            genre = detail.genre,
                            cast = detail.cast,
                            onClose = { viewModel.closeMediaDetail() },
                            onPlayMovie = { mov ->
                                viewModel.playMedia(
                                    PlayingMedia(
                                        id = mov.id, title = mov.title, streamUrl = mov.streamUrl,
                                        posterUrl = mov.posterUrl, category = mov.category, type = "MOVIE"
                                    )
                                )
                            },
                            onPlayEpisode = { ep ->
                                viewModel.playMedia(
                                    PlayingMedia(
                                        id = ep.id,
                                        title = "${detail.series?.title ?: ""} - ${ep.title}",
                                        streamUrl = ep.streamUrl,
                                        posterUrl = detail.series?.posterUrl ?: "",
                                        type = "EPISODE",
                                        episodeList = detail.episodes.sortedWith(compareBy({ it.seasonNum }, { it.episodeNum })),
                                        seriesTitle = detail.series?.title ?: ""
                                    )
                                )
                            },
                            onToggleFavorite = {
                                detail.movie?.let { viewModel.toggleMovieFavorite(it) }
                                detail.series?.let { viewModel.toggleSeriesFavorite(it) }
                            }
                        )
                    }

                    if (showSettings) {
                        // بدون تركيز أوّلي صريح هنا، هذه النافذة العائمة (overlay) لا تملك أي عنصر
                        // مركَّز عند ظهورها (بخلاف تبديل التبويبات العادي حيث يُحافَظ على تركيز
                        // الشريط الجانبي) فتضيع كل ضغطات D-pad — نفس السبب الجذري الذي كان يمنع
                        // فتح المشغّل من شاشة التفاصيل
                        val settingsCloseFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                        LaunchedEffect(showSettings) {
                            kotlinx.coroutines.delay(80)
                            runCatching { settingsCloseFocusRequester.requestFocus() }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            TvSettingsScreen(
                                autoplayEnabled = autoplay,
                                onAutoplayChange = { autoplay = it },
                                activePlaylistName = activePlaylist?.name,
                                onClearCache = { viewModel.clearAppCache() },
                                onClearFavorites = { /* يطابق سلوك سطح المكتب: يمسح المفضلة محلياً فقط لاحقاً عند الحاجة */ },
                                onLogout = { viewModel.logoutUser() },
                                onOpenTerms = { openUrl("$ADMIN_URL/terms.php") },
                                onOpenPrivacy = { openUrl("$ADMIN_URL/privacy.php") }
                            )
                            TvOutlineButton(
                                text = "✕ Close",
                                onClick = { showSettings = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 36.dp, end = 36.dp)
                                    .focusRequester(settingsCloseFocusRequester)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = currentlyPlaying != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        currentlyPlaying?.let { media ->
                            androidx.compose.runtime.key(media.streamUrl) {
                                TvPlayerView(
                                    mediaUrl = media.streamUrl,
                                    title = media.title,
                                    type = media.type,
                                    channelList = media.channelList,
                                    episodeList = media.episodeList,
                                    initialPlaybackPositionMs = media.startPositionMs,
                                    onClose = { viewModel.stopMedia() },
                                    onProgressUpdate = { pos, dur -> viewModel.recordProgress(media.id, pos, dur) },
                                    onChannelSelect = { ch ->
                                        viewModel.playMedia(
                                            PlayingMedia(
                                                id = ch.id, title = ch.name, streamUrl = ch.streamUrl,
                                                posterUrl = ch.logoUrl, category = ch.category, type = "LIVE",
                                                channelList = channels
                                            )
                                        )
                                    },
                                    onEpisodeSelect = { ep ->
                                        viewModel.playMedia(
                                            PlayingMedia(
                                                id = ep.id,
                                                title = "${media.seriesTitle} - ${ep.title}",
                                                streamUrl = ep.streamUrl,
                                                posterUrl = media.posterUrl,
                                                type = "EPISODE",
                                                episodeList = media.episodeList,
                                                seriesTitle = media.seriesTitle
                                            )
                                        )
                                    },
                                    isChannelRecording = { id -> viewModel.isChannelRecording(id) },
                                    onStartRecording = { ch, title, minutes -> viewModel.startRecording(ch, title, minutes) },
                                    onStopRecording = { ch ->
                                        viewModel.activeRecordingForChannel(ch.id)?.let { viewModel.stopRecording(it.id) }
                                    }
                                )
                            }
                        }
                    }

                    if (isSyncing) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Importing content... $importProgress%", color = TvTextWhite, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@androidx.compose.runtime.Composable
private fun TvPendingActivationScreen(
    activationCode: String,
    isSyncing: Boolean,
    onCheckActivation: () -> Unit,
    onLogout: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(TvBg), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.width(420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⏳", fontSize = 40.sp)
            Spacer(Modifier.height(10.dp))
            Text("Waiting for Activation", color = TvTextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your account has been created. Send your activation code to the admin to activate your subscription.",
                color = TvTextGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Activation Code", color = TvTextGray, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(activationCode, color = TvTextWhite, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            TvPrimaryButton(
                text = if (isSyncing) "Checking..." else "🔄 Check Activation Status",
                enabled = !isSyncing,
                onClick = onCheckActivation
            )
            Spacer(Modifier.height(10.dp))
            TvOutlineButton(text = "Logout", onClick = onLogout)
        }
    }
}
