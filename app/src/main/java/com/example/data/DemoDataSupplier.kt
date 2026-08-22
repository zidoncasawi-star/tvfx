package com.example.data

import com.example.model.*

object DemoDataSupplier {

    // لا يوجد حساب Xtream افتراضي مكتوب في الكود — كل اشتراك يُجلب حصرياً من لوحة التحكم
    val DEMO_PLAYLIST = PlaylistEntity(
        id = 1L,
        name = "Waiting for a subscription from Admin",
        type = PlaylistType.XTREAM,
        serverUrl = "",
        username = "",
        password = "",
        isActive = false
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
                        title = "Season $season - Episode $ep",
                        streamUrl = sampleUrls[urlIndex],
                        duration = "${(40..55).random()} min",
                        plot = "Exciting and thrilling events in episode $ep of season $season."
                    )
                )
            }
        }
        return episodes
    }
}
