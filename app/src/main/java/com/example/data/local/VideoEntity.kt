package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.YouTubeSearchResult
import com.example.data.YouTubeSnippet
import com.example.data.YouTubeThumbnail
import com.example.data.YouTubeThumbnails
import com.example.data.YouTubeVideoId

/** A cached news video, so the video tab opens instantly with the last list. */
@Entity(tableName = "videos", indices = [Index("publishedMillis")])
data class VideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val description: String,
    val channelTitle: String,
    val channelId: String?,
    val channelCountry: String?,
    val publishedAt: String?,
    val publishedMillis: Long,
    /** Web-search times are estimates ("3 hours ago"); API/RSS times are exact. */
    val timeIsEstimate: Boolean,
    val thumbnailUrl: String?,
    val fetchedAt: Long
) {
    fun toResult(): YouTubeSearchResult {
        val thumb = thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        return YouTubeSearchResult(
            id = YouTubeVideoId(videoId = videoId),
            snippet = YouTubeSnippet(
                title = title,
                description = description,
                channelTitle = channelTitle,
                channelId = channelId,
                channelCountry = channelCountry,
                timeIsEstimate = timeIsEstimate,
                publishedAt = publishedAt,
                thumbnails = YouTubeThumbnails(
                    high = YouTubeThumbnail(thumb),
                    medium = YouTubeThumbnail(thumb),
                    default = YouTubeThumbnail(thumb)
                )
            )
        )
    }

    /**
     * Combines a fresh copy of the same video with this stored one: an exact time beats an
     * estimate, and a known channel ID or country is never replaced by a missing one.
     */
    fun mergedWith(fresh: VideoEntity): VideoEntity {
        val keepOldTime = timeIsEstimate.not() && fresh.timeIsEstimate
        return fresh.copy(
            channelId = fresh.channelId ?: channelId,
            channelCountry = fresh.channelCountry ?: channelCountry,
            publishedAt = if (keepOldTime) publishedAt else fresh.publishedAt,
            publishedMillis = if (keepOldTime) publishedMillis else fresh.publishedMillis,
            timeIsEstimate = timeIsEstimate && fresh.timeIsEstimate,
            description = fresh.description.ifBlank { description }
        )
    }

    companion object {
        fun from(result: YouTubeSearchResult, publishedMillis: Long, now: Long): VideoEntity? {
            val id = result.id?.videoId?.takeIf { it.isNotBlank() } ?: return null
            val s = result.snippet ?: return null
            return VideoEntity(
                videoId = id,
                title = s.title.orEmpty(),
                description = s.description.orEmpty(),
                channelTitle = s.channelTitle.orEmpty(),
                channelId = s.channelId,
                channelCountry = s.channelCountry,
                publishedAt = s.publishedAt,
                publishedMillis = publishedMillis,
                timeIsEstimate = s.timeIsEstimate,
                thumbnailUrl = s.thumbnails?.high?.url ?: s.thumbnails?.medium?.url ?: s.thumbnails?.default?.url,
                fetchedAt = now
            )
        }
    }
}
