package com.example.data

import com.example.model.ChannelEntity
import com.example.model.MovieEntity
import com.example.model.PlaylistEntity
import com.example.model.SeriesEntity
import com.example.model.Episode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object XtreamCodesClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS) // بعض التصنيفات على حسابات ضخمة (عشرات آلاف العناصر) تحتاج وقتاً أطول لتحميل استجابة JSON الكبيرة
        .writeTimeout(30, TimeUnit.SECONDS)
        .dns(DohDns) // يتجاوز حظر/تسميم DNS من الراوتر أو مزوّد الإنترنت عبر Cloudflare DoH كخطة بديلة
        .build()

    suspend fun fetchPlaylistData(playlist: PlaylistEntity): ParsedPlaylistResult = withContext(Dispatchers.IO) {
        val serverUrl = playlist.serverUrl.trimEnd('/')
        val username = playlist.username
        val password = com.example.util.SecurityUtils.decrypt(playlist.password)

        val channels = mutableListOf<ChannelEntity>()
        val movies = mutableListOf<MovieEntity>()
        val series = mutableListOf<SeriesEntity>()

        val baseUrl = "$serverUrl/player_api.php?username=$username&password=$password"

        // 1. LiveStreams
        try {
            val liveUrl = "$baseUrl&action=get_live_streams"
            val liveJson = executeGet(liveUrl)
            if (liveJson != null && liveJson.trim().startsWith("[")) {
                val array = JSONArray(liveJson)
                val limit = minOf(array.length(), 5000)
                for (i in 0 until limit) {
                    try {
                        val obj = array.getJSONObject(i)
                        val streamId = obj.optString("stream_id")
                        if (streamId.isNullOrBlank()) continue
                        val name = obj.optString("name", "Channel ${i + 1}")
                        val logo = obj.optString("stream_icon", "")
                        val categoryName = obj.optString("category_name", "General")
                        val num = obj.optInt("num", i + 1)

                        val streamUrl = "$serverUrl/live/$username/$password/$streamId.m3u8"
                        channels.add(
                            ChannelEntity(
                                id = "xt_ch_${playlist.id}_$streamId",
                                playlistId = playlist.id,
                                name = name,
                                streamUrl = streamUrl,
                                logoUrl = logo,
                                category = categoryName,
                                channelNum = num
                            )
                        )
                    } catch (inner: Throwable) {
                        inner.printStackTrace()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // 2. VOD (Movies)
        try {
            val vodUrl = "$baseUrl&action=get_vod_streams"
            val vodJson = executeGet(vodUrl)
            if (vodJson != null && vodJson.trim().startsWith("[")) {
                val array = JSONArray(vodJson)
                val limit = minOf(array.length(), 5000)
                for (i in 0 until limit) {
                    try {
                        val obj = array.getJSONObject(i)
                        val streamId = obj.optString("stream_id")
                        if (streamId.isNullOrBlank()) continue
                        val name = obj.optString("name", "Movie ${i + 1}")
                        val logo = obj.optString("stream_icon", "")
                        val ext = obj.optString("container_extension", "mp4").ifBlank { "mp4" }
                        val rating = obj.optString("rating", "8.0")
                        val categoryName = obj.optString("category_name", "Movies")

                        val streamUrl = "$serverUrl/movie/$username/$password/$streamId.$ext"
                        movies.add(
                            MovieEntity(
                                id = "xt_mov_${playlist.id}_$streamId",
                                playlistId = playlist.id,
                                title = name,
                                streamUrl = streamUrl,
                                posterUrl = logo,
                                backdropUrl = logo,
                                category = categoryName,
                                rating = rating,
                                releaseYear = "2024"
                            )
                        )
                    } catch (inner: Throwable) {
                        inner.printStackTrace()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // 3. Series
        try {
            val seriesUrl = "$baseUrl&action=get_series"
            val seriesJson = executeGet(seriesUrl)
            if (seriesJson != null && seriesJson.trim().startsWith("[")) {
                val array = JSONArray(seriesJson)
                val limit = minOf(array.length(), 5000)
                for (i in 0 until limit) {
                    try {
                        val obj = array.getJSONObject(i)
                        val seriesId = obj.optString("series_id")
                        if (seriesId.isNullOrBlank()) continue
                        val name = obj.optString("name", "Series ${i + 1}")
                        val logo = obj.optString("cover", "")
                        val rating = obj.optString("rating", "8.5")
                        val categoryName = obj.optString("category_name", "Series")

                        series.add(
                            SeriesEntity(
                                id = "xt_ser_${playlist.id}_$seriesId",
                                playlistId = playlist.id,
                                title = name,
                                posterUrl = logo,
                                backdropUrl = logo,
                                category = categoryName,
                                rating = rating,
                                releaseYear = "2024"
                            )
                        )
                    } catch (inner: Throwable) {
                        inner.printStackTrace()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Fallback if empty or unreachable
        if (channels.isEmpty() && movies.isEmpty() && series.isEmpty()) {
            return@withContext ParsedPlaylistResult(
                channels = DemoDataSupplier.DEMO_CHANNELS,
                movies = DemoDataSupplier.DEMO_MOVIES,
                series = DemoDataSupplier.DEMO_SERIES
            )
        }

        ParsedPlaylistResult(channels, movies, series)
    }

    suspend fun fetchCategories(playlist: PlaylistEntity, type: String): List<com.example.model.XtreamCategoryEntity> = withContext(Dispatchers.IO) {
        val serverUrl = playlist.serverUrl.trimEnd('/')
        val username = playlist.username
        val password = com.example.util.SecurityUtils.decrypt(playlist.password)
        
        val action = when(type) {
            "live" -> "get_live_categories"
            "vod" -> "get_vod_categories"
            "series" -> "get_series_categories"
            else -> "get_live_categories"
        }
        
        val url = "$serverUrl/player_api.php?username=$username&password=$password&action=$action"
        val categories = mutableListOf<com.example.model.XtreamCategoryEntity>()
        
        try {
            val jsonStr = executeGet(url)
            if (jsonStr != null && jsonStr.trim().startsWith("[")) {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("category_id")
                    val name = obj.optString("category_name", "Unknown")
                    if (id.isNotBlank()) {
                        categories.add(
                            com.example.model.XtreamCategoryEntity(
                                id = id,
                                playlistId = playlist.id,
                                name = name,
                                type = type
                            )
                        )
                    }
                }
            } else {
                RemoteLogger.log(
                    level = "ERROR", tag = "SyncDebug",
                    message = "fetchCategories($type) got non-array response, len=${jsonStr?.length ?: -1}, prefix=${jsonStr?.take(120)}"
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            RemoteLogger.log(
                level = "ERROR", tag = "SyncDebug",
                message = "fetchCategories($type) EXCEPTION host=$serverUrl error=${e.javaClass.simpleName}: ${e.message}"
            )
        }
        categories
    }

    suspend fun fetchChannelsByCategory(playlist: PlaylistEntity, categoryId: String): List<ChannelEntity> = withContext(Dispatchers.IO) {
        val serverUrl = playlist.serverUrl.trimEnd('/')
        val username = playlist.username
        val password = com.example.util.SecurityUtils.decrypt(playlist.password)
        val url = "$serverUrl/player_api.php?username=$username&password=$password&action=get_live_streams&category_id=$categoryId"
        
        val channels = mutableListOf<ChannelEntity>()
        try {
            val jsonStr = executeGet(url)
            if (jsonStr != null && jsonStr.trim().startsWith("[")) {
                val array = JSONArray(jsonStr)
                // حماية من تصنيف واحد ضخم بشكل غير طبيعي (آلاف العناصر) يستهلك الذاكرة/الشبكة دفعة واحدة
                val limit = minOf(array.length(), 3000)
                for (i in 0 until limit) {
                    val obj = array.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    if (streamId.isNotBlank()) {
                        val name = obj.optString("name")
                        val logo = obj.optString("stream_icon")
                        val streamUrl = "$serverUrl/live/$username/$password/$streamId.m3u8"
                        var epg = obj.optString("epg_title", "")
                        if (epg.isEmpty()) epg = obj.optString("title", "")
                        channels.add(
                            ChannelEntity(
                                id = "xt_ch_${playlist.id}_$streamId",
                                playlistId = playlist.id,
                                name = name,
                                streamUrl = streamUrl,
                                logoUrl = logo,
                                category = categoryId, // Use ID for internal matching
                                epgNow = epg
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        channels
    }

    suspend fun fetchMoviesByCategory(playlist: PlaylistEntity, categoryId: String): List<MovieEntity> = withContext(Dispatchers.IO) {
        val serverUrl = playlist.serverUrl.trimEnd('/')
        val username = playlist.username
        val password = com.example.util.SecurityUtils.decrypt(playlist.password)
        val url = "$serverUrl/player_api.php?username=$username&password=$password&action=get_vod_streams&category_id=$categoryId"
        
        val movies = mutableListOf<MovieEntity>()
        try {
            val jsonStr = executeGet(url)
            if (jsonStr != null && jsonStr.trim().startsWith("[")) {
                val array = JSONArray(jsonStr)
                val limit = minOf(array.length(), 3000)
                for (i in 0 until limit) {
                    val obj = array.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    if (streamId.isNotBlank()) {
                        val name = obj.optString("name")
                        val logo = obj.optString("stream_icon")
                        val ext = obj.optString("container_extension", "mp4").ifBlank { "mp4" }
                        val rating = obj.optString("rating", "8.0")
                        val streamUrl = "$serverUrl/movie/$username/$password/$streamId.$ext"
                        movies.add(
                            MovieEntity(
                                id = "xt_mov_${playlist.id}_$streamId",
                                playlistId = playlist.id,
                                title = name,
                                streamUrl = streamUrl,
                                posterUrl = logo,
                                backdropUrl = logo,
                                category = categoryId,
                                rating = rating
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        movies
    }

    suspend fun fetchSeriesByCategory(playlist: PlaylistEntity, categoryId: String): List<SeriesEntity> = withContext(Dispatchers.IO) {
        val serverUrl = playlist.serverUrl.trimEnd('/')
        val username = playlist.username
        val password = com.example.util.SecurityUtils.decrypt(playlist.password)
        val url = "$serverUrl/player_api.php?username=$username&password=$password&action=get_series&category_id=$categoryId"
        
        val series = mutableListOf<SeriesEntity>()
        try {
            val jsonStr = executeGet(url)
            if (jsonStr != null && jsonStr.trim().startsWith("[")) {
                val array = JSONArray(jsonStr)
                val limit = minOf(array.length(), 3000)
                for (i in 0 until limit) {
                    val obj = array.getJSONObject(i)
                    val seriesId = obj.optString("series_id")
                    if (seriesId.isNotBlank()) {
                        val name = obj.optString("name")
                        val logo = obj.optString("cover")
                        val rating = obj.optString("rating", "8.5")
                        series.add(
                            SeriesEntity(
                                id = "xt_ser_${playlist.id}_$seriesId",
                                playlistId = playlist.id,
                                title = name,
                                posterUrl = logo,
                                backdropUrl = logo,
                                category = categoryId,
                                rating = rating
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        series
    }

    private fun executeGet(url: String): String? {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return response.body?.string()
            }
        }
        return null
    }

    suspend fun fetchSeriesEpisodes(playlist: PlaylistEntity, seriesId: String): SeriesInfoResult = withContext(Dispatchers.IO) {
        val serverUrl = playlist.serverUrl.trimEnd('/')
        val username = playlist.username
        val password = com.example.util.SecurityUtils.decrypt(playlist.password)
        val baseUrl = "$serverUrl/player_api.php?username=$username&password=$password"
        val url = "$baseUrl&action=get_series_info&series_id=$seriesId"

        val episodes = mutableListOf<Episode>()
        var genre = ""
        var cast = ""
        var infoPlot = ""
        try {
            val jsonStr = executeGet(url)
            if (jsonStr != null && jsonStr.trim().startsWith("{")) {
                val jsonObj = JSONObject(jsonStr)
                val infoObj = jsonObj.optJSONObject("info")
                if (infoObj != null) {
                    genre = infoObj.optString("genre", "")
                    cast = infoObj.optString("cast", "")
                    infoPlot = infoObj.optString("plot", "").ifBlank { infoObj.optString("description", "") }
                }
                val episodesObj = jsonObj.optJSONObject("episodes")
                if (episodesObj == null) {
                    RemoteLogger.log(
                        level = "ERROR", tag = "SyncDebug",
                        message = "fetchSeriesEpisodes seriesId=$seriesId: 'episodes' key missing/not-an-object. hasEpisodesKey=${jsonObj.has("episodes")} episodesType=${jsonObj.opt("episodes")?.javaClass?.simpleName} responsePrefix=${jsonStr.take(300)}"
                    )
                }
                if (episodesObj != null) {
                    val keys = episodesObj.keys()
                    while (keys.hasNext()) {
                        val seasonNumStr = keys.next()
                        val seasonNum = seasonNumStr.toIntOrNull() ?: 1
                        val epArray = episodesObj.optJSONArray(seasonNumStr)
                        if (epArray != null) {
                            for (i in 0 until epArray.length()) {
                                try {
                                    val epObj = epArray.getJSONObject(i)
                                    val id = epObj.optString("id")
                                    if (id.isNullOrBlank()) continue
                                    val title = epObj.optString("title", "Episode ${i + 1}")
                                    val epNum = epObj.optInt("episode_num", i + 1)
                                    val ext = epObj.optString("container_extension", "mp4").ifBlank { "mp4" }
                                    
                                    val streamUrl = "$serverUrl/series/$username/$password/$id.$ext"
                                    episodes.add(
                                        Episode(
                                            id = "xt_ep_${playlist.id}_${id}",
                                            seriesId = "xt_ser_${playlist.id}_$seriesId",
                                            seasonNum = seasonNum,
                                            episodeNum = epNum,
                                            title = title,
                                            streamUrl = streamUrl,
                                            duration = "45 min",
                                            plot = ""
                                        )
                                    )
                                } catch (inner: Throwable) {
                                    inner.printStackTrace()
                                }
                            }
                        }
                    }
                }
            } else {
                RemoteLogger.log(
                    level = "ERROR", tag = "SyncDebug",
                    message = "fetchSeriesEpisodes seriesId=$seriesId: non-object response, len=${jsonStr?.length ?: -1} prefix=${jsonStr?.take(200)}"
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            RemoteLogger.log(
                level = "ERROR", tag = "SyncDebug",
                message = "fetchSeriesEpisodes seriesId=$seriesId EXCEPTION host=$serverUrl error=${e.javaClass.simpleName}: ${e.message}"
            )
        }
        SeriesInfoResult(episodes = episodes, genre = genre, cast = cast, plot = infoPlot)
    }

    /** يجلب genre/cast/plot لفيلم واحد — نفس get_vod_info المستخدم في سطح المكتب لعرضها في شاشة التفاصيل */
    suspend fun fetchVodInfo(playlist: PlaylistEntity, vodId: String): VodInfoResult = withContext(Dispatchers.IO) {
        val serverUrl = playlist.serverUrl.trimEnd('/')
        val username = playlist.username
        val password = com.example.util.SecurityUtils.decrypt(playlist.password)
        val url = "$serverUrl/player_api.php?username=$username&password=$password&action=get_vod_info&vod_id=$vodId"
        try {
            val jsonStr = executeGet(url)
            if (jsonStr != null && jsonStr.trim().startsWith("{")) {
                val jsonObj = JSONObject(jsonStr)
                val infoObj = jsonObj.optJSONObject("info")
                if (infoObj != null) {
                    return@withContext VodInfoResult(
                        genre = infoObj.optString("genre", ""),
                        cast = infoObj.optString("cast", ""),
                        plot = infoObj.optString("plot", "").ifBlank { infoObj.optString("description", "") }
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        VodInfoResult()
    }

    // دليل البرامج الحقيقي (EPG) للقناة — يجلب البرنامج الحالي والقادم فعلياً من سيرفر Xtream
    // بدل الاعتماد على أي بيانات وهمية أو نص القناة نفسه
    suspend fun fetchShortEpg(playlist: PlaylistEntity, streamId: String, limit: Int = 4): List<ShortEpgEntry> = withContext(Dispatchers.IO) {
        val serverUrl = playlist.serverUrl.trimEnd('/')
        val username = playlist.username
        val password = com.example.util.SecurityUtils.decrypt(playlist.password)
        val url = "$serverUrl/player_api.php?username=$username&password=$password&action=get_short_epg&stream_id=$streamId&limit=$limit"

        val result = mutableListOf<ShortEpgEntry>()
        try {
            val jsonStr = executeGet(url)
            if (jsonStr != null && jsonStr.trim().startsWith("{")) {
                val jsonObj = JSONObject(jsonStr)
                val listings = jsonObj.optJSONArray("epg_listings")
                if (listings != null) {
                    for (i in 0 until listings.length()) {
                        val item = listings.getJSONObject(i)
                        val title = decodeEpgText(item.optString("title", ""))
                        val description = decodeEpgText(item.optString("description", ""))
                        val start = formatEpgTime(item.optString("start", ""))
                        val end = formatEpgTime(item.optString("end", ""))
                        val nowPlaying = item.optInt("now_playing", 0) == 1
                        if (title.isNotBlank()) {
                            result.add(ShortEpgEntry(title, description, start, end, nowPlaying))
                        }
                    }
                } else {
                    RemoteLogger.log(
                        level = "DEBUG", tag = "SyncDebug",
                        message = "fetchShortEpg streamId=$streamId: no epg_listings in response, prefix=${jsonStr.take(150)}"
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            RemoteLogger.log(
                level = "ERROR", tag = "SyncDebug",
                message = "fetchShortEpg streamId=$streamId EXCEPTION error=${e.javaClass.simpleName}: ${e.message}"
            )
        }
        result
    }

    private fun decodeEpgText(base64Text: String): String {
        if (base64Text.isBlank()) return ""
        return try {
            String(android.util.Base64.decode(base64Text, android.util.Base64.DEFAULT), Charsets.UTF_8).trim()
        } catch (e: Exception) {
            base64Text
        }
    }

    private fun formatEpgTime(raw: String): String {
        // الصيغة القادمة من Xtream عادة "yyyy-MM-dd HH:mm:ss" — نستخرج الساعة والدقيقة فقط
        return try {
            val timePart = raw.split(" ").getOrNull(1) ?: return raw
            timePart.take(5)
        } catch (e: Exception) {
            raw
        }
    }
}

data class SeriesInfoResult(
    val episodes: List<Episode>,
    val genre: String = "",
    val cast: String = "",
    val plot: String = ""
)

data class VodInfoResult(
    val genre: String = "",
    val cast: String = "",
    val plot: String = ""
)

data class ShortEpgEntry(
    val title: String,
    val description: String,
    val start: String,
    val end: String,
    val isNowPlaying: Boolean
)
