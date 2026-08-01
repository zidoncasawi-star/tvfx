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

    suspend fun fetchAndStoreStreamsByCategory(playlistId: Long, categoryId: String, type: String, forceRefresh: Boolean = false) {
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
        try {
            when (type) {
                "live" -> {
                    val channels = XtreamCodesClient.fetchChannelsByCategory(playlist, categoryId)
                    if (channels.isNotEmpty()) {
                        db.channelDao().insertChannels(channels)
                    }
                }
                "vod" -> {
                    val movies = XtreamCodesClient.fetchMoviesByCategory(playlist, categoryId)
                    if (movies.isNotEmpty()) {
                        db.movieDao().insertMovies(movies)
                    }
                }
                "series" -> {
                    val series = XtreamCodesClient.fetchSeriesByCategory(playlist, categoryId)
                    if (series.isNotEmpty()) {
                        db.seriesDao().insertSeries(series)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    suspend fun savePlaylistAndSync(playlist: PlaylistEntity, onProgress: (Int) -> Unit = {}): Long {
        db.playlistDao().deactivateAllPlaylists()
        val playlistId = db.playlistDao().insertPlaylist(playlist.copy(isActive = true))
        val active = playlist.copy(id = playlistId, isActive = true)

        syncPlaylistContent(active, onProgress, forceRefresh = true)
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

    suspend fun syncPlaylistContent(playlist: PlaylistEntity, onProgress: (Int) -> Unit = {}, forceRefresh: Boolean = false) {
        onProgress(10)
        
        if (playlist.type == PlaylistType.XTREAM) {
            val hasCategories = db.xtreamCategoryDao().getCategoriesSync(playlist.id).isNotEmpty()
            val isCacheStale = System.currentTimeMillis() - playlist.lastUpdated > 6 * 60 * 60 * 1000L

            if (hasCategories && !forceRefresh && !isCacheStale) {
                onProgress(100)
                return
            }

            // Fetch from network
            try {
                val liveCats = XtreamCodesClient.fetchCategories(playlist, "live")
                onProgress(30)
                val vodCats = XtreamCodesClient.fetchCategories(playlist, "vod")
                onProgress(60)
                val seriesCats = XtreamCodesClient.fetchCategories(playlist, "series")
                onProgress(90)
                
                db.xtreamCategoryDao().deleteByPlaylist(playlist.id)
                db.xtreamCategoryDao().insertCategories(liveCats + vodCats + seriesCats)
                
                // Save updated timestamp
                val updatedPlaylist = playlist.copy(lastUpdated = System.currentTimeMillis())
                db.playlistDao().insertPlaylist(updatedPlaylist)
                
                // Pre-fetch the first category of each type so the Home screen has some content initially
                if (liveCats.isNotEmpty()) fetchAndStoreStreamsByCategory(playlist.id, liveCats.first().id, "live", forceRefresh)
                if (vodCats.isNotEmpty()) fetchAndStoreStreamsByCategory(playlist.id, vodCats.first().id, "vod", forceRefresh)
                if (seriesCats.isNotEmpty()) fetchAndStoreStreamsByCategory(playlist.id, seriesCats.first().id, "series", forceRefresh)
                
            } catch (e: Exception) {
                e.printStackTrace()
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
