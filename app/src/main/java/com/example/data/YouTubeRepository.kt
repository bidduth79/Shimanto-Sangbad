package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class YouTubeFetchResult {
    data class Success(val videos: List<YouTubeSearchResult>) : YouTubeFetchResult()
    data class Error(val message: String, val rawCode: Int? = null) : YouTubeFetchResult()
}

class YouTubeRepository {
    private fun logWarn(msg: String) {
        try {
            Log.w("YouTubeRepository", msg)
        } catch (_: Throwable) {}
    }
    private val httpClient = SharedHttpClient.client
    private val moshi = Moshi.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(YouTubeApiService::class.java)

    // Maximum age for videos: 7 days (168 hours), while UI defaults to 48 hours filter
    private val maxVideoAgeMillis = 7L * 24L * 60L * 60L * 1000L

    private val defaultNewsChannels = listOf(
        "UCxHoBXkY88Tb8z1Ssj6CWsQ", // Somoy TV
        "UCN6sm8iHiPd0cnoUardDAnw", // Jamuna TV
        "UCATUkaOHwO9EP_W87zCiPbA", // Independent TV
        "UCtqvtAVmad5zywaziN6CbfA", // Ekattor TV
        "UCHLqIOMPk20w-6cFgkA90jw", // Channel 24
        "UCUvXoiDEKI8VZJrr58g4VAw"  // DBC News
    )

    suspend fun fetchBgbVideos(
        keywords: List<String>,
        customApiKey: String = "",
        customChannelIds: String = ""
    ): YouTubeFetchResult = withContext(Dispatchers.IO) {
        val effectiveKeywords = keywords.ifEmpty {
            listOf("বিজিবি", "বর্ডার গার্ড বাংলাদেশ", "সীমান্ত", "BGB", "BSF")
        }

        val allVideoItems = mutableListOf<YouTubeSearchResult>()
        val apiKey = customApiKey.trim().ifBlank { BuildConfig.YOUTUBE_API_KEY }
        val hasValidApiKey = apiKey.isNotBlank() && apiKey != "MY_YOUTUBE_API_KEY"
        val userChannels = customChannelIds
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "UCxAI1M5Wv1rE4XbT1gNnt7g" }

        if (hasValidApiKey) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val publishedAfter = sdf.format(Date(System.currentTimeMillis() - maxVideoAgeMillis))

                if (userChannels.isEmpty()) {
                    val batchedQueries = effectiveKeywords.chunked(5).map { chunk ->
                        chunk.joinToString(" | ")
                    }
                    supervisorScope {
                        val deferreds = batchedQueries.map { combinedQuery ->
                            async {
                                try {
                                    apiService.searchVideos(
                                        query = combinedQuery,
                                        publishedAfter = publishedAfter,
                                        maxResults = 25,
                                        apiKey = apiKey
                                    ).items
                                } catch (e: Exception) {
                                    logWarn("API search batch failed: ${e.message}")
                                    emptyList()
                                }
                            }
                        }
                        deferreds.awaitAll().forEach { allVideoItems.addAll(it) }
                    }
                } else {
                    supervisorScope {
                        val deferreds = userChannels.map { channelId ->
                            async {
                                try {
                                    val channelResponse = apiService.getChannels(
                                        channelId = channelId,
                                        apiKey = apiKey
                                    )
                                    val uploadsPlaylistId = channelResponse.items.firstOrNull()?.contentDetails?.relatedPlaylists?.uploads
                                    if (!uploadsPlaylistId.isNullOrBlank()) {
                                        val response = apiService.getPlaylistItems(
                                            playlistId = uploadsPlaylistId,
                                            maxResults = 35,
                                            apiKey = apiKey
                                        )
                                        response.items.mapNotNull { item ->
                                            val title = item.snippet?.title ?: ""
                                            val desc = item.snippet?.description ?: ""
                                            val pubAt = item.snippet?.publishedAt
                                            val isRecent = isRecentTimestamp(parseIsoTimestamp(pubAt))
                                            val matches = effectiveKeywords.any { k ->
                                                title.contains(k, ignoreCase = true) || desc.contains(k, ignoreCase = true)
                                            }
                                            if (isRecent && matches) {
                                                YouTubeSearchResult(
                                                    id = YouTubeVideoId(videoId = item.snippet?.resourceId?.videoId),
                                                    snippet = item.snippet
                                                )
                                            } else null
                                        }
                                    } else emptyList()
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        }
                        deferreds.awaitAll().forEach { allVideoItems.addAll(it) }
                    }
                }
            } catch (e: Exception) {
                logWarn("API fetch error, falling back to RSS/Web: ${e.message}")
            }
        }

