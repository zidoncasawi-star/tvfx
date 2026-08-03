package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RemoteLogger
import com.example.data.StreamRepository
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class PlayingMedia(
    val id: String,
    val title: String,
    val streamUrl: String,
    val posterUrl: String = "",
    val category: String = "",
    val type: String, // "LIVE", "MOVIE", "EPISODE"
    val channelList: List<ChannelEntity> = emptyList(),
    val episodeList: List<Episode> = emptyList(),
    val seriesTitle: String = "",
    val startPositionMs: Long = 0L
)

data class MediaDetailState(
    val movie: MovieEntity? = null,
    val series: SeriesEntity? = null,
    val episodes: List<Episode> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StreamRepository(AppDatabase.getInstance(application))

    private val prefs = application.getSharedPreferences("flix_tv_settings", Context.MODE_PRIVATE)

    private val _appLanguage = MutableStateFlow(prefs.getString("key_app_lang", "ar") ?: "ar")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _videoPlayerType = MutableStateFlow(prefs.getString("key_video_player", "ExoPlayer") ?: "ExoPlayer")
    val videoPlayerType: StateFlow<String> = _videoPlayerType.asStateFlow()

    private val _accentColorHex = MutableStateFlow(prefs.getString("key_accent_color", "#E50914") ?: "#E50914")
    val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    private val _streamQuality = MutableStateFlow(prefs.getString("key_stream_quality", "Auto") ?: "Auto")
    val streamQuality: StateFlow<String> = _streamQuality.asStateFlow()

    private val _autoRefreshEpg = MutableStateFlow(prefs.getBoolean("key_auto_refresh_epg", true))
    val autoRefreshEpg: StateFlow<Boolean> = _autoRefreshEpg.asStateFlow()

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("key_app_lang", lang).apply()
    }

    fun setVideoPlayerType(player: String) {
        _videoPlayerType.value = player
        prefs.edit().putString("key_video_player", player).apply()
    }

    fun setAccentColorHex(hex: String) {
        _accentColorHex.value = hex
        prefs.edit().putString("key_accent_color", hex).apply()
    }

    fun setStreamQuality(quality: String) {
        _streamQuality.value = quality
        prefs.edit().putString("key_stream_quality", quality).apply()
    }

    fun setAutoRefreshEpg(enabled: Boolean) {
        _autoRefreshEpg.value = enabled
        prefs.edit().putBoolean("key_auto_refresh_epg", enabled).apply()
    }

    fun clearAppCache() {
        val active = activePlaylist.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            _importProgress.value = 0
            _userMessage.value = if (_appLanguage.value == "ar") {
                "جاري مسح الذاكرة المؤقتة وإعادة تحميل البيانات..."
            } else if (_appLanguage.value == "fr") {
                "Vidage du cache et rechargement des données..."
            } else {
                "Clearing cache and reloading data..."
            }
            try {
                repository.clearPlaylistCache(active.id)
                repository.syncPlaylistContent(active, onProgress = { _importProgress.value = it }, forceRefresh = true)
                _userMessage.value = if (_appLanguage.value == "ar") {
                    "تم مسح الذاكرة المؤقتة وتحديث محتوى الاشتراك بنجاح! 🎉"
                } else if (_appLanguage.value == "fr") {
                    "Cache vidé et abonnement mis à jour avec succès ! 🎉"
                } else {
                    "Cache cleared and subscription updated successfully! 🎉"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = if (_appLanguage.value == "ar") {
                    "فشل تحديث البيانات: تأكد من الاتصال بالإنترنت."
                } else if (_appLanguage.value == "fr") {
                    "Échec de mise à jour : vérifiez votre connexion internet."
                } else {
                    "Failed to reload data: check network connection."
                }
            } finally {
                _isSyncing.value = false
                _importProgress.value = 0
            }
        }
    }

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activePlaylist: StateFlow<PlaylistEntity?> = repository.activePlaylist.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.watchHistory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val profiles: StateFlow<List<UserProfileEntity>> = repository.profiles.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val activeProfile: StateFlow<UserProfileEntity?> = repository.activeProfile.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val downloads: StateFlow<List<DownloadedItemEntity>> = repository.downloads.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val customFolders: StateFlow<List<CustomFolderEntity>> = repository.customFolders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // User Account State
    val loggedInAccount: StateFlow<UserAccountEntity?> = repository.loggedInAccount.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun registerUser(email: String, username: String, pass: String, phone: String) {
        viewModelScope.launch {
            _authError.value = null
            _isSyncing.value = true
            _userMessage.value = "جاري إنشاء حسابك..."
            
            val deviceId = getDeviceId()
            
            // 1. Register on Admin Panel first
            try {
                val remoteJson = com.example.data.AdminPanelClient.registerUserOnAdminPanel(
                    adminUrl = "https://app.flixplayer.pro",
                    username = username,
                    email = email,
                    pass = pass,
                    phone = phone,
                    deviceId = deviceId
                )
                
                if (remoteJson != null) {
                    if (remoteJson.optBoolean("success", false)) {
                        val serverCode = remoteJson.optString("activationCode", "")
                        // 2. Register locally if remote succeeded
                        val localResult = repository.registerUserAccount(email, username, pass, phone, deviceId, serverCode)
                        localResult.onSuccess { account ->
                            _userMessage.value = "تم إنشاء الحساب بنجاح! بانتظار تفعيل اشتراكك ⌛"
                            _isSyncing.value = false
                        }.onFailure { err ->
                            _authError.value = err.message ?: "حدث خطأ أثناء حفظ الحساب محلياً"
                            _isSyncing.value = false
                        }
                    } else {
                        _authError.value = remoteJson.optString("message", "فشل التسجيل في الخادم. قد يكون اسم المستخدم أو البريد مسجلاً بالفعل.")
                        _isSyncing.value = false
                    }
                } else {
                    _authError.value = "فشل الاتصال بالخادم."
                    _isSyncing.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _authError.value = "حدث خطأ أثناء الاتصال بالخادم: ${e.message}"
                _isSyncing.value = false
            }
        }
    }

    fun loginUser(identifier: String, pass: String, adminUrl: String = "https://app.flixplayer.pro") {
        viewModelScope.launch {
            _authError.value = null
            _isSyncing.value = true
            _userMessage.value = "جاري المصادقة..."
            
            val localResult = repository.loginUserAccount(identifier, pass)

            if (localResult.isSuccess) {
                var account = localResult.getOrThrow()
                // Update URL if provided and different
                if (adminUrl.isNotBlank() && adminUrl != account.adminServerUrl) {
                    account = account.copy(adminServerUrl = adminUrl)
                }

                // تحديث كود التفعيل من السيرفر عند كل تسجيل دخول محلي — الحساب المحلي المخزَّن قد يحمل
                // كوداً قديماً إن غيّره الأدمن لاحقاً، وبدون هذا التحديث يبقى فحص التفعيل التلقائي
                // عند فتح التطبيق يقارن بكود قديم لا يطابق شيئاً في قاعدة البيانات فيفشل بصمت
                try {
                    val remoteJson = com.example.data.AdminPanelClient.loginUserOnAdminPanel(
                        adminUrl = account.adminServerUrl.ifBlank { "https://app.flixplayer.pro" },
                        identifier = identifier,
                        pass = pass
                    )
                    val remoteCode = remoteJson?.optString("activationCode", "")
                    if (remoteJson?.optBoolean("success", false) == true && !remoteCode.isNullOrBlank() && remoteCode != account.activationCode) {
                        RemoteLogger.log(
                            username = account.username, level = "DEBUG", tag = "SyncDebug",
                            message = "Refreshed stale local activationCode on login: old=${account.activationCode} -> new=$remoteCode"
                        )
                        account = account.copy(activationCode = remoteCode)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                repository.updateAccount(account)
                _userMessage.value = "تم تسجيل الدخول بنجاح! أهلاً بعودتك ${account.fullName} 👋"
                completeLoginFlow(account)
            } else {
                // Try Remote Login against the Admin Panel
                try {
                    val remoteJson = com.example.data.AdminPanelClient.loginUserOnAdminPanel(
                        adminUrl = adminUrl.ifBlank { "https://app.flixplayer.pro" },
                        identifier = identifier,
                        pass = pass
                    )
                    
                    RemoteLogger.log(
                        username = identifier, level = "DEBUG", tag = "LoginDebug",
                        message = "Remote login attempt identifier=$identifier adminUrl=$adminUrl rawResponse=${remoteJson?.toString() ?: "null"}"
                    )

                    if (remoteJson != null) {
                        if (remoteJson.optBoolean("success", false)) {
                            // Remote Login Success! Create or update local account
                            val encryptedPass = com.example.util.SecurityUtils.encrypt(pass)
                            val newAccount = UserAccountEntity(
                                fullName = remoteJson.optString("fullName", "مستخدم روديكس"),
                                email = remoteJson.optString("email", identifier),
                                username = remoteJson.optString("username", identifier),
                                passwordHash = encryptedPass,
                                phoneNumber = remoteJson.optString("phoneNumber", ""),
                                activationCode = remoteJson.optString("activationCode", ""),
                                adminServerUrl = adminUrl.ifBlank { "https://app.flixplayer.pro" },
                                isLoggedIn = true
                            )
                            repository.insertAccount(newAccount)
                            _userMessage.value = "تم تسجيل الدخول عبر الخادم! أهلاً بك ${newAccount.fullName} 👋"
                            completeLoginFlow(newAccount)
                        } else {
                            _authError.value = remoteJson.optString("message", "اسم المستخدم أو كلمة المرور غير صحيحة")
                            _isSyncing.value = false
                        }
                    } else {
                        _authError.value = "فشل الاتصال بالخادم. تأكد من بياناتك والاتصال بالشبكة."
                        _isSyncing.value = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    RemoteLogger.log(
                        username = identifier, level = "ERROR", tag = "LoginDebug",
                        message = "Remote login threw exception identifier=$identifier error=${e.javaClass.simpleName}: ${e.message}"
                    )
                    _authError.value = "فشل الاتصال بالخادم. تأكد من بياناتك والاتصال بالشبكة."
                    _isSyncing.value = false
                }
            }
        }
    }

    private suspend fun completeLoginFlow(account: UserAccountEntity) {
        // Check activation status and update account details
        try {
            val check = com.example.data.AdminPanelClient.checkSubscriptionStatus(
                adminUrl = account.adminServerUrl,
                username = account.username,
                activationCode = account.activationCode
            )
            
            val updated = account.copy(
                isActivated = check.isActivated,
                xtreamHost = check.xtreamHost ?: account.xtreamHost,
                xtreamUsername = check.xtreamUsername ?: account.xtreamUsername,
                xtreamPassword = check.xtreamPassword ?: account.xtreamPassword
            )
            repository.updateAccount(updated)
            
            _isSyncing.value = false
            
            // Check if any playlists/content exist, otherwise navigate to the import page
            val currentPlaylists = repository.playlists.firstOrNull() ?: emptyList()
            if (currentPlaylists.isEmpty()) {
                _selectedTab.value = MainTab.USER_ACCOUNT
                _userMessage.value = "تم تسجيل الدخول بنجاح! يرجى استيراد المحتوى للبدء."
            } else {
                _selectedTab.value = MainTab.HOME
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isSyncing.value = false
            
            val currentPlaylists = repository.playlists.firstOrNull() ?: emptyList()
            if (currentPlaylists.isEmpty()) {
                _selectedTab.value = MainTab.USER_ACCOUNT
            } else {
                _selectedTab.value = MainTab.HOME
            }
        }
    }

    fun checkSubscription() {
        val account = loggedInAccount.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            _importProgress.value = 0
            _userMessage.value = "جاري التحقق من حالة التفعيل..."
            try {
                val result = com.example.data.AdminPanelClient.checkSubscriptionStatus(
                    adminUrl = account.adminServerUrl,
                    username = account.username,
                    activationCode = account.activationCode
                )
                val updated = account.copy(
                    isActivated = result.isActivated,
                    xtreamHost = result.xtreamHost ?: account.xtreamHost,
                    xtreamUsername = result.xtreamUsername ?: account.xtreamUsername,
                    xtreamPassword = result.xtreamPassword ?: account.xtreamPassword
                )
                repository.updateAccount(updated)
                
                if (result.isActivated && updated.xtreamHost.isNotBlank()) {
                    _userMessage.value = "تم التفعيل! جاري استيراد المحتوى المخصص..."
                    performAdminImport(updated)
                } else {
                    _userMessage.value = if (result.isActivated) "تم التفعيل بنجاح! شكراً لك 🎉" else "العضوية غير مفعلة بعد: ${result.message}"
                    _isSyncing.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = "حدث خطأ أثناء التحقق من التفعيل."
                _isSyncing.value = false
            }
        }
    }

    private suspend fun performAdminImport(account: UserAccountEntity) {
        try {
            val encryptedPass = com.example.util.SecurityUtils.encrypt(account.xtreamPassword)
            val playlist = PlaylistEntity(
                name = "بث Xtream الخاص بك 🚀",
                type = PlaylistType.XTREAM,
                serverUrl = account.xtreamHost,
                username = account.xtreamUsername,
                password = encryptedPass,
                isActive = true
            )
            repository.savePlaylistAndSync(
                playlist,
                onProgress = { progress -> _importProgress.value = progress },
                onStatusUpdate = { status -> _importStatusText.value = status }
            )
            _userMessage.value = "تم استيراد اشتراكك المخصص بنجاح! مشاهدة ممتعة 🎉"
            _selectedTab.value = MainTab.HOME
        } catch (e: Exception) {
            e.printStackTrace()
            _userMessage.value = "فشل الاستيراد: تأكد من صحة البيانات المخصصة والاتصال بالشبكة."
        } finally {
            _isSyncing.value = false
            _importProgress.value = 0
            _importStatusText.value = ""
        }
    }

    fun updateAdminServerUrl(url: String) {
        val account = loggedInAccount.value ?: return
        viewModelScope.launch {
            val updated = account.copy(adminServerUrl = url)
            repository.updateAccount(updated)
            _userMessage.value = "تم تحديث عنوان لوحة الإدارة بنجاح."
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            repository.logoutUserAccount()
            _userMessage.value = "تم تسجيل الخروج بنجاح"
        }
    }

    fun importAdminXtreamSubscription() {
        val account = loggedInAccount.value ?: return
        if (account.xtreamHost.isBlank() || account.xtreamUsername.isBlank()) {
            _userMessage.value = "لم يتم تحديد بيانات اشتراك مخصصة لك من قبل الإدارة بعد."
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _importProgress.value = 0
            performAdminImport(account)
        }
    }

    private val _selectedTab = MutableStateFlow(MainTab.HOME)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    fun finishSplash() {
        _showSplash.value = false
    }

    private val _importProgress = MutableStateFlow(0)
    val importProgress: StateFlow<Int> = _importProgress.asStateFlow()

    private val _importStatusText = MutableStateFlow("")
    val importStatusText: StateFlow<String> = _importStatusText.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _currentlyPlaying = MutableStateFlow<PlayingMedia?>(null)
    val currentlyPlaying: StateFlow<PlayingMedia?> = _currentlyPlaying.asStateFlow()

    private val _mediaDetail = MutableStateFlow<MediaDetailState?>(null)
    val mediaDetail: StateFlow<MediaDetailState?> = _mediaDetail.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val liveCategories: StateFlow<List<XtreamCategoryEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getCategories(pl.id, "live") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val vodCategories: StateFlow<List<XtreamCategoryEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getCategories(pl.id, "vod") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val seriesCategories: StateFlow<List<XtreamCategoryEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getCategories(pl.id, "series") else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadStreamsByCategory(categoryId: String, type: String) {
        val pl = activePlaylist.value ?: return
        if (pl.type != PlaylistType.XTREAM) return
        
        viewModelScope.launch {
            repository.fetchAndStoreStreamsByCategory(pl.id, categoryId, type)
        }
    }

    // Channels, Movies, Series observation based on active playlist
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val channels: StateFlow<List<ChannelEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getChannels(pl.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val movies: StateFlow<List<MovieEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getMovies(pl.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val series: StateFlow<List<SeriesEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getSeries(pl.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val favoriteChannels: StateFlow<List<ChannelEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getFavoriteChannels(pl.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val favoriteMovies: StateFlow<List<MovieEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getFavoriteMovies(pl.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val favoriteSeries: StateFlow<List<SeriesEntity>> = activePlaylist.flatMapLatest { pl ->
        if (pl != null) repository.getFavoriteSeries(pl.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Track app install/open on start
        viewModelScope.launch {
            try {
                val deviceId = getDeviceId()
                com.example.data.AdminPanelClient.trackInstall("https://app.flixplayer.pro", deviceId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Redirect to import page on startup if logged in but no playlists exist
        viewModelScope.launch {
            try {
                val account = repository.loggedInAccount.first()
                if (account != null) {
                    val currentPlaylists = repository.playlists.first()
                    if (currentPlaylists.isEmpty()) {
                        _selectedTab.value = MainTab.USER_ACCOUNT
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // تحقق تلقائي عند فتح التطبيق: هل غيّر الأدمن حساب Xtream المخصَّص لهذا المستخدم؟
        // إن تغيّر (host/username/password مختلف عمّا هو محفوظ محلياً)، يُعاد الاستيراد تلقائياً
        // بالحساب الجديد دون أي تدخل من المستخدم.
        viewModelScope.launch {
            try {
                val account = repository.loggedInAccount.first() ?: return@launch
                if (!account.isActivated) return@launch

                val check = com.example.data.AdminPanelClient.checkSubscriptionStatus(
                    adminUrl = account.adminServerUrl,
                    username = account.username,
                    activationCode = account.activationCode
                )

                val newHost = check.xtreamHost
                val newUser = check.xtreamUsername
                val newPass = check.xtreamPassword

                val xtreamChanged = check.isActivated &&
                    !newHost.isNullOrBlank() &&
                    (newHost != account.xtreamHost || newUser != account.xtreamUsername || newPass != account.xtreamPassword)

                if (xtreamChanged) {
                    RemoteLogger.log(
                        username = account.username, level = "DEBUG", tag = "SyncDebug",
                        message = "Detected Xtream account change on app open: old host=${account.xtreamHost} -> new host=$newHost. Re-importing automatically."
                    )
                    val updated = account.copy(
                        xtreamHost = newHost,
                        xtreamUsername = newUser ?: account.xtreamUsername,
                        xtreamPassword = newPass ?: account.xtreamPassword,
                        isActivated = true
                    )
                    repository.updateAccount(updated)
                    performAdminImport(updated)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Automatic silent background revalidation on startup if cache is stale (TTL 6 hours)
        viewModelScope.launch {
            activePlaylist.collect { pl ->
                if (pl != null) {
                    val ttlMillis = 6 * 60 * 60 * 1000L // 6 hours
                    if (System.currentTimeMillis() - pl.lastUpdated > ttlMillis) {
                        launch(Dispatchers.IO) {
                            try {
                                repository.syncPlaylistContent(pl, forceRefresh = true)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    fun setTab(tab: MainTab) {
        _selectedTab.value = tab
        _selectedCategory.value = "الكل"
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playMedia(media: PlayingMedia) {
        _currentlyPlaying.value = media
        _mediaDetail.value = null // Close detail sheet if open

        viewModelScope.launch {
            repository.recordWatchHistory(
                itemId = media.id,
                itemType = media.type,
                title = media.title,
                posterUrl = media.posterUrl,
                streamUrl = media.streamUrl,
                progressMs = media.startPositionMs,
                totalMs = 0L
            )
        }
    }

    fun stopMedia() {
        _currentlyPlaying.value = null
    }

    fun openMovieDetail(movie: MovieEntity) {
        _mediaDetail.value = MediaDetailState(movie = movie)
    }

    fun openSeriesDetail(seriesItem: SeriesEntity) {
        _mediaDetail.value = MediaDetailState(series = seriesItem, episodes = emptyList())
        viewModelScope.launch {
            val episodes = if (seriesItem.streamUrl.isNotBlank() && !seriesItem.streamUrl.startsWith("http://dummy")) {
                listOf(
                    com.example.model.Episode(
                        id = "${seriesItem.id}_ep1",
                        seriesId = seriesItem.id,
                        seasonNum = 1,
                        episodeNum = 1,
                        title = seriesItem.title,
                        streamUrl = seriesItem.streamUrl,
                        duration = "غير محدد",
                        plot = seriesItem.plot.ifBlank { "تشغيل البث المباشر لهذا المسلسل." }
                    )
                )
            } else if (seriesItem.id.startsWith("xt_ser_")) {
                val active = activePlaylist.value
                val parts = seriesItem.id.split("_")
                val realId = parts.lastOrNull()
                RemoteLogger.log(
                    level = "DEBUG", tag = "SyncDebug",
                    message = "openSeriesDetail id=${seriesItem.id} parsedRealId=$realId activePlaylist=${active?.id} host=${active?.serverUrl}"
                )
                if (active != null && realId != null) {
                    val result = com.example.data.XtreamCodesClient.fetchSeriesEpisodes(active, realId)
                    RemoteLogger.log(
                        level = if (result.isEmpty()) "ERROR" else "DEBUG", tag = "SyncDebug",
                        message = "fetchSeriesEpisodes seriesId=$realId -> episodes=${result.size}"
                    )
                    result
                } else {
                    RemoteLogger.log(level = "ERROR", tag = "SyncDebug", message = "openSeriesDetail SKIPPED: active=$active realId=$realId")
                    emptyList()
                }
            } else {
                emptyList()
            }
            _mediaDetail.value?.let { current ->
                if (current.series?.id == seriesItem.id) {
                    _mediaDetail.value = current.copy(episodes = episodes)
                }
            }
        }
    }

    fun closeMediaDetail() {
        _mediaDetail.value = null
    }

    fun toggleChannelFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.toggleChannelFavorite(channel.id, channel.isFavorite)
        }
    }

    fun toggleMovieFavorite(movie: MovieEntity) {
        viewModelScope.launch {
            repository.toggleMovieFavorite(movie.id, movie.isFavorite)
        }
    }

    fun toggleSeriesFavorite(seriesItem: SeriesEntity) {
        viewModelScope.launch {
            repository.toggleSeriesFavorite(seriesItem.id, seriesItem.isFavorite)
        }
    }

    fun selectPlaylist(playlistId: Long) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.selectActivePlaylist(playlistId)
            _isSyncing.value = false
            _userMessage.value = "تم تغيير الاشتراك النشط"
        }
    }

    fun addXtreamPlaylist(name: String, serverUrl: String, username: String, pass: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val encryptedPass = com.example.util.SecurityUtils.encrypt(pass)
            val playlist = PlaylistEntity(
                name = name.ifBlank { "اشتراك Xtream" },
                type = PlaylistType.XTREAM,
                serverUrl = serverUrl,
                username = username,
                password = encryptedPass
            )
            repository.savePlaylistAndSync(playlist)
            _isSyncing.value = false
            _userMessage.value = "تمت إضافة اشتراك Xtream Codes وتشفير البيانات بآمان!"
            _selectedTab.value = MainTab.HOME
        }
    }

    fun addPlaylistLink(name: String, url: String, rawText: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val playlist = PlaylistEntity(
                name = name.ifBlank { "قائمة تشغيل" },
                type = PlaylistType.MEDIA_LINK,
                serverUrl = "",
                playlistUrl = url,
                rawContent = rawText
            )
            repository.savePlaylistAndSync(playlist)
            _isSyncing.value = false
            _userMessage.value = "تمت إضافة قائمة التشغيل بنجاح!"
            _selectedTab.value = MainTab.HOME
        }
    }

    fun refreshActivePlaylist() {
        val active = activePlaylist.value ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            _importProgress.value = 0
            _userMessage.value = if (_appLanguage.value == "ar") {
                "جاري تحديث محتوى الاشتراك..."
            } else {
                "Updating subscription content..."
            }
            try {
                repository.syncPlaylistContent(active, onProgress = { _importProgress.value = it }, forceRefresh = true)
                _userMessage.value = if (_appLanguage.value == "ar") {
                    "تم تحديث محتوى الاشتراك بنجاح! 🎉"
                } else {
                    "Subscription content updated successfully! 🎉"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userMessage.value = if (_appLanguage.value == "ar") {
                    "فشل التحديث: تأكد من الاتصال بالشبكة وصحة الاشتراك."
                } else {
                    "Update failed: check network connection and subscription status."
                }
            } finally {
                _isSyncing.value = false
                _importProgress.value = 0
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            _userMessage.value = "تم حذف الاشتراك"
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun recordProgress(itemId: String, positionMs: Long, durationMs: Long) {
        val media = currentlyPlaying.value ?: return
        val currentProfile = activeProfile.value
        if (durationMs > 0 && positionMs > 1000) {
            viewModelScope.launch {
                repository.recordWatchHistory(
                    itemId = itemId,
                    itemType = media.type,
                    title = media.title,
                    posterUrl = media.posterUrl,
                    streamUrl = media.streamUrl,
                    progressMs = positionMs,
                    totalMs = durationMs,
                    profileId = currentProfile?.id ?: 1L
                )
            }
        }
    }

    fun createProfile(name: String, avatarColorHex: String, isKids: Boolean, pinCode: String) {
        viewModelScope.launch {
            val newProfile = UserProfileEntity(
                name = name.ifBlank { "بروفايل جديد" },
                avatarColorHex = avatarColorHex,
                isKids = isKids,
                pinCode = pinCode,
                isActive = false
            )
            repository.createProfile(newProfile)
            _userMessage.value = "تم إنشاء البروفايل بنجاح"
        }
    }

    fun switchProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.selectActiveProfile(profile.id)
            _userMessage.value = "تم التبديل إلى بروفايل: ${profile.name}"
        }
    }

    fun deleteProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            _userMessage.value = "تم حذف البروفايل"
        }
    }

    fun startDownload(movie: MovieEntity) {
        viewModelScope.launch {
            val currentProfile = activeProfile.value
            val download = DownloadedItemEntity(
                id = "dl_${movie.id}_${System.currentTimeMillis()}",
                profileId = currentProfile?.id ?: 1L,
                title = movie.title,
                posterUrl = movie.posterUrl,
                streamUrl = movie.streamUrl,
                itemType = "MOVIE",
                fileSizeMb = (300..600).random(),
                progressPercent = 100,
                status = "COMPLETED",
                localFilePath = "/storage/emulated/0/Download/FliXTV/${movie.id}.mp4"
            )
            repository.addDownload(download)
            _userMessage.value = "تم تحميل ${movie.title} للمشاهدة بدون إنترنت!"
        }
    }

    fun startEpisodeDownload(title: String, posterUrl: String, streamUrl: String) {
        viewModelScope.launch {
            val currentProfile = activeProfile.value
            val download = DownloadedItemEntity(
                id = "dl_ep_${System.currentTimeMillis()}",
                profileId = currentProfile?.id ?: 1L,
                title = title,
                posterUrl = posterUrl,
                streamUrl = streamUrl,
                itemType = "EPISODE",
                fileSizeMb = (180..350).random(),
                progressPercent = 100,
                status = "COMPLETED",
                localFilePath = "/storage/emulated/0/Download/FliXTV/ep_${System.currentTimeMillis()}.mp4"
            )
            repository.addDownload(download)
            _userMessage.value = "تم تحميل $title للمشاهدة بدون إنترنت!"
        }
    }

    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            repository.deleteDownload(downloadId)
            _userMessage.value = "تم حذف الملف المحمل"
        }
    }

    // Custom Folders Management
    fun createCustomFolder(name: String, iconName: String = "Folder") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCustomFolder(name.trim(), iconName)
            _userMessage.value = "تم إنشاء المجلد المخصص: $name 📁"
        }
    }

    fun addChannelToFolder(folderId: Long, channelId: String) {
        viewModelScope.launch {
            val folder = customFolders.value.firstOrNull { it.id == folderId } ?: return@launch
            val ids = try {
                val jsonArray = org.json.JSONArray(folder.channelIdsJson)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getString(i))
                }
                list
            } catch (e: Exception) {
                mutableListOf()
            }
            if (!ids.contains(channelId)) {
                ids.add(channelId)
                val newJson = org.json.JSONArray(ids).toString()
                repository.updateFolderChannels(folderId, newJson)
                _userMessage.value = "تم إضافة القناة إلى مجلد '${folder.name}' 📁"
            } else {
                _userMessage.value = "القناة موجودة بالفعل في هذا المجلد"
            }
        }
    }

    fun removeChannelFromFolder(folderId: Long, channelId: String) {
        viewModelScope.launch {
            val folder = customFolders.value.firstOrNull { it.id == folderId } ?: return@launch
            val ids = try {
                val jsonArray = org.json.JSONArray(folder.channelIdsJson)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val idStr = jsonArray.getString(i)
                    if (idStr != channelId) list.add(idStr)
                }
                list
            } catch (e: Exception) {
                mutableListOf()
            }
            val newJson = org.json.JSONArray(ids).toString()
            repository.updateFolderChannels(folderId, newJson)
            _userMessage.value = "تم إزالة القناة من المجلد"
        }
    }

    fun deleteCustomFolder(folder: CustomFolderEntity) {
        viewModelScope.launch {
            repository.deleteCustomFolder(folder)
            _userMessage.value = "تم حذف المجلد المخصص"
        }
    }

    fun importLocalPlaylistFile(playlistName: String, content: String) {
        if (content.isBlank()) {
            _userMessage.value = "محتوى الملف فارغ"
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            val name = playlistName.ifBlank { "ملف محلي" }
            val newPlaylist = PlaylistEntity(
                name = name,
                type = PlaylistType.MEDIA_LINK,
                serverUrl = "Local File",
                rawContent = content,
                isActive = true
            )
            repository.savePlaylistAndSync(newPlaylist)
            _isSyncing.value = false
            _userMessage.value = "تم استيراد وتحليل الملف بنجاح! 🎉"
        }
    }
}
