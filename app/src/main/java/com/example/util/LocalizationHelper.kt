package com.example.util

object LocalizationHelper {
    private val translations = mapOf(
        "home" to "Home",
        "live_tv" to "Live TV",
        "movies" to "Movies",
        "series" to "Series",
        "favorites" to "Favorites",
        "my_account" to "Account",
        "continue_watching" to "Continue Watching",
        "all" to "All",
        "search_hint" to "Search movies, series, channels...",
        "settings_title" to "App Settings",
        "app_language" to "App Language",
        "video_player" to "Video Player",
        "accent_color" to "Theme Accent Color",
        "stream_quality" to "Default Stream Quality",
        "clear_cache" to "Clear Cache",
        "clear_cache_desc" to "Helps speed up the app and solve stuttering issues",
        "cache_cleared" to "Cache cleared successfully! 🎉",
        "server_status" to "Server Status",
        "playlists" to "Active Subscriptions",
        "theme" to "App Theme",
        "vlc_desc" to "Requires external VLC Player app installed",
        "mx_desc" to "Requires external MX Player app installed",
        "system_desc" to "Play using default System Player",
        "exoplayer_desc" to "Fast & stable built-in player",
        "server" to "Active Server",
        "no_history" to "No watch history yet",
        "favorite_added" to "Added to Favorites",
        "favorite_removed" to "Removed from Favorites",
        "save" to "Save",
        "cancel" to "Cancel",
        "close" to "Close",
        "change_lang_alert" to "Changing language might require restarting the app for complete changes.",
        "auto" to "Automatic (Auto)",
        "fhd" to "High (FHD 1080p)",
        "hd" to "Medium (HD 720p)",
        "sd" to "Low (SD 480p)",
        "legal" to "Legal",
        "terms_conditions" to "Terms and Conditions",
        "privacy_policy" to "Privacy Policy"
    )

    // التطبيق يدعم الإنجليزية فقط الآن — يتجاهل معامل lang عمداً بدل حذفه، لتفادي تعديل كل
    // نقاط الاستدعاء التي ما زالت تمرّره
    fun translate(key: String, lang: String = "en"): String {
        return translations[key] ?: key
    }
}