        if (allVideoItems.size < 10) {
            supervisorScope {
                val channelsToPoll = if (userChannels.isNotEmpty()) userChannels else defaultNewsChannels
                val rssDeferreds = channelsToPoll.map { channelId ->
                    async { fetchChannelRssVideos(channelId, effectiveKeywords) }
                }

                val searchKeywords = effectiveKeywords.take(6)
                val webSearchDeferreds = searchKeywords.flatMap { kw ->
                    listOf(
                        // Today filter + Upload date sort
                        async { fetchYouTubeDirectSearch(kw, "CAISBAgCEAE%3D") },
                        // Upload date sort (catches 24h-7d recent videos)
                        async { fetchYouTubeDirectSearch(kw, "CAI%3D") }
                    )
                }

                rssDeferreds.awaitAll().forEach { allVideoItems.addAll(it) }
                webSearchDeferreds.awaitAll().forEach { allVideoItems.addAll(it) }
            }
        }

        // Strictly filter out old videos and sort newest first
        val validVideoItems = allVideoItems
            .filter { item ->
                val vId = item.id?.videoId
                val ts = parseIsoTimestamp(item.snippet?.publishedAt)
                !vId.isNullOrBlank() && isRecentTimestamp(ts)
            }
            .distinctBy { it.id?.videoId }
            .sortedByDescending { parseIsoTimestamp(it.snippet?.publishedAt) }

