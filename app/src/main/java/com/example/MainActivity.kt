package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.model.MainTab
import com.example.ui.components.UserAccountSection
import com.example.ui.components.ExoPlayerView
import com.example.ui.components.MediaDetailSheet
import com.example.ui.screens.*
import com.example.ui.theme.FlixTvTheme
import com.example.ui.theme.NetflixRed
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlayingMedia
import com.example.util.LocalizationHelper

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private fun playWelcomeSound() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run_sound", true)
        
        if (isFirstRun) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Try to play from resources if it exists
                    val resId = resources.getIdentifier("welcome_sound", "raw", packageName)
                    if (resId != 0) {
                        val mediaPlayer = android.media.MediaPlayer.create(this@MainActivity, resId)
                        mediaPlayer?.setOnCompletionListener { it.release() }
                        mediaPlayer?.start()
                    } else {
                        // If no file, play a cinematic 'ta-dum' sequence as a placeholder
                        val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                        toneG.startTone(android.media.ToneGenerator.TONE_DTMF_D, 150)
                        kotlinx.coroutines.delay(200)
                        toneG.startTone(android.media.ToneGenerator.TONE_DTMF_D, 300)
                    }
                    prefs.edit().putBoolean("is_first_run_sound", false).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        playWelcomeSound()

        setContent {
                val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
                val videoPlayerType by viewModel.videoPlayerType.collectAsStateWithLifecycle()
                val accentColorHex by viewModel.accentColorHex.collectAsStateWithLifecycle()
                val streamQuality by viewModel.streamQuality.collectAsStateWithLifecycle()
                val autoRefreshEpg by viewModel.autoRefreshEpg.collectAsStateWithLifecycle()

                val parsedAccentColor = remember(accentColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(accentColorHex))
                    } catch (e: Exception) {
                        Color(0xFFE50914) // Fallback to NetflixRed
                    }
                }

                var showSettingsDialog by remember { mutableStateOf(false) }

            FlixTvTheme(accentColor = parsedAccentColor) {
                val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
                val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

                val channels by viewModel.channels.collectAsStateWithLifecycle()
                val movies by viewModel.movies.collectAsStateWithLifecycle()
                val series by viewModel.series.collectAsStateWithLifecycle()
                
                val liveCategories by viewModel.liveCategories.collectAsStateWithLifecycle()
                val vodCategories by viewModel.vodCategories.collectAsStateWithLifecycle()
                val seriesCategories by viewModel.seriesCategories.collectAsStateWithLifecycle()
                
                val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()

                val favChannels by viewModel.favoriteChannels.collectAsStateWithLifecycle()
                val favMovies by viewModel.favoriteMovies.collectAsStateWithLifecycle()
                val favSeries by viewModel.favoriteSeries.collectAsStateWithLifecycle()

                val activePlaylist by viewModel.activePlaylist.collectAsStateWithLifecycle()
                val playlists by viewModel.playlists.collectAsStateWithLifecycle()
                val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
                val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

                val loggedInAccount by viewModel.loggedInAccount.collectAsStateWithLifecycle()
                val authError by viewModel.authError.collectAsStateWithLifecycle()
                val customFolders by viewModel.customFolders.collectAsStateWithLifecycle()

                val profiles by viewModel.profiles.collectAsStateWithLifecycle()
                val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
                val downloads by viewModel.downloads.collectAsStateWithLifecycle()
                val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()

                val currentlyPlaying by viewModel.currentlyPlaying.collectAsStateWithLifecycle()
                val mediaDetail by viewModel.mediaDetail.collectAsStateWithLifecycle()
                val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()

                if (showSplash) {
                    SplashScreen(onFinish = { viewModel.finishSplash() })
                    return@FlixTvTheme
                }

                val isAuthRequired = loggedInAccount == null

                var showGlobalCastDialog by remember { mutableStateOf(false) }
                var connectedCastDeviceName by remember { mutableStateOf<String?>(null) }
                var showProfileDialog by remember { mutableStateOf(false) }
                var showDownloadsDialog by remember { mutableStateOf(false) }

                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(userMessage) {
                    userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearUserMessage()
                    }
                }

                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val isWideScreen = configuration.screenWidthDp >= 600
                var isDrawerExpanded by remember { mutableStateOf(false) }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        if (currentlyPlaying == null && !isAuthRequired) {
                            TopAppBar(
                                title = {
                                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Text(
                                            text = "FLIX",
                                            color = parsedAccentColor,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 22.sp
                                        )
                                        Text(
                                            text = "TV",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = parsedAccentColor.copy(alpha = 0.2f),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Streaming",
                                                color = parsedAccentColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { showGlobalCastDialog = true }) {
                                        Icon(
                                            imageVector = if (connectedCastDeviceName != null) Icons.Default.CastConnected else Icons.Default.Cast,
                                            contentDescription = "Google Cast",
                                            tint = if (connectedCastDeviceName != null) parsedAccentColor else Color.LightGray
                                        )
                                    }

                                    IconButton(onClick = { showDownloadsDialog = true }) {
                                        BadgedBox(
                                            badge = {
                                                if (downloads.isNotEmpty()) {
                                                    Badge(containerColor = parsedAccentColor) {
                                                        Text(downloads.size.toString(), color = Color.White)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = "التنزيلات أوفلاين",
                                                tint = Color.LightGray
                                            )
                                        }
                                    }

                                    IconButton(onClick = { showProfileDialog = true }) {
                                        val profColor = try {
                                            activeProfile?.avatarColorHex?.let { Color(android.graphics.Color.parseColor(it)) } ?: parsedAccentColor
                                        } catch (e: Exception) {
                                            parsedAccentColor
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(profColor),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Text(
                                                text = activeProfile?.name?.take(1)?.uppercase() ?: "P",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }



                                    // Dedicated Content Refresh Button
                                    IconButton(onClick = { viewModel.refreshActivePlaylist() }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "تحديث المحتوى 🔄",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Dedicated Settings Button
                                    IconButton(onClick = { showSettingsDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "الإعدادات",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = com.example.ui.theme.DarkBackground,
                                    titleContentColor = Color.White
                                )
                            )
                        }
                    },
                    bottomBar = {
                        if (currentlyPlaying == null && !isWideScreen && !isAuthRequired) {
                            NavigationBar(
                                containerColor = com.example.ui.theme.DarkCardBg,
                                contentColor = Color.White,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                NavigationBarItem(
                                    selected = selectedTab == MainTab.HOME,
                                    onClick = { viewModel.setTab(MainTab.HOME) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = LocalizationHelper.translate("home", appLanguage)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = parsedAccentColor,
                                        selectedTextColor = parsedAccentColor,
                                        indicatorColor = parsedAccentColor.copy(alpha = 0.15f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == MainTab.LIVE_TV,
                                    onClick = { viewModel.setTab(MainTab.LIVE_TV) },
                                    icon = { Icon(Icons.Default.Tv, contentDescription = LocalizationHelper.translate("live_tv", appLanguage)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = parsedAccentColor,
                                        selectedTextColor = parsedAccentColor,
                                        indicatorColor = parsedAccentColor.copy(alpha = 0.15f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == MainTab.MOVIES,
                                    onClick = { viewModel.setTab(MainTab.MOVIES) },
                                    icon = { Icon(Icons.Default.Movie, contentDescription = LocalizationHelper.translate("movies", appLanguage)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = parsedAccentColor,
                                        selectedTextColor = parsedAccentColor,
                                        indicatorColor = parsedAccentColor.copy(alpha = 0.15f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == MainTab.SERIES,
                                    onClick = { viewModel.setTab(MainTab.SERIES) },
                                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = LocalizationHelper.translate("series", appLanguage)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = parsedAccentColor,
                                        selectedTextColor = parsedAccentColor,
                                        indicatorColor = parsedAccentColor.copy(alpha = 0.15f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == MainTab.FAVORITES,
                                    onClick = { viewModel.setTab(MainTab.FAVORITES) },
                                    icon = { Icon(Icons.Default.Favorite, contentDescription = LocalizationHelper.translate("favorites", appLanguage)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = parsedAccentColor,
                                        selectedTextColor = parsedAccentColor,
                                        indicatorColor = parsedAccentColor.copy(alpha = 0.15f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )

                                NavigationBarItem(
                                    selected = selectedTab == MainTab.USER_ACCOUNT,
                                    onClick = { viewModel.setTab(MainTab.USER_ACCOUNT) },
                                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = LocalizationHelper.translate("my_account", appLanguage)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = parsedAccentColor,
                                        selectedTextColor = parsedAccentColor,
                                        indicatorColor = parsedAccentColor.copy(alpha = 0.15f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        BackHandler(enabled = selectedTab != MainTab.HOME && !isAuthRequired && currentlyPlaying == null) { viewModel.setTab(MainTab.HOME) }
                        val isDrawerVisible = isWideScreen && currentlyPlaying == null && !isAuthRequired && selectedTab == MainTab.HOME
                        val contentPaddingStart = if (isDrawerVisible) 72.dp else 0.dp

                        if (isDrawerVisible) {
                            val drawerWidth by animateDpAsState(
                                targetValue = if (isDrawerExpanded) 220.dp else 72.dp,
                                label = "drawerWidth"
                            )
                            Surface(
                                color = com.example.ui.theme.DarkCardBg,
                                contentColor = Color.White,
                                modifier = Modifier
                                    .width(drawerWidth)
                                    .fillMaxHeight()
                                    .align(androidx.compose.ui.Alignment.CenterStart)
                                    .zIndex(1f)
                                    ,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalAlignment = androidx.compose.ui.Alignment.Start
                                ) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    // Custom Drawer Header / Icon Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isDrawerExpanded = !isDrawerExpanded }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu",
                                            tint = parsedAccentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (isDrawerExpanded) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                                Text(
                                                    text = "FLIX",
                                                    color = parsedAccentColor,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 18.sp
                                                )
                                                Text(
                                                    text = "TV",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Navigation Items
                                    val navItems = listOf(
                                        MainTab.HOME to (Icons.Default.Home to "home"),
                                        MainTab.LIVE_TV to (Icons.Default.Tv to "live_tv"),
                                        MainTab.MOVIES to (Icons.Default.Movie to "movies"),
                                        MainTab.SERIES to (Icons.Default.VideoLibrary to "series"),
                                        MainTab.FAVORITES to (Icons.Default.Favorite to "favorites"),
                                        MainTab.USER_ACCOUNT to (Icons.Default.AccountCircle to "my_account")
                                    )

                                    navItems.forEach { (tab, info) ->
                                        val isSelected = selectedTab == tab
                                        var isItemFocused by remember { mutableStateOf(false) }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isSelected -> parsedAccentColor.copy(alpha = 0.15f)
                                                        isItemFocused -> Color.White.copy(alpha = 0.08f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .onFocusChanged { isItemFocused = it.isFocused }
                                                .clickable { viewModel.setTab(tab) }
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = androidx.compose.ui.Alignment.CenterStart
                                        ) {
                                            Row(
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = info.first,
                                                    contentDescription = LocalizationHelper.translate(info.second, appLanguage),
                                                    tint = if (isSelected || isItemFocused) parsedAccentColor else Color.LightGray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                if (isDrawerExpanded) {
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = LocalizationHelper.translate(info.second, appLanguage),
                                                        color = if (isSelected || isItemFocused) parsedAccentColor else Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = contentPaddingStart)
                        ) {
                        if (isAuthRequired) {
                            UserAccountSection(
                                userAccount = null,
                                authError = authError,
                                isSyncing = isSyncing,
                                importProgress = importProgress,
                                playlistsEmpty = playlists.isEmpty(),
                                onLogin = { identifier, pass, adminUrl ->
                                    viewModel.loginUser(identifier, pass, adminUrl)
                                },
                                onRegister = { email, username, pass, phone ->
                                    viewModel.registerUser(email, username, pass, phone)
                                },
                                onLogout = { viewModel.logoutUser() },
                                onImportXtream = { username, pass ->
                                    viewModel.importDefaultSubscription(username, pass)
                                },
                                onCheckActivation = {
                                    viewModel.checkSubscription()
                                },
                                onUpdateAdminUrl = { url ->
                                    viewModel.updateAdminServerUrl(url)
                                },
                                onImportAdminXtream = {
                                    viewModel.importAdminXtreamSubscription()
                                }
                            )
                        } else {
                            val currentAccount = loggedInAccount
                            if (currentAccount != null && !currentAccount.isActivated && selectedTab != MainTab.USER_ACCOUNT) {
                            // Locked Screen for Non-Activated Accounts
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = parsedAccentColor,
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Text(
                                    text = "الاشتراك غير نشط 🔒",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "بانتظار تفعيل اشتراكك من قبل الإدارة لتتمكن من الوصول لكامل محتوى البث التلفزيوني والأفلام والمسلسلات.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Surface(
                                    color = parsedAccentColor.copy(alpha = 0.15f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, parsedAccentColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth().widthIn(max = 350.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        Text("كود التفعيل الخاص بك", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentAccount.activationCode,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(28.dp))
                                Button(
                                    onClick = { viewModel.setTab(MainTab.USER_ACCOUNT) },
                                    colors = ButtonDefaults.buttonColors(containerColor = parsedAccentColor),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(min = 220.dp)
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("صفحة الحساب والتفعيل", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            when (selectedTab) {
                                MainTab.HOME -> HomeScreen(
                                    channels = channels,
                                    movies = movies,
                                    series = series,
                                    watchHistory = watchHistory,
                                    activePlaylist = activePlaylist,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    onCategorySelect = { viewModel.setCategory(it) },
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onPlayMedia = { viewModel.playMedia(it) },
                                    onMovieClick = { viewModel.openMovieDetail(it) },
                                    onSeriesClick = { viewModel.openSeriesDetail(it) },
                                    onToggleChannelFav = { viewModel.toggleChannelFavorite(it) },
                                    onToggleMovieFav = { viewModel.toggleMovieFavorite(it) },
                                    onToggleSeriesFav = { viewModel.toggleSeriesFavorite(it) },
                                    liveCategories = liveCategories,
                                    vodCategories = vodCategories,
                                    seriesCategories = seriesCategories,
                                    onLoadCategoryStreams = { catId, type -> viewModel.loadStreamsByCategory(catId, type) }
                                )

                                MainTab.LIVE_TV -> LiveTvScreen(
                                    channels = channels,
                                    xtreamCategories = liveCategories,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { viewModel.setCategory(it) },
                                    onPlayChannel = { viewModel.playMedia(it) },
                                    onToggleFavorite = { viewModel.toggleChannelFavorite(it) },
                                    onLoadCategoryStreams = { viewModel.loadStreamsByCategory(it, "live") },
                                    onExit = { viewModel.setTab(MainTab.HOME) }
                                )

                                MainTab.MOVIES -> MoviesScreen(
                                    movies = movies,
                                    xtreamCategories = vodCategories,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { viewModel.setCategory(it) },
                                    onMovieClick = { viewModel.openMovieDetail(it) },
                                    onToggleFavorite = { viewModel.toggleMovieFavorite(it) },
                                    onLoadCategoryStreams = { viewModel.loadStreamsByCategory(it, "vod") }
                                )

                                MainTab.SERIES -> SeriesScreen(
                                    seriesList = series,
                                    xtreamCategories = seriesCategories,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { viewModel.setCategory(it) },
                                    onSeriesClick = { viewModel.openSeriesDetail(it) },
                                    onToggleFavorite = { viewModel.toggleSeriesFavorite(it) },
                                    onLoadCategoryStreams = { viewModel.loadStreamsByCategory(it, "series") }
                                )

                                MainTab.FAVORITES -> FavoritesScreen(
                                    favChannels = favChannels,
                                    favMovies = favMovies,
                                    favSeries = favSeries,
                                    allChannels = channels,
                                    onPlayChannel = { viewModel.playMedia(it) },
                                    onMovieClick = { viewModel.openMovieDetail(it) },
                                    onSeriesClick = { viewModel.openSeriesDetail(it) },
                                    onToggleChannelFav = { viewModel.toggleChannelFavorite(it) },
                                    onToggleMovieFav = { viewModel.toggleMovieFavorite(it) },
                                    onToggleSeriesFav = { viewModel.toggleSeriesFavorite(it) }
                                )

                                MainTab.USER_ACCOUNT -> UserAccountSection(
                                    userAccount = loggedInAccount,
                                    authError = authError,
                                    isSyncing = isSyncing,
                                    importProgress = importProgress,
                                    playlistsEmpty = playlists.isEmpty(),
                                    onLogin = { identifier, pass, adminUrl ->
                                        viewModel.loginUser(identifier, pass, adminUrl)
                                    },
                                    onRegister = { email, username, pass, phone ->
                                        viewModel.registerUser(email, username, pass, phone)
                                    },
                                    onLogout = { viewModel.logoutUser() },
                                    onImportXtream = { username, pass ->
                                        viewModel.importDefaultSubscription(username, pass)
                                    },
                                    onCheckActivation = {
                                        viewModel.checkSubscription()
                                    },
                                    onUpdateAdminUrl = { url ->
                                        viewModel.updateAdminServerUrl(url)
                                    },
                                    onImportAdminXtream = {
                                    viewModel.importAdminXtreamSubscription()
                                }
                            )
                        }
                    }
                }

                        // Full Screen ExoPlayer Overlay
                        androidx.compose.animation.AnimatedVisibility(
                            visible = currentlyPlaying != null,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            currentlyPlaying?.let { media ->
                                androidx.compose.runtime.key(media.streamUrl) {
                                    ExoPlayerView(
                                        mediaUrl = media.streamUrl,
                                        title = media.title,
                                        type = media.type,
                                        channelList = media.channelList,
                                        initialPlaybackPositionMs = media.startPositionMs,
                                        onClose = { viewModel.stopMedia() },
                                        onProgressUpdate = { pos, dur ->
                                            viewModel.recordProgress(media.id, pos, dur)
                                        },
                                        onChannelSelect = { ch ->
                                            viewModel.playMedia(
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
                                        }
                                    )
                                }
                            }
                        }

                        // Media Detail Bottom Sheet Modal
                        mediaDetail?.let { detail ->
                            MediaDetailSheet(
                                movie = detail.movie,
                                series = detail.series,
                                episodes = detail.episodes,
                                onDismiss = { viewModel.closeMediaDetail() },
                                onPlayMovie = { mov ->
                                    viewModel.playMedia(
                                        PlayingMedia(
                                            id = mov.id,
                                            title = mov.title,
                                            streamUrl = mov.streamUrl,
                                            posterUrl = mov.posterUrl,
                                            category = mov.category,
                                            type = "MOVIE"
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
                                            type = "EPISODE"
                                        )
                                    )
                                },
                                onToggleFavoriteMovie = { mov -> viewModel.toggleMovieFavorite(mov) },
                                onToggleFavoriteSeries = { ser -> viewModel.toggleSeriesFavorite(ser) },
                                onDownloadMovie = { mov -> viewModel.startDownload(mov) }
                            )
                        }

                        // Profile Management Dialog
                        if (showProfileDialog) {
                            com.example.ui.components.ProfileManagementDialog(
                                profiles = profiles,
                                activeProfile = activeProfile,
                                onSwitchProfile = { prof -> viewModel.switchProfile(prof) },
                                onCreateProfile = { name, color, isKids, pin -> viewModel.createProfile(name, color, isKids, pin) },
                                onDeleteProfile = { prof -> viewModel.deleteProfile(prof) },
                                onDismiss = { showProfileDialog = false }
                            )
                        }

                        // Offline Downloads Dialog
                        if (showDownloadsDialog) {
                            com.example.ui.components.OfflineDownloadsDialog(
                                downloads = downloads,
                                onPlayOffline = { item ->
                                    viewModel.playMedia(
                                        PlayingMedia(
                                            id = item.id,
                                            title = "${item.title} (أوفلاين)",
                                            streamUrl = item.streamUrl,
                                            posterUrl = item.posterUrl,
                                            type = item.itemType
                                        )
                                    )
                                },
                                onDeleteDownload = { dlId -> viewModel.deleteDownload(dlId) },
                                onDismiss = { showDownloadsDialog = false }
                            )
                        }

                        // Global Google Cast Dialog
                        if (showGlobalCastDialog) {
                            com.example.ui.components.GoogleCastDialog(
                                currentConnectedDevice = connectedCastDeviceName,
                                mediaTitle = currentlyPlaying?.title ?: "",
                                onConnectDevice = { devName ->
                                    connectedCastDeviceName = devName
                                    showGlobalCastDialog = false
                                },
                                onDisconnect = {
                                    connectedCastDeviceName = null
                                    showGlobalCastDialog = false
                                },
                                onDismiss = { showGlobalCastDialog = false }
                            )
                        }

                        // App Settings Dialog
                        if (showSettingsDialog) {
                            com.example.ui.components.SettingsDialog(
                                currentLanguage = appLanguage,
                                currentVideoPlayer = videoPlayerType,
                                currentAccentColorHex = accentColorHex,
                                currentStreamQuality = streamQuality,
                                currentAutoRefresh = autoRefreshEpg,
                                activePlaylistName = activePlaylist?.name,
                                onLanguageChange = { lang -> viewModel.setAppLanguage(lang) },
                                onVideoPlayerChange = { player -> viewModel.setVideoPlayerType(player) },
                                onAccentColorChange = { hex -> viewModel.setAccentColorHex(hex) },
                                onStreamQualityChange = { quality -> viewModel.setStreamQuality(quality) },
                                onAutoRefreshChange = { enabled -> viewModel.setAutoRefreshEpg(enabled) },
                                onClearCache = { viewModel.clearAppCache() },
                                onDismiss = { showSettingsDialog = false }
                            )
                        }

                        // Global Sync Progress Overlay
                        if (isSyncing) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.85f),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { importProgress / 100f },
                                            modifier = Modifier.size(100.dp),
                                            color = parsedAccentColor,
                                            strokeWidth = 8.dp,
                                            trackColor = Color.White.copy(alpha = 0.1f)
                                        )
                                        Text(
                                            text = "$importProgress%",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "جاري استيراد وتحليل المحتوى...",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "يرجى الانتظار، يتم الآن مزامنة المحتوى لتجربة مشاهدة مثالية.",
                                        color = Color.Gray,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
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
}
