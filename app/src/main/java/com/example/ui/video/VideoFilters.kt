package com.example.ui.video

import com.example.data.ChannelInfo
import com.example.data.SourceKind
import com.example.data.VideoRegion
import com.example.data.YouTubeChannelDirectory
import com.example.data.YouTubeSearchResult
import com.example.data.YouTubeSnippet

/**
 * Chips after the time chip. The "all" tab offers watched/unwatched; a country tab offers the kind of
 * source (TV, national, local) and a separate "only unwatched" toggle.
 */
enum class VideoFilter { ALL, TV, NATIONAL, LOCAL, OTHER, UNREAD, READ }

/** Country tabs, in the order of the shared tab bar (0 = all). */
val VideoTabs: List<VideoRegion?> = listOf(null, VideoRegion.BANGLADESH, VideoRegion.INDIA, VideoRegion.MYANMAR, VideoRegion.INTERNATIONAL)

/** The link stored when a video is watched; shares the "visited" list with news. */
fun watchedLinkOf(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

data class RegionCounts(val all: Int, val bangladesh: Int, val india: Int, val myanmar: Int, val international: Int)

data class VideoCounts(val all: Int, val tv: Int, val national: Int, val local: Int, val other: Int, val unread: Int, val read: Int)

/**
 * Sorts videos by country ([VideoRegion]) and kind (outlet vs local uploader), and by watched state.
 *
 * Search results scraped from the web sometimes lack a channel ID; such a video still counts as a
 * Bangladeshi outlet when another video in the list from a known channel carries the same name.
 */
class VideoClassifier(
    videos: List<YouTubeSearchResult>,
    private val mediaChannelIds: Set<String>,
    private val watchedLinks: Set<String>
) {
    private val mediaTitles: Set<String> = videos
        .filter { it.snippet?.channelId in mediaChannelIds }
        .mapNotNull { it.snippet?.channelTitle?.trim()?.lowercase() }
        .toSet()

    private val infos = HashMap<String, ChannelInfo>()

    fun infoOf(video: YouTubeSearchResult): ChannelInfo {
        val snippet = video.snippet ?: return ChannelInfo(VideoRegion.BANGLADESH, SourceKind.OTHER)
        val key = video.id?.videoId ?: return compute(snippet)
        return infos.getOrPut(key) { compute(snippet) }
    }

    private fun compute(snippet: YouTubeSnippet): ChannelInfo {
        if (snippet.channelId == null && snippet.channelTitle?.trim()?.lowercase() in mediaTitles) {
            return ChannelInfo(VideoRegion.BANGLADESH, SourceKind.NATIONAL)
        }
        return YouTubeChannelDirectory.infoOf(
            channelId = snippet.channelId,
            channelTitle = snippet.channelTitle,
            videoTitle = snippet.title,
            country = snippet.channelCountry,
            bangladeshMediaIds = mediaChannelIds
        )
    }

    fun isWatched(video: YouTubeSearchResult): Boolean =
        video.id?.videoId?.let { watchedLinkOf(it) in watchedLinks } == true

    fun inRegion(video: YouTubeSearchResult, region: VideoRegion?): Boolean =
        region == null || infoOf(video).region == region

    fun matches(video: YouTubeSearchResult, filter: VideoFilter): Boolean = when (filter) {
        VideoFilter.ALL -> true
        VideoFilter.TV -> infoOf(video).kind == SourceKind.TV
        VideoFilter.NATIONAL -> infoOf(video).kind == SourceKind.NATIONAL
        VideoFilter.LOCAL -> infoOf(video).kind == SourceKind.LOCAL
        VideoFilter.OTHER -> infoOf(video).kind == SourceKind.OTHER
        VideoFilter.UNREAD -> !isWatched(video)
        VideoFilter.READ -> isWatched(video)
    }

    fun regionCounts(videos: List<YouTubeSearchResult>): RegionCounts {
        val byRegion = videos.groupingBy { infoOf(it).region }.eachCount()
        return RegionCounts(
            all = videos.size,
            bangladesh = byRegion[VideoRegion.BANGLADESH] ?: 0,
            india = byRegion[VideoRegion.INDIA] ?: 0,
            myanmar = byRegion[VideoRegion.MYANMAR] ?: 0,
            international = byRegion[VideoRegion.INTERNATIONAL] ?: 0
        )
    }

    fun counts(videos: List<YouTubeSearchResult>): VideoCounts {
        val byKind = videos.groupingBy { infoOf(it).kind }.eachCount()
        val read = videos.count(::isWatched)
        return VideoCounts(
            all = videos.size,
            tv = byKind[SourceKind.TV] ?: 0,
            national = byKind[SourceKind.NATIONAL] ?: 0,
            local = byKind[SourceKind.LOCAL] ?: 0,
            other = byKind[SourceKind.OTHER] ?: 0,
            unread = videos.size - read,
            read = read
        )
    }
}
