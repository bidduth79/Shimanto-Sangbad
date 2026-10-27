package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 15,
        @Query("order") order: String = "date",
        @Query("channelId") channelId: String? = null,
        @Query("publishedAfter") publishedAfter: String? = null,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse

    @GET("playlistItems")
    suspend fun getPlaylistItems(
        @Query("part") part: String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("key") apiKey: String
    ): YouTubePlaylistResponse

    @GET("channels")
    suspend fun getChannels(
        @Query("part") part: String = "contentDetails",
        @Query("id") channelId: String,
        @Query("key") apiKey: String
    ): YouTubeChannelResponse
}
