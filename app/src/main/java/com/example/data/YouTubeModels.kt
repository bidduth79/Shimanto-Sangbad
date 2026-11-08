package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchResult> = emptyList(),
    val nextPageToken: String? = null
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
    /** Uploader's channel; used to tell newspaper/TV channels from local uploaders. */
    val channelId: String? = null,
    /** Country the channel declares on YouTube ("BD", "IN"…); filled in by the app, not by search. */
    val channelCountry: String? = null,
    /** True for web-search results, whose time comes from "3 hours ago" text. */
    val timeIsEstimate: Boolean = false,
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
    val id: String? = null,
    val contentDetails: YouTubeChannelContentDetails? = null,
    val snippet: YouTubeChannelSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelSnippet(
    val country: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelContentDetails(
    val relatedPlaylists: YouTubeRelatedPlaylists? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeRelatedPlaylists(
    val uploads: String? = null
)
