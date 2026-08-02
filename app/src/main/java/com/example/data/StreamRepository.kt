package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class StreamRepository(private val db: AppDatabase) {

    val playlists: Flow<List<PlaylistEntity>> = db.playlistDao().getAllPlaylists()
    val activePlaylist: Flow<PlaylistEntity?> = db.playlistDao().getActivePlaylist()
    val watchHistory: Flow<List<WatchHistoryEntity>> = db.watchHistoryDao().getWatchHistory()
    val loggedInAccount: Flow<UserAccountEntity?> = db.userAccountDao().getLoggedInAccount()

    suspend fun registerUserAccount(
        email: String,
        username: String,
        passwordRaw: String,
        phone: String,
        deviceId: String,
        serverActivationCode: String
    ): Result<UserAccountEntity> {
        val existing = db.userAccountDao().findAccountByEmailOrUsername(email, username)
        if (existing != null) {
            return Result.failure(Exception("البريد الإلكتروني أو اسم المستخدم مسجل بالفعل محلياً"))
        }
        val passHash = com.example.util.SecurityUtils.encrypt(passwordRaw)
        db.userAccountDao().logoutAllAccounts()
        
        val account = UserAccountEntity(
            fullName = username,
            email = email.trim(),
            username = username.trim(),
            passwordHash = passHash,
            phoneNumber = phone.trim(),
            deviceId = deviceId,
            isLoggedIn = true,
            isActivated = false,
            activationCode = serverActivationCode,
            adminServerUrl = "https://app.flixplayer.pro"
        )
        val id = db.userAccountDao().insertAccount(account)
        val inserted = account.copy(id = id)
        
        return Result.success(inserted)
    }

    suspend fun updateAccount(account: UserAccountEntity) {
        db.userAccountDao().updateAccount(account)
    }

    suspend fun insertAccount(account: UserAccountEntity) {
        db.userAccountDao().logoutAllAccounts()
        db.userAccountDao().insertAccount(account)
    }

    suspend fun loginUserAccount(identifier: String, passwordRaw: String): Result<UserAccountEntity> {
        val account = db.userAccountDao().findAccountByIdentifier(identifier.trim())
            ?: return Result.failure(Exception("اسم المستخدم أو البريد الإلكتروني غير صحيح"))

        val decPass = com.example.util.SecurityUtils.decrypt(account.passwordHash)
        if (decPass != passwordRaw) {
            return Result.failure(Exception("كلمة المرور غير صحيحة"))
        }

        db.userAccountDao().logoutAllAccounts()
        db.userAccountDao().setAccountLoggedIn(account.id)
        return Result.success(account.copy(isLoggedIn = true))
    }

    suspend fun logoutUserAccount() {
        db.userAccountDao().logoutAllAccounts()
    }

    fun getChannels(playlistId: Long): Flow<List<ChannelEntity>> {
        android.util.Log.d("SyncDebug", "Reading channels: playlistId=$playlistId")
        return db.channelDao().getChannelsByPlaylist(playlistId)
    }

    fun getFavoriteChannels(playlistId: Long): Flow<List<ChannelEntity>> =
        db.channelDao().getFavoriteChannels(playlistId)

    fun getMovies(playlistId: Long): Flow<List<MovieEntity>> {
        android.util.Log.d("SyncDebug", "Reading movies: playlistId=$playlistId")
        return db.movieDao().getMoviesByPlaylist(playlistId)
    }

    fun getFavoriteMovies(playlistId: Long): Flow<List<MovieEntity>> =
        db.movieDao().getFavoriteMovies(playlistId)

    fun getSeries(playlistId: Long): Flow<List<SeriesEntity>> {
        android.util.Log.d("SyncDebug", "Reading series: playlistId=$playlistId")
        return db.seriesDao().getSeriesByPlaylist(playlistId)
    }

    fun getFavoriteSeries(playlistId: Long): Flow<List<SeriesEntity>> =
        db.seriesDao().getFavoriteSeries(playlistId)

    fun getCategories(playlistId: Long, type: String): Flow<List<XtreamCategoryEntity>> =
        db.xtreamCategoryDao().getCategories(playlistId, type)

    // يحاول عدة تصنيفات بالترتيب حتى يجد أول تصنيف يحتوي محتوى فعلياً ويُحمِّله —
    // مهم جداً في الحسابات الضخمة (آلاف التصنيفات) حيث قد يكون أول تصنيف في القائمة فارغاً بالمصادفة،
    // فيبقى المستخدم أمام شاشة فارغة رغم نجاح الاستيراد تقنياً
    suspend fun fetchFirstNonEmptyCategory(
        playlistId: Long,
        categories: List<com.example.model.XtreamCategoryEntity>,
        type: String,
        forceRefresh: Boolean,
        maxAttempts: Int = 8,
        onItemCount: (type: String, count: Int) -> Unit = { _, _ -> }
    ) {
        val logUsername = db.userAccountDao().getLoggedInAccount().firstOrNull()?.username
        RemoteLogger.log(
            username = logUsername, level = "DEBUG", tag = "SyncDebug",
            message = "fetchFirstNonEmptyCategory START type=$type candidates=${categories.take(maxAttempts).map { it.id + ":" + it.name }}"
        )
        for (cat in categories.take(maxAttempts)) {
            val countBefore = when (type) {
                "live" -> db.channelDao().getChannelsByPlaylistAndCategorySync(playlistId, cat.id).size
                "vod" -> db.movieDao().getMoviesByPlaylistAndCategorySync(playlistId, cat.id).size
                "series" -> db.seriesDao().getSeriesByPlaylistAndCategorySync(playlistId, cat.id).size
                else -> 0
            }
            fetchAndStoreStreamsByCategory(playlistId, cat.id, type, forceRefresh, onItemCount)
            val countAfter = when (type) {
                "live" -> db.channelDao().getChannelsByPlaylistAndCategorySync(playlistId, cat.id).size
                "vod" -> db.movieDao().getMoviesByPlaylistAndCategorySync(playlistId, cat.id).size
                "series" -> db.seriesDao().getSeriesByPlaylistAndCategorySync(playlistId, cat.id).size
                else -> 0
            }
            RemoteLogger.log(
                username = logUsername, level = "DEBUG", tag = "SyncDebug",
                message = "fetchFirstNonEmptyCategory type=$type category=${cat.id}:${cat.name} before=$countBefore after=$countAfter"
            )
            if (countAfter > countBefore) return // وجدنا تصنيفاً بمحتوى حقيقي، توقّف هنا
        }
        RemoteLogger.log(
            username = logUsername, level = "ERROR", tag = "SyncDebug",
            message = "fetchFirstNonEmptyCategory FAILED — all $maxAttempts attempts for type=$type returned empty"
        )
    }

    suspend fun fetchAndStoreStreamsByCategory(
        playlistId: Long,
        categoryId: String,
        type: String,
        forceRefresh: Boolean = false,
        onItemCount: (type: String, count: Int) -> Unit = { _, _ -> }
    ) {
        android.util.Log.d("SyncDebug", "Saving: playlistId=$playlistId, categoryId=$categoryId, type=$type")
        val playlist = db.playlistDao().getAllPlaylists().firstOrNull()?.find { it.id == playlistId } ?: return
        if (playlist.type != PlaylistType.XTREAM) return

        // 1. Check if we already have cached streams for this category
        val hasCached = when (type) {
            "live" -> db.channelDao().getChannelsByPlaylistAndCategorySync(playlistId, categoryId).isNotEmpty()
            "vod" -> db.movieDao().getMoviesByPlaylistAndCategorySync(playlistId, categoryId).isNotEmpty()
            "series" -> db.seriesDao().getSeriesByPlaylistAndCategorySync(playlistId, categoryId).isNotEmpty()
            else -> false
        }

        // 2. If we have cache and we don't force refresh, and it's not stale (TTL of 6 hours)
        val isCacheStale = System.currentTimeMillis() - playlist.lastUpdated > 6 * 60 * 60 * 1000L
        if (hasCached && !forceRefresh && !isCacheStale) {
            return // Skip network request and display from cache
        }

        // 3. Otherwise, fetch from network and store
        // نُدرج النتائج على دفعات صغيرة (بدل إدراج كل التصنيف دفعة واحدة) حتى تتحدّث الواجهة تدريجياً
        // ولا تتجمّد الشاشة عند فتح تصنيف يحتوي آلاف العناصر، ولا تنطلق كل طلبات تحميل صور الملصقات دفعة واحدة
        try {
            when (type) {
                "live" -> {
                    val channels = XtreamCodesClient.fetchChannelsByCategory(playlist, categoryId)
                    var inserted = 0
                    channels.chunked(100).forEach { chunk ->
                        db.channelDao().insertChannels(chunk)
                        inserted += chunk.size
                        onItemCount("live", inserted)
                        kotlinx.coroutines.delay(15)
                    }
                }
                "vod" -> {
                    val movies = XtreamCodesClient.fetchMoviesByCategory(playlist, categoryId)
                    var inserted = 0
                    movies.chunked(100).forEach { chunk ->
                        db.movieDao().insertMovies(chunk)
                        inserted += chunk.size
                        onItemCount("vod", inserted)
                        kotlinx.coroutines.delay(15)
                    }
                }
                "series" -> {
                    val series = XtreamCodesClient.fetchSeriesByCategory(playlist, categoryId)
                    var inserted = 0
                    series.chunked(100).forEach { chunk ->
                        db.seriesDao().insertSeries(chunk)
                        inserted += chunk.size
                        onItemCount("series", inserted)
                        kotlinx.coroutines.delay(15)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val logUsername = db.userAccountDao().getLoggedInAccount().firstOrNull()?.username
            RemoteLogger.log(
                username = logUsername, level = "ERROR", tag = "SyncDebug",
                message = "fetchAndStoreStreamsByCategory FAILED type=$type categoryId=$categoryId host=${playlist.serverUrl} error=${e.javaClass.simpleName}: ${e.message}"
            )
            // If network fetch fails but we already have cached data, suppress error and keep using cache
            if (!hasCached) throw e
        }
    }

    suspend fun clearPlaylistCache(playlistId: Long) {
        db.xtreamCategoryDao().deleteByPlaylist(playlistId)
        db.channelDao().deleteByPlaylist(playlistId)
        db.movieDao().deleteByPlaylist(playlistId)
        db.seriesDao().deleteByPlaylist(playlistId)
        
        val playlistsList = db.playlistDao().getAllPlaylists().firstOrNull() ?: emptyList()
        val pl = playlistsList.find { it.id == playlistId }
        if (pl != null) {
            db.playlistDao().insertPlaylist(pl.copy(lastUpdated = 0))
        }
    }

    suspend fun savePlaylistAndSync(
        playlist: PlaylistEntity,
        onProgress: (Int) -> Unit = {},
        onStatusUpdate: (String) -> Unit = {}
    ): Long {
        db.playlistDao().deactivateAllPlaylists()
        val playlistId = db.playlistDao().insertPlaylist(playlist.copy(isActive = true))
        val active = playlist.copy(id = playlistId, isActive = true)

        syncPlaylistContent(active, onProgress, forceRefresh = true, onStatusUpdate = onStatusUpdate)
        return playlistId
    }

    suspend fun selectActivePlaylist(playlistId: Long, onProgress: (Int) -> Unit = {}) {
        db.playlistDao().deactivateAllPlaylists()
        db.playlistDao().activatePlaylist(playlistId)
        val active = db.playlistDao().getActivePlaylistSync()
        if (active != null) {
            syncPlaylistContent(active, onProgress, forceRefresh = false)
        }
    }

    // ترتيب الاستيراد: المسلسلات أولاً، ثم الأفلام، ثم القنوات المباشرة — مع تحديث نصي حي بعدد العناصر المستوردة فعلياً
    suspend fun syncPlaylistContent(
        playlist: PlaylistEntity,
        onProgress: (Int) -> Unit = {},
        forceRefresh: Boolean = false,
        onStatusUpdate: (String) -> Unit = {}
    ) {
        onProgress(10)

        if (playlist.type == PlaylistType.XTREAM) {
            val hasCategories = db.xtreamCategoryDao().getCategoriesSync(playlist.id).isNotEmpty()
            val isCacheStale = System.currentTimeMillis() - playlist.lastUpdated > 6 * 60 * 60 * 1000L

            if (hasCategories && !forceRefresh && !isCacheStale) {
                onProgress(100)
                return
            }

            // Fetch from network
            val logUsername = db.userAccountDao().getLoggedInAccount().firstOrNull()?.username
            RemoteLogger.log(
                username = logUsername, level = "DEBUG", tag = "SyncDebug",
                message = "syncPlaylistContent START host=${playlist.serverUrl} user=${playlist.username} playlistId=${playlist.id}"
            )
            try {
                onStatusUpdate("جاري تحميل تصنيفات المسلسلات...")
                val seriesCats = XtreamCodesClient.fetchCategories(playlist, "series")
                RemoteLogger.log(username = logUsername, level = "DEBUG", tag = "SyncDebug", message = "fetchCategories series -> count=${seriesCats.size}")
                onProgress(20)

                onStatusUpdate("جاري تحميل تصنيفات الأفلام...")
                val vodCats = XtreamCodesClient.fetchCategories(playlist, "vod")
                RemoteLogger.log(username = logUsername, level = "DEBUG", tag = "SyncDebug", message = "fetchCategories vod -> count=${vodCats.size}")
                onProgress(35)

                onStatusUpdate("جاري تحميل تصنيفات القنوات...")
                val liveCats = XtreamCodesClient.fetchCategories(playlist, "live")
                RemoteLogger.log(username = logUsername, level = "DEBUG", tag = "SyncDebug", message = "fetchCategories live -> count=${liveCats.size}")
                onProgress(50)

                if (seriesCats.isEmpty() && vodCats.isEmpty() && liveCats.isEmpty()) {
                    RemoteLogger.log(
                        username = logUsername, level = "ERROR", tag = "SyncDebug",
                        message = "ALL category lists came back EMPTY for host=${playlist.serverUrl} — likely a silently-swallowed network error in fetchCategories()"
                    )
                }

                db.xtreamCategoryDao().deleteByPlaylist(playlist.id)
                db.xtreamCategoryDao().insertCategories(seriesCats + vodCats + liveCats)

                // Save updated timestamp
                val updatedPlaylist = playlist.copy(lastUpdated = System.currentTimeMillis())
                db.playlistDao().insertPlaylist(updatedPlaylist)

                // استيراد تدريجي: مسلسلات أولاً، ثم أفلام، ثم قنوات — مع عرض عدد العناصر لحظياً.
                // نجرّب عدة تصنيفات (حتى 8) بدل الاكتفاء بأول واحد فقط، لأن أول تصنيف في القائمة
                // قد يكون فارغاً بالمصادفة على حسابات ضخمة تحتوي آلاف التصنيفات
                if (seriesCats.isNotEmpty()) {
                    fetchFirstNonEmptyCategory(playlist.id, seriesCats, "series", forceRefresh) { _, count ->
                        onStatusUpdate("جاري استيراد المسلسلات... ($count)")
                    }
                }
                onProgress(65)

                if (vodCats.isNotEmpty()) {
                    fetchFirstNonEmptyCategory(playlist.id, vodCats, "vod", forceRefresh) { _, count ->
                        onStatusUpdate("جاري استيراد الأفلام... ($count)")
                    }
                }
                onProgress(85)

                if (liveCats.isNotEmpty()) {
                    fetchFirstNonEmptyCategory(playlist.id, liveCats, "live", forceRefresh) { _, count ->
                        onStatusUpdate("جاري استيراد القنوات... ($count)")
                    }
                }
                onProgress(95)
                onStatusUpdate("اكتمل الاستيراد بنجاح 🎉")

            } catch (e: Exception) {
                e.printStackTrace()
                RemoteLogger.log(
                    username = logUsername, level = "ERROR", tag = "SyncDebug",
                    message = "syncPlaylistContent EXCEPTION host=${playlist.serverUrl} error=${e.javaClass.simpleName}: ${e.message}"
                )
                // Force throw to ensure the UI knows sync failed
                throw e
            }

            onProgress(100)
            return
        }

        val result = when (playlist.type) {
            PlaylistType.XTREAM -> {
                // This block is now handled above for lazy loading
                ParsedPlaylistResult(emptyList(), emptyList(), emptyList())
            }
            PlaylistType.MEDIA_LINK -> {
                if (playlist.rawContent.isNotBlank()) {
                    PlaylistParser.parse(playlist.rawContent, playlist.id)
                } else if (playlist.playlistUrl.isNotBlank()) {
                    // Try fetch playlist or use fallback
                    XtreamCodesClient.fetchPlaylistData(playlist)
                } else {
                    ParsedPlaylistResult(
                        channels = DemoDataSupplier.DEMO_CHANNELS,
                        movies = DemoDataSupplier.DEMO_MOVIES,
                        series = DemoDataSupplier.DEMO_SERIES
                    )
                }
            }
        }
        onProgress(40)

        db.channelDao().deleteByPlaylist(playlist.id)
        db.movieDao().deleteByPlaylist(playlist.id)
        db.seriesDao().deleteByPlaylist(playlist.id)
        onProgress(50)

        val totalItems = result.channels.size + result.movies.size + result.series.size
        var processed = 0

        result.channels.chunked(300).forEach { chunk ->
            db.channelDao().insertChannels(chunk)
            processed += chunk.size
            if (totalItems > 0) onProgress(50 + (processed * 45 / totalItems))
            kotlinx.coroutines.delay(10) // Small delay for visual progress
        }
        result.movies.chunked(300).forEach { chunk ->
            db.movieDao().insertMovies(chunk)
            processed += chunk.size
            if (totalItems > 0) onProgress(50 + (processed * 45 / totalItems))
            kotlinx.coroutines.delay(10)
        }
        result.series.chunked(300).forEach { chunk ->
            db.seriesDao().insertSeries(chunk)
            processed += chunk.size
            if (totalItems > 0) onProgress(50 + (processed * 45 / totalItems))
            kotlinx.coroutines.delay(10)
        }
        
        onProgress(100)
        kotlinx.coroutines.delay(500) // Brief pause at 100% for satisfaction
    }

    suspend fun toggleChannelFavorite(channelId: String, currentFav: Boolean) {
        db.channelDao().updateFavorite(channelId, !currentFav)
    }

    suspend fun toggleMovieFavorite(movieId: String, currentFav: Boolean) {
        db.movieDao().updateFavorite(movieId, !currentFav)
    }

    suspend fun toggleSeriesFavorite(seriesId: String, currentFav: Boolean) {
        db.seriesDao().updateFavorite(seriesId, !currentFav)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        db.channelDao().deleteByPlaylist(playlist.id)
        db.movieDao().deleteByPlaylist(playlist.id)
        db.seriesDao().deleteByPlaylist(playlist.id)
        db.playlistDao().deletePlaylist(playlist)
    }

    val profiles: Flow<List<UserProfileEntity>> = db.userProfileDao().getAllProfiles()
    val activeProfile: Flow<UserProfileEntity?> = db.userProfileDao().getActiveProfile()
    val downloads: Flow<List<DownloadedItemEntity>> = db.downloadedItemDao().getAllDownloads()
    val customFolders: Flow<List<CustomFolderEntity>> = db.customFolderDao().getAllFolders()

    suspend fun createCustomFolder(name: String, iconName: String = "Folder"): Long {
        return db.customFolderDao().insertFolder(
            CustomFolderEntity(name = name, iconName = iconName, channelIdsJson = "[]")
        )
    }

    suspend fun updateFolderChannels(folderId: Long, channelIdsJson: String) {
        db.customFolderDao().updateFolderChannels(folderId, channelIdsJson)
    }

    suspend fun deleteCustomFolder(folder: CustomFolderEntity) {
        db.customFolderDao().deleteFolder(folder)
    }

    suspend fun createProfile(profile: UserProfileEntity): Long {
        return db.userProfileDao().insertProfile(profile)
    }

    suspend fun selectActiveProfile(profileId: Long) {
        db.userProfileDao().deactivateAllProfiles()
        db.userProfileDao().activateProfile(profileId)
    }

    suspend fun deleteProfile(profile: UserProfileEntity) {
        db.userProfileDao().deleteProfile(profile)
    }

    suspend fun addDownload(download: DownloadedItemEntity) {
        db.downloadedItemDao().insertDownload(download)
    }

    suspend fun deleteDownload(id: String) {
        db.downloadedItemDao().deleteDownload(id)
    }

    suspend fun recordWatchHistory(
        itemId: String,
        itemType: String,
        title: String,
        posterUrl: String,
        streamUrl: String,
        progressMs: Long,
        totalMs: Long,
        profileId: Long = 1
    ) {
        db.watchHistoryDao().insertHistory(
            WatchHistoryEntity(
                itemId = itemId,
                profileId = profileId,
                itemType = itemType,
                title = title,
                posterUrl = posterUrl,
                streamUrl = streamUrl,
                progressMs = progressMs,
                totalMs = totalMs,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun ensureInitialDemoData() {
        // No more demo data by default as per request
    }
}
