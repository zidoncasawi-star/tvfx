package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlaylistType {
    XTREAM,
    MEDIA_LINK
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: PlaylistType,
    val serverUrl: String,
    val username: String = "",
    val password: String = "",
    val playlistUrl: String = "",
    val rawContent: String = "",
    val isActive: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val playlistId: Long,
    val name: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val category: String = "عام",
    val channelNum: Int = 0,
    val epgNow: String = "",
    val epgNext: String = "",
    val isFavorite: Boolean = false
)

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: String,
    val playlistId: Long,
    val title: String,
    val streamUrl: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val category: String = "أفلام",
    val rating: String = "8.5",
    val releaseYear: String = "2024",
    val duration: String = "120 دقيقة",
    val plot: String = "",
    val isFavorite: Boolean = false
)

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val playlistId: Long,
    val title: String,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val category: String = "مسلسلات",
    val rating: String = "8.8",
    val releaseYear: String = "2024",
    val plot: String = "",
    val isFavorite: Boolean = false,
    val streamUrl: String = ""
)

data class Episode(
    val id: String,
    val seriesId: String,
    val seasonNum: Int,
    val episodeNum: Int,
    val title: String,
    val streamUrl: String,
    val duration: String = "45 دقيقة",
    val plot: String = ""
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val itemId: String,
    val profileId: Long = 1,
    val itemType: String, // "LIVE", "MOVIE", "EPISODE"
    val title: String,
    val posterUrl: String,
    val streamUrl: String,
    val progressMs: Long,
    val totalMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarColorHex: String = "#E50914", // Hex string or avatar style
    val isKids: Boolean = false,
    val pinCode: String = "",
    val isActive: Boolean = false
)

@Entity(tableName = "downloaded_items")
data class DownloadedItemEntity(
    @PrimaryKey val id: String,
    val profileId: Long = 1,
    val title: String,
    val posterUrl: String = "",
    val streamUrl: String = "",
    val itemType: String = "MOVIE", // "MOVIE", "EPISODE"
    val fileSizeMb: Int = 320,
    val progressPercent: Int = 100, // 0 to 100
    val status: String = "COMPLETED", // "DOWNLOADING", "PAUSED", "COMPLETED"
    val localFilePath: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_folders")
data class CustomFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "Sports", "Movies", "News", "قنوات رياضية"
    val iconName: String = "Folder",
    val channelIdsJson: String = "[]" // JSON string or comma separated IDs
)

@Entity(tableName = "xtream_categories")
data class XtreamCategoryEntity(
    @PrimaryKey val id: String, // String because it could be anything from Xtream
    val playlistId: Long,
    val name: String,
    val type: String // "live", "vod", "series"
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val email: String,
    val username: String,
    val passwordHash: String,
    val phoneNumber: String = "",
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isActivated: Boolean = true,
    val activationCode: String = "",
    val adminServerUrl: String = "",
    val deviceId: String = "",
    val xtreamHost: String = "",
    val xtreamUsername: String = "",
    val xtreamPassword: String = ""
)

enum class MainTab(val titleAr: String) {
    HOME("الرئيسية"),
    LIVE_TV("القنوات المباشرة"),
    MOVIES("الأفلام"),
    SERIES("المسلسلات"),
    FAVORITES("المفضلة"),
    USER_ACCOUNT("حسابي")
}
