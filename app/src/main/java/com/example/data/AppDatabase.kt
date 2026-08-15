package com.example.data

import android.content.Context
import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY id DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    fun getActivePlaylist(): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE isActive = 1 LIMIT 1")
    suspend fun getActivePlaylistSync(): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET isActive = 0")
    suspend fun deactivateAllPlaylists()

    @Query("UPDATE playlists SET isActive = 1 WHERE id = :id")
    suspend fun activatePlaylist(id: Long)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId")
    fun getChannelsByPlaylist(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isFavorite = 1")
    fun getFavoriteChannels(playlistId: Long): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND category = :categoryId")
    suspend fun getChannelsByPlaylistAndCategorySync(playlistId: Long, categoryId: String): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE channels SET isFavorite = :isFav WHERE id = :channelId")
    suspend fun updateFavorite(channelId: String, isFav: Boolean)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies WHERE playlistId = :playlistId")
    fun getMoviesByPlaylist(playlistId: Long): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE playlistId = :playlistId AND isFavorite = 1")
    fun getFavoriteMovies(playlistId: Long): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE playlistId = :playlistId AND category = :categoryId")
    suspend fun getMoviesByPlaylistAndCategorySync(playlistId: Long, categoryId: String): List<MovieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("UPDATE movies SET isFavorite = :isFav WHERE id = :movieId")
    suspend fun updateFavorite(movieId: String, isFav: Boolean)

    @Query("DELETE FROM movies WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series WHERE playlistId = :playlistId")
    fun getSeriesByPlaylist(playlistId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE playlistId = :playlistId AND isFavorite = 1")
    fun getFavoriteSeries(playlistId: Long): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE playlistId = :playlistId AND category = :categoryId")
    suspend fun getSeriesByPlaylistAndCategorySync(playlistId: Long, categoryId: String): List<SeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeries(series: List<SeriesEntity>)

    @Query("UPDATE series SET isFavorite = :isFav WHERE id = :seriesId")
    suspend fun updateFavorite(seriesId: String, isFav: Boolean)

    @Query("DELETE FROM series WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface XtreamCategoryDao {
    @Query("SELECT * FROM xtream_categories WHERE playlistId = :playlistId AND type = :type")
    fun getCategories(playlistId: Long, type: String): Flow<List<XtreamCategoryEntity>>

    @Query("SELECT * FROM xtream_categories WHERE playlistId = :playlistId")
    suspend fun getCategoriesSync(playlistId: Long): List<XtreamCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<XtreamCategoryEntity>)

    @Query("DELETE FROM xtream_categories WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY timestamp DESC")
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistoryEntity)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInAccount(): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_accounts WHERE email = :email OR username = :username LIMIT 1")
    suspend fun findAccountByEmailOrUsername(email: String, username: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE email = :identifier OR username = :identifier LIMIT 1")
    suspend fun findAccountByIdentifier(identifier: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccountEntity): Long

    @Update
    suspend fun updateAccount(account: UserAccountEntity)

    @Query("UPDATE user_accounts SET isLoggedIn = 0")
    suspend fun logoutAllAccounts()

    @Query("UPDATE user_accounts SET isLoggedIn = 1 WHERE id = :id")
    suspend fun setAccountLoggedIn(id: Long)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity): Long

    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE user_profiles SET isActive = 1 WHERE id = :id")
    suspend fun activateProfile(id: Long)

    @Delete
    suspend fun deleteProfile(profile: UserProfileEntity)
}

@Dao
interface DownloadedItemDao {
    @Query("SELECT * FROM downloaded_items")
    fun getAllDownloads(): Flow<List<DownloadedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadedItemEntity)

    @Query("DELETE FROM downloaded_items WHERE id = :id")
    suspend fun deleteDownload(id: String)
}

@Dao
interface CustomFolderDao {
    @Query("SELECT * FROM custom_folders")
    fun getAllFolders(): Flow<List<CustomFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: CustomFolderEntity): Long

    @Query("UPDATE custom_folders SET channelIdsJson = :channelIds WHERE id = :id")
    suspend fun updateFolderChannels(id: Long, channelIds: String)

    @Delete
    suspend fun deleteFolder(folder: CustomFolderEntity)
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY startedAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE status = 'RECORDING'")
    suspend fun getActiveRecordingsSync(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE status = 'SCHEDULED'")
    suspend fun getScheduledRecordingsSync(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)
}

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        XtreamCategoryEntity::class,
        UserAccountEntity::class,
        UserProfileEntity::class,
        DownloadedItemEntity::class,
        CustomFolderEntity::class,
        WatchHistoryEntity::class,
        RecordingEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun xtreamCategoryDao(): XtreamCategoryDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun downloadedItemDao(): DownloadedItemDao
    abstract fun customFolderDao(): CustomFolderDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