        if (validVideoItems.isNotEmpty()) {
            YouTubeFetchResult.Success(validVideoItems)
        } else {
            YouTubeFetchResult.Error(
                "সাম্প্রতিক সময়ে আপনার কীওয়ার্ড অনুযায়ী কোনো নতুন ভিডিও পাওয়া যায়নি। রিফ্রেশ করে আবার চেষ্টা করুন।"
            )
        }
    }

    private fun isRecentTimestamp(timestampMillis: Long): Boolean {
        if (timestampMillis <= 0L) return false
        val age = System.currentTimeMillis() - timestampMillis
        return age >= -600_000L && age <= maxVideoAgeMillis
    }

    private fun fetchChannelRssVideos(channelId: String, keywords: List<String>): List<YouTubeSearchResult> {
        val results = mutableListOf<YouTubeSearchResult>()
        val feedUrl = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        try {
            val request = Request.Builder()
                .url(feedUrl)
                .header("User-Agent", "Mozilla/5.0 (Android 14; Mobile)")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val xmlText = response.body?.string() ?: return@use
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    parser.setInput(StringReader(xmlText))

                    var eventType = parser.eventType
                    var insideEntry = false
                    var videoId: String? = null
                    var title: String? = null
                    var published: String? = null
                    var channelTitle: String? = null
                    var description: String? = null

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                val tagName = parser.name
                                if (tagName.equals("entry", ignoreCase = true)) {
                                    insideEntry = true
                                    videoId = null
                                    title = null
                                    published = null
                                    description = null
                                } else if (insideEntry) {
                                    when {
                                        tagName.equals("yt:videoId", ignoreCase = true) || tagName.endsWith(":videoId") -> {
                                            videoId = parser.nextText()?.trim()
                                        }
                                        tagName.equals("title", ignoreCase = true) -> {
                                            title = parser.nextText()?.trim()
                                        }
                                        tagName.equals("published", ignoreCase = true) -> {
                                            published = parser.nextText()?.trim()
                                        }
                                        tagName.equals("name", ignoreCase = true) -> {
                                            channelTitle = parser.nextText()?.trim()
                                        }
                                        tagName.equals("media:description", ignoreCase = true) || tagName.endsWith(":description") -> {
                                            description = parser.nextText()?.trim()
                                        }
                                    }
                                } else if (tagName.equals("title", ignoreCase = true) && channelTitle == null) {
                                    channelTitle = parser.nextText()?.trim()
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name.equals("entry", ignoreCase = true) && insideEntry) {
                                    insideEntry = false
                                    val vId = videoId
                                    val vTitle = title ?: ""
                                    val vDesc = description ?: ""
                                    val pubTs = parseIsoTimestamp(published)
                                    if (!vId.isNullOrBlank() && vTitle.isNotBlank() && isRecentTimestamp(pubTs)) {
                                        val matchesKeyword = keywords.isEmpty() || keywords.any { kw ->
                                            vTitle.contains(kw, ignoreCase = true) || vDesc.contains(kw, ignoreCase = true)
                                        }
                                        if (matchesKeyword) {
                                            val thumbUrl = "https://i.ytimg.com/vi/$vId/hqdefault.jpg"
                                            results.add(
                                                YouTubeSearchResult(
                                                    id = YouTubeVideoId(videoId = vId),
                                                    snippet = YouTubeSnippet(
                                                        title = vTitle,
                                                        description = vDesc,
                                                        channelTitle = channelTitle ?: "YouTube News",
                                                        publishedAt = published,
                                                        thumbnails = YouTubeThumbnails(
                                                            high = YouTubeThumbnail(url = thumbUrl),
                                                            medium = YouTubeThumbnail(url = thumbUrl),
                                                            default = YouTubeThumbnail(url = thumbUrl)
                                                        )
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }
        } catch (e: Exception) {
            logWarn("RSS fetch failed for $channelId: ${e.message}")
        }
        return results
    }

    private fun fetchYouTubeDirectSearch(keyword: String, spParam: String = "CAISBAgCEAE%3D"): List<YouTubeSearchResult> {
        val results = mutableListOf<YouTubeSearchResult>()
        try {
            val encoded = URLEncoder.encode(keyword, "UTF-8")
            val searchUrl = "https://www.youtube.com/results?search_query=$encoded&sp=$spParam"
            val request = Request.Builder()
                .url(searchUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
                .header("Accept-Language", "en-US,en;q=0.9")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: return@use
                    val marker = "var ytInitialData = "
                    val startIdx = html.indexOf(marker)
                    if (startIdx != -1) {
                        val jsonStart = startIdx + marker.length
                        val endIdx = html.indexOf(";</script>", jsonStart)
                        if (endIdx != -1) {
                            val jsonStr = html.substring(jsonStart, endIdx)
                            parseYtInitialData(jsonStr, keyword, results)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logWarn("Direct search failed for $keyword: ${e.message}")
        }
        return results
    }

    private val videoIdRegex = """^\{"videoId":"([a-zA-Z0-9_-]{11})"""".toRegex()
    private val titleRunsRegex = """"title":\{"runs":\[\{"text":"((?:\\.|[^"\\])*)"""".toRegex()
    private val titleSimpleRegex = """"title":\{.*?"simpleText":"((?:\\.|[^"\\])*)"""".toRegex()
    private val channelRegex = """"ownerText":\{"runs":\[\{"text":"((?:\\.|[^"\\])*)"""".toRegex()
    private val publishedTimeSimpleRegex = """"publishedTimeText":\{"simpleText":"([^"]+)"\}""".toRegex()
    private val publishedTimeRunsRegex = """"publishedTimeText":\{"runs":\[\{"text":"([^"]+)"\}""".toRegex()

    private fun parseYtInitialData(
        jsonStr: String,
        searchedKeyword: String,
        outList: MutableList<YouTubeSearchResult>
    ) {
        try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            // Split by "videoRenderer": so each chunk strictly belongs to one single video
            val blocks = jsonStr.split("\"videoRenderer\":")
            for (i in 1 until minOf(blocks.size, 25)) {
                val block = blocks[i].take(4000)
                val videoId = videoIdRegex.find(block)?.groupValues?.get(1) ?: continue

                val rawTitle = (titleRunsRegex.find(block)?.groupValues?.get(1)
                    ?: titleSimpleRegex.find(block)?.groupValues?.get(1)
                    ?: continue)
                    .replace("\\\"", "\"")
                    .replace("\\u0026", "&")

                // Extract relative time strictly within this videoRenderer block
                val relTimeStr = publishedTimeSimpleRegex.find(block)?.groupValues?.get(1)
                    ?: publishedTimeRunsRegex.find(block)?.groupValues?.get(1)
                    ?: ""
                val estimatedTimestamp = parseRelativeYouTubeTime(relTimeStr) ?: continue

                val channelName = (channelRegex.find(block)?.groupValues?.get(1) ?: "YouTube News")
                    .replace("\\\"", "\"")
                    .replace("\\u0026", "&")

                // Reject unrelated shelf videos ("For you" / "Recommended") that don't match the keyword or border context
                val coreTerms = listOf(searchedKeyword, "বিজিবি", "সীমান্ত", "বর্ডার", "BGB", "BSF", "বিএসএফ")
                val isRelevant = coreTerms.any { term ->
                    term.isNotBlank() && (rawTitle.contains(term, ignoreCase = true) || channelName.contains(term, ignoreCase = true))
                }
                if (!isRelevant) continue

                val thumbUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

                outList.add(
                    YouTubeSearchResult(
                        id = YouTubeVideoId(videoId = videoId),
                        snippet = YouTubeSnippet(
                            title = rawTitle,
                            description = rawTitle,
                            channelTitle = channelName,
                            publishedAt = isoFormat.format(Date(estimatedTimestamp)),
                            thumbnails = YouTubeThumbnails(
                                high = YouTubeThumbnail(url = thumbUrl),
                                medium = YouTubeThumbnail(url = thumbUrl),
                                default = YouTubeThumbnail(url = thumbUrl)
                            )
                        )
                    )
                )
            }
        } catch (e: Exception) {
            logWarn("Error parsing ytInitialData: ${e.message}")
        }
    }

    /**
     * Converts YouTube's relative time string (both abbreviated like "6h ago", "1d ago", "35m ago"
     * and full like "6 hours ago", "1 day ago", "35 minutes ago", or Bengali equivalents)
     * into a real epoch timestamp, and returns null if the video is older than 7 days.
     */
    private fun parseRelativeYouTubeTime(relText: String): Long? {
        if (relText.isBlank()) return null

        // Convert any Bengali digits ('০'..'৯') to ASCII ('0'..'9') first
        val normalized = buildString(relText.length) {
            for (ch in relText) {
                if (ch in '০'..'৯') {
                    append(('0'.code + (ch.code - '০'.code)).toChar())
                } else {
                    append(ch)
                }
            }
        }.lowercase(Locale.ENGLISH).trim()

        // Immediately reject weeks, months, years (both full words and abbreviations like "2w ago", "3mo ago", "1y ago")
        if (normalized.contains("week") || normalized.contains("month") || normalized.contains("year") ||
            normalized.contains("সপ্তাহ") || normalized.contains("মাস") || normalized.contains("বছর") ||
            Regex("""\b\d+\s*(w|wk|wks|mo|mos|yr|yrs|y)\b""").containsMatchIn(normalized)
        ) {
            return null
        }

        val now = System.currentTimeMillis()
        if (normalized.contains("just now") || normalized.contains("এইমাত্র") || normalized.contains("সদ্য")) {
            return now - 30_000L
        }

        val num = Regex("""(\d+)""").find(normalized)?.groupValues?.get(1)?.toLongOrNull() ?: return null

        return when {
            normalized.contains("second") || normalized.contains("sec") || normalized.contains("সেকেন্ড") ||
                Regex("""\b\d+\s*s\b""").containsMatchIn(normalized) -> {
                now - num * 1000L
            }
            normalized.contains("minute") || normalized.contains("min") || normalized.contains("মিনিট") ||
                Regex("""\b\d+\s*m\b""").containsMatchIn(normalized) -> {
                now - num * 60_000L
            }
            normalized.contains("hour") || normalized.contains("hr") || normalized.contains("ঘণ্টা") ||
                Regex("""\b\d+\s*h\b""").containsMatchIn(normalized) -> {
                if (num <= 168L) now - num * 3_600_000L else null
            }
            normalized.contains("day") || normalized.contains("দিন") ||
                Regex("""\b\d+\s*d\b""").containsMatchIn(normalized) -> {
                if (num <= 7L) {
                    now - num * 24L * 3_600_000L
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun parseIsoTimestamp(pubAt: String?): Long {
        if (pubAt.isNullOrBlank()) return 0L
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val cleaned = pubAt.take(19)
            format.parse(cleaned)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
