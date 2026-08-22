package com.example.data

import com.example.model.ChannelEntity
import com.example.model.MovieEntity
import com.example.model.SeriesEntity
import java.io.BufferedReader
import java.io.StringReader

data class ParsedPlaylistResult(
    val channels: List<ChannelEntity>,
    val movies: List<MovieEntity>,
    val series: List<SeriesEntity>
)

object PlaylistParser {

    fun parse(content: String, playlistId: Long): ParsedPlaylistResult {
        val channels = mutableListOf<ChannelEntity>()
        val movies = mutableListOf<MovieEntity>()
        val series = mutableListOf<SeriesEntity>()

        val reader = BufferedReader(StringReader(content))
        var line: String?
        var currentExtInf: String? = null
        var itemNum = 1

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: continue
            if (trimmed.startsWith("#EXTINF:")) {
                currentExtInf = trimmed
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && currentExtInf != null) {
                val url = trimmed
                val parsed = parseItem(currentExtInf, url, playlistId, itemNum++)
                when (parsed) {
                    is ParsedItem.Channel -> channels.add(parsed.channel)
                    is ParsedItem.Movie -> movies.add(parsed.movie)
                    is ParsedItem.Series -> series.add(parsed.series)
                }
                currentExtInf = null
            }
        }

        return ParsedPlaylistResult(channels, movies, series)
    }

    private sealed class ParsedItem {
        data class Channel(val channel: ChannelEntity) : ParsedItem()
        data class Movie(val movie: MovieEntity) : ParsedItem()
        data class Series(val series: SeriesEntity) : ParsedItem()
    }

    private fun parseItem(
        extInf: String,
        url: String,
        playlistId: Long,
        num: Int
    ): ParsedItem {
        // Extract attributes
        val logo = extractAttribute(extInf, "tvg-logo")
        var group = extractAttribute(extInf, "group-title").ifEmpty { "General" }
        var name = extInf.substringAfterLast(",").trim()
        if (name.isEmpty()) name = "Channel $num"

        val lowerUrl = url.lowercase()
        val lowerGroup = group.lowercase()

        val isMovie = lowerGroup.contains("movie") || lowerGroup.contains("movies") ||
                lowerGroup.contains("أفلام") || lowerGroup.contains("افلام") ||
                lowerGroup.contains("فيلم") || lowerGroup.contains("فلتر") ||
                lowerGroup.contains("cinema") || lowerGroup.contains("vod") ||
                lowerUrl.contains("/movie/") || lowerUrl.contains("/movies/") ||
                lowerUrl.contains(".mp4") || lowerUrl.contains(".mkv") || lowerUrl.contains(".avi")

        val isSeries = lowerGroup.contains("series") || lowerGroup.contains("مسلسلات") ||
                lowerGroup.contains("مسلسل") || lowerGroup.contains("سلسلة") ||
                lowerUrl.contains("/series/") || lowerUrl.contains("/show/") || lowerUrl.contains("/shows/")

        return when {
            isMovie -> {
                ParsedItem.Movie(
                    MovieEntity(
                        id = "stream_mov_${playlistId}_$num",
                        playlistId = playlistId,
                        title = name,
                        streamUrl = url,
                        posterUrl = logo,
                        category = group,
                        rating = "8.0",
                        releaseYear = "2024"
                    )
                )
            }
            isSeries -> {
                ParsedItem.Series(
                    SeriesEntity(
                        id = "stream_ser_${playlistId}_$num",
                        playlistId = playlistId,
                        title = name,
                        posterUrl = logo,
                        category = group,
                        rating = "8.2",
                        releaseYear = "2024",
                        streamUrl = url
                    )
                )
            }
            else -> {
                ParsedItem.Channel(
                    ChannelEntity(
                        id = "stream_ch_${playlistId}_$num",
                        playlistId = playlistId,
                        name = name,
                        streamUrl = url,
                        logoUrl = logo,
                        category = group,
                        channelNum = num
                    )
                )
            }
        }
    }

    private fun extractAttribute(extInf: String, attrName: String): String {
        val regex = Regex("""$attrName="([^"]*)"""", RegexOption.IGNORE_CASE)
        val match = regex.find(extInf)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }
}
