package com.example.data

import com.example.model.*

object DemoDataSupplier {

    val DEMO_PLAYLIST = PlaylistEntity(
        id = 1L,
        name = "Premium Stream Server",
        type = PlaylistType.XTREAM,
        serverUrl = "http://line.dndnscloud.ru",
        username = "4357d392ea",
        password = com.example.util.SecurityUtils.encrypt("dd828ce13049"),
        isActive = true
    )

    val DEMO_CHANNELS = emptyList<ChannelEntity>()
    val DEMO_MOVIES = emptyList<MovieEntity>()
    val DEMO_SERIES = emptyList<SeriesEntity>()

    fun getDemoEpisodesForSeries(seriesId: String): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val sampleUrls = listOf(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
        )

        for (season in 1..3) {
            for (ep in 1..5) {
                val urlIndex = ((season - 1) * 5 + (ep - 1)) % sampleUrls.size
                episodes.add(
                    Episode(
                        id = "${seriesId}_s${season}_ep${ep}",
                        seriesId = seriesId,
                        seasonNum = season,
                        episodeNum = ep,
                        title = "الموسم $season - الحلقة $ep",
                        streamUrl = sampleUrls[urlIndex],
                        duration = "${(40..55).random()} دقيقة",
                        plot = "أحداث مثيرة ومشوقة في الحلقة $ep من الموسم $season."
                    )
                )
            }
        }
        return episodes
    }
}
