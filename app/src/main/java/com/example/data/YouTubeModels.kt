package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchResult> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResult(
    val id: YouTubeVideoId? = null,
    val snippet: YouTubeSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoId(
    val kind: String? = null,
    val videoId: String? = null,
    val channelId: String? = null,
    val playlistId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
    val title: String? = null,
    val description: String? = null,
    val channelTitle: String? = null,
    val publishedAt: String? = null,
    val thumbnails: YouTubeThumbnails? = null,
    val resourceId: YouTubeResourceId? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
    val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistResponse(
    val items: List<YouTubePlaylistItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItem(
    val snippet: YouTubeSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
    val high: YouTubeThumbnail? = null,
    val medium: YouTubeThumbnail? = null,
    val default: YouTubeThumbnail? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelResponse(
    val items: List<YouTubeChannelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelItem(
    val contentDetails: YouTubeChannelContentDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelContentDetails(
    val relatedPlaylists: YouTubeRelatedPlaylists? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeRelatedPlaylists(
    val uploads: String? = null
)
