package com.example.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

sealed class YouTubeFetchResult {
    data class Success(val videos: List<YouTubeSearchResult>) : YouTubeFetchResult()
    data class Error(val message: String, val rawCode: Int? = null) : YouTubeFetchResult()
}

/**
 * Bangladeshi news videos: YouTube Data API (quota-guarded) with a free web fallback when no key is set.
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val httpClient: OkHttpClient,
    private val apiService: YouTubeApiService,
    @param:ApplicationContext private val context: Context
) {
    init {
        QuotaGuard.attach(context)
    }

    /** API units spent today (YouTube's day, which resets at midnight Pacific time). */
    val quotaUsedToday: StateFlow<Int> get() = QuotaGuard.usedToday

    /** Free daily allowance from Google and the share of it the app lets itself use. */
    val dailyQuotaLimit: Int get() = QuotaGuard.FREE_DAILY_UNITS
    val dailyQuotaBudget: Int get() = QuotaGuard.DAILY_UNIT_BUDGET

    private fun logWarn(msg: String) {
        try {
            Log.w("YouTubeRepository", msg)
        } catch (_: Throwable) {}
    }

    /**
     * At most this many YouTube requests at a time. ~150 channel feeds fired at once filled the
     * shared HTTP client's queue and held back the news refresh.
     */
    private val networkSlots = kotlinx.coroutines.sync.Semaphore(6)

    // Maximum age for videos: 7 days (168 hours), while UI defaults to 48 hours filter
    private val maxVideoAgeMillis = 7L * 24L * 60L * 60L * 1000L

    /**
     * Collects recent keyword videos from every source at once. Each source hands its videos to
     * [onPartial] the moment it finishes, so the screen fills in after a couple of seconds instead
     * of waiting for the slowest of ~70 channels. The returned result is the complete, de-duplicated
     * list with channel countries filled in.
     */
    suspend fun fetchBgbVideos(
        keywords: List<String>,
        customApiKey: String = "",
        customChannelIds: String = "",
        onPartial: suspend (List<YouTubeSearchResult>) -> Unit = {}
    ): YouTubeFetchResult = withContext(Dispatchers.IO) {
        val effectiveKeywords = keywords.ifEmpty {
            listOf("বিজিবি", "বর্ডার গার্ড বাংলাদেশ", "সীমান্ত", "BGB", "BSF")
        }

        val collected = java.util.Collections.synchronizedList(mutableListOf<YouTubeSearchResult>())
        val emitLock = kotlinx.coroutines.sync.Mutex()
        val apiKey = customApiKey.trim()
        val hasValidApiKey = apiKey.isNotBlank() && apiKey != "MY_YOUTUBE_API_KEY"
        val userChannels = customChannelIds
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "UCxAI1M5Wv1rE4XbT1gNnt7g" }
        val channels = (userChannels + NewsSources.youtubeNewsChannels + NewsSources.youtubeIndianChannels).distinct()

        suspend fun deliver(items: List<YouTubeSearchResult>) {
            if (items.isEmpty()) return
            collected.addAll(items)
            val valid = validOnly(items, effectiveKeywords)
                .map { it.withCountry(QuotaGuard.channelCountries[it.snippet?.channelId]) }
            if (valid.isNotEmpty()) {
                // one at a time, so the receiver never sees two batches interleaved
                emitLock.lock()
                try {
                    runCatching { onPartial(valid) }
                } finally {
                    emitLock.unlock()
                }
            }
        }

        supervisorScope {
            if (hasValidApiKey) {
                // Quota plan (free tier = 10,000 units/day):
                //  - Each channel's uploads playlist costs 1 unit and is cached for CHANNEL_CACHE_MS.
                //  - Keyword searches cost 100 units per page and run at most once per SEARCH_INTERVAL_MS.
                //  - QuotaGuard stops all API calls before the daily budget is exhausted.
                channels.forEach { channelId ->
                    launch {
                        deliver(
                            networkSlots.withPermit { fetchChannelUploadsViaApi(channelId, apiKey) }.map { snippet ->
                                YouTubeSearchResult(id = YouTubeVideoId(videoId = snippet.resourceId?.videoId), snippet = snippet)
                            }
                        )
                    }
                }
                launch { deliver(networkSlots.withPermit { fetchKeywordSearchViaApi(effectiveKeywords, apiKey) }) }
            }

            // Free sources run every time, API key or not: channel RSS feeds and the public search
            // page cost no quota and catch videos the API's 50-result pages leave out.
            channels.forEach { channelId ->
                launch { deliver(networkSlots.withPermit { fetchChannelRssVideos(channelId, effectiveKeywords) }) }
            }
            effectiveKeywords.take(8).forEach { kw ->
                launch { deliver(networkSlots.withPermit { fetchYouTubeDirectSearch(kw, "CAISBAgCEAE%3D") }) } // today, newest first
                launch { deliver(networkSlots.withPermit { fetchYouTubeDirectSearch(kw, "CAI%3D") }) }         // newest first (up to 7 days)
            }
        }

        val validVideoItems = validOnly(collected.toList(), effectiveKeywords)
        val withCountries = if (hasValidApiKey) withChannelCountries(validVideoItems, apiKey) else {
            validVideoItems.map { it.withCountry(QuotaGuard.channelCountries[it.snippet?.channelId]) }
        }

        if (withCountries.isNotEmpty()) {
            YouTubeFetchResult.Success(withCountries)
        } else {
            YouTubeFetchResult.Error(
                "সাম্প্রতিক সময়ে আপনার কীওয়ার্ড অনুযায়ী কোনো নতুন ভিডিও পাওয়া যায়নি। রিফ্রেশ করে আবার চেষ্টা করুন।"
            )
        }
    }

    /**
     * Recent videos whose TITLE is about one of the keywords (search results match descriptions,
     * tags and other languages too: "BGB" also finds German civil-code videos), newest first.
     */
    private fun validOnly(items: List<YouTubeSearchResult>, keywords: List<String>): List<YouTubeSearchResult> =
        items
            .filter { item ->
                val vId = item.id?.videoId
                val ts = parseIsoTimestamp(item.snippet?.publishedAt)
                !vId.isNullOrBlank() && isRecentTimestamp(ts) &&
                    KeywordMatcher.matches(item.snippet?.title.orEmpty(), keywords)
            }
            .distinctBy { it.id?.videoId }
            .sortedByDescending { parseIsoTimestamp(it.snippet?.publishedAt) }

    /**
     * Latest uploads of one channel via its "UU…" uploads playlist (1 quota unit).
     * Served from cache while fresh; on failure or when the budget is spent, the last good copy is reused.
     */
    private suspend fun fetchChannelUploadsViaApi(channelId: String, apiKey: String): List<YouTubeSnippet> {
        val now = System.currentTimeMillis()
        val cached = QuotaGuard.channelCache[channelId]
        if (cached != null && now - cached.first < QuotaGuard.CHANNEL_CACHE_MS) return cached.second

        return try {
            val uploadsPlaylistId = if (channelId.startsWith("UC")) {
                "UU" + channelId.removePrefix("UC")
            } else {
                // Not a standard channel ID: resolve it once (1 unit) and remember the result
                QuotaGuard.uploadsPlaylistIds[channelId] ?: run {
                    if (!QuotaGuard.tryConsume(1)) return cached?.second.orEmpty()
                    apiService.getChannels(channelId = channelId, apiKey = apiKey)
                        .items.firstOrNull()?.contentDetails?.relatedPlaylists?.uploads
                        ?.also { QuotaGuard.uploadsPlaylistIds[channelId] = it }
                } ?: return cached?.second.orEmpty()
            }
            if (!QuotaGuard.tryConsume(1)) return cached?.second.orEmpty()
            val snippets = apiService.getPlaylistItems(
                playlistId = uploadsPlaylistId,
                maxResults = 50,
                apiKey = apiKey
            ).items.mapNotNull { it.snippet }
            QuotaGuard.channelCache[channelId] = now to snippets
            snippets
        } catch (e: Exception) {
            logWarn("Uploads fetch failed for $channelId: ${e.message}")
            cached?.second.orEmpty()
        }
    }

    /**
     * Keyword search across all of YouTube, run at most once per SEARCH_INTERVAL_MS (between runs
     * the previous results are returned). Each keyword group gets its own query limited to the last
     * SEARCH_WINDOW_MS and sorted newest first, so busy days are not cut off by one shared 50-result
     * page. Each page costs 100 units; a second page is read only when the first was full of recent
     * videos.
     */
    private suspend fun fetchKeywordSearchViaApi(keywords: List<String>, apiKey: String): List<YouTubeSearchResult> {
        val now = System.currentTimeMillis()
        if (now - QuotaGuard.lastSearchAt < QuotaGuard.SEARCH_INTERVAL_MS) return QuotaGuard.lastSearchResults
        QuotaGuard.lastSearchAt = now
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val publishedAfter = sdf.format(Date(now - QuotaGuard.SEARCH_WINDOW_MS))
        val results = mutableListOf<YouTubeSearchResult>()
        var anySucceeded = false
        for (query in searchQueries(keywords)) {
            var pageToken: String? = null
            for (page in 0 until QuotaGuard.MAX_SEARCH_PAGES) {
                if (!QuotaGuard.tryConsume(100)) break
                val response = try {
                    apiService.searchVideos(
                        query = query,
                        publishedAfter = publishedAfter,
                        maxResults = 50,
                        pageToken = pageToken,
                        apiKey = apiKey
                    )
                } catch (e: Exception) {
                    logWarn("API search failed for '$query': ${e.message}")
                    break
                }
                anySucceeded = true
                results += response.items
                pageToken = response.nextPageToken
                if (pageToken == null || response.items.size < 50) break
            }
        }
        if (!anySucceeded) return QuotaGuard.lastSearchResults
        QuotaGuard.lastSearchResults = results
        return results
    }

    /**
     * Groups keywords into at most MAX_SEARCH_QUERIES "a | b | c" queries (YouTube's OR syntax),
     * pairing each Bengali keyword with its English aliases so one query covers both spellings.
     */
    private fun searchQueries(keywords: List<String>): List<String> {
        val terms = keywords.flatMap { KeywordMatcher.termsFor(it) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
        if (terms.isEmpty()) return emptyList()
        val perQuery = ((terms.size + QuotaGuard.MAX_SEARCH_QUERIES - 1) / QuotaGuard.MAX_SEARCH_QUERIES)
            .coerceIn(3, 6)
        return terms.chunked(perQuery).take(QuotaGuard.MAX_SEARCH_QUERIES).map { it.joinToString(" | ") }
    }

    /**
     * Adds each uploader's declared country (used to put Indian channels in their own chip).
     * Unknown channels are looked up 50 at a time for 1 quota unit per batch and remembered for
     * good, so after the first day this costs almost nothing.
     */
    private suspend fun withChannelCountries(videos: List<YouTubeSearchResult>, apiKey: String): List<YouTubeSearchResult> {
        val known = YouTubeChannelDirectory.knownIds
        val unknown = videos.mapNotNull { it.snippet?.channelId }
            .filter { it !in known && !QuotaGuard.channelCountries.containsKey(it) }
            .distinct()
        for (batch in unknown.chunked(50)) {
            if (!QuotaGuard.tryConsume(1)) break
            try {
                val found = apiService.getChannels(
                    part = "snippet",
                    channelId = batch.joinToString(","),
                    maxResults = 50,
                    apiKey = apiKey
                ).items.associate { it.id.orEmpty() to it.snippet?.country.orEmpty() }
                // Channels without a declared country are stored as "" so they are not asked again
                batch.forEach { QuotaGuard.rememberCountry(it, found[it].orEmpty()) }
            } catch (e: Exception) {
                logWarn("Channel country lookup failed: ${e.message}")
                break
            }
        }
        return videos.map { it.withCountry(QuotaGuard.channelCountries[it.snippet?.channelId]) }
    }

    private fun YouTubeSearchResult.withCountry(country: String?): YouTubeSearchResult =
        if (country.isNullOrEmpty() || snippet == null) this
        else copy(snippet = snippet.copy(channelCountry = country))

    /**
     * Process-wide YouTube quota bookkeeping shared by every repository instance. Today's spend is
     * saved so an app restart neither forgets it nor overspends.
     *
     * Rough daily cost with the app open all day: channels ~140 units every hour (~3,400) plus
     * keyword searches 3 queries x up to 2 pages every 2 hours (~3,600) — under the 8,000 budget,
     * and the guard stops everything before it is reached anyway.
     */
    private object QuotaGuard {
        const val CHANNEL_CACHE_MS = 60L * 60L * 1000L        // re-read a channel at most every hour
        const val SEARCH_INTERVAL_MS = 2L * 60L * 60L * 1000L // keyword search at most every 2 hours
        const val SEARCH_WINDOW_MS = 24L * 60L * 60L * 1000L  // searches look at the last 24 hours
        const val MAX_SEARCH_QUERIES = 3
        const val MAX_SEARCH_PAGES = 2
        const val FREE_DAILY_UNITS = 10_000
        const val DAILY_UNIT_BUDGET = 8_000                   // stay safely under the free 10,000

        val channelCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<YouTubeSnippet>>>()
        val uploadsPlaylistIds = java.util.concurrent.ConcurrentHashMap<String, String>()
        @Volatile var lastSearchAt = 0L
        @Volatile var lastSearchResults: List<YouTubeSearchResult> = emptyList()

        /** Channel ID -> declared country ("" when the channel declares none); kept across restarts. */
        val channelCountries = java.util.concurrent.ConcurrentHashMap<String, String>()

        fun rememberCountry(channelId: String, country: String) {
            channelCountries[channelId] = country
            prefs?.edit()?.putString("country_$channelId", country)?.apply()
        }

        private val _usedToday = MutableStateFlow(0)
        val usedToday: StateFlow<Int> = _usedToday.asStateFlow()

        private var prefs: android.content.SharedPreferences? = null
        private var quotaDay = ""
        private var unitsUsed = 0

        @Synchronized
        fun attach(context: Context) {
            if (prefs != null) return
            prefs = context.getSharedPreferences("youtube_quota", Context.MODE_PRIVATE).also {
                quotaDay = it.getString("day", "") ?: ""
                unitsUsed = it.getInt("units", 0)
                it.all.forEach { (key, value) ->
                    if (key.startsWith("country_") && value is String) channelCountries[key.removePrefix("country_")] = value
                }
            }
            rollOverIfNewDay()
            _usedToday.value = unitsUsed
        }

        /** Reserves [units] from today's budget. */
        @Synchronized
        fun tryConsume(units: Int): Boolean {
            rollOverIfNewDay()
            if (unitsUsed + units > DAILY_UNIT_BUDGET) return false
            unitsUsed += units
            _usedToday.value = unitsUsed
            prefs?.edit()?.putString("day", quotaDay)?.putInt("units", unitsUsed)?.apply()
            return true
        }

        private fun rollOverIfNewDay() {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("America/Los_Angeles")
            }.format(Date())
            if (today != quotaDay) {
                quotaDay = today
                unitsUsed = 0
                _usedToday.value = 0
            }
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
                                                        channelId = channelId,
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
                // Without a consent cookie YouTube bounces EU/unknown regions between consent pages
                .header("Cookie", "SOCS=CAI; CONSENT=YES+cb")
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
    private val ownerBrowseIdRegex = """"browseId":"(UC[\w-]{22})"""".toRegex()
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

                val ownerStart = block.indexOf("\"ownerText\"")
                val ownerChannelId = if (ownerStart >= 0) {
                    ownerBrowseIdRegex.find(block, ownerStart)?.groupValues?.get(1)
                } else null
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
                            channelId = ownerChannelId,
                            publishedAt = isoFormat.format(Date(estimatedTimestamp)),
                            timeIsEstimate = true,
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

    /** Epoch millis of a YouTube/RSS timestamp, or 0 when it cannot be read. */
    fun parseIsoTimestamp(pubAt: String?): Long {
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
