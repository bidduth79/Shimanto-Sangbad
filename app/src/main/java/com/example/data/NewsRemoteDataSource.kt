package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to the outside world: news search engines (Google News, Bing) and newspapers' own RSS feeds.
 * Returns raw articles; filtering and storage live in [NewsRepository]. Every request's outcome is
 * noted in [SourceHealth], and known time-zone mistakes of sources are repaired by [FeedClock].
 */
@Singleton
class NewsRemoteDataSource @Inject constructor(
    private val httpClient: OkHttpClient
) {

    /**
     * One Google News RSS search. [keyword] is a complete Google query (operators such as intitle:,
     * OR and -site: are allowed); [whenWindow] limits it to recent items ("3h", "8h", "1d").
     * Google returns at most 100 items per search, ranked by relevance rather than by date.
     */
    suspend fun fetchNews(
        keyword: String,
        hl: String = "bn",
        gl: String = "BD",
        ceid: String = "BD:bn",
        whenWindow: String = "1d"
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val cacheBuster = System.currentTimeMillis() / 30_000L
        val urlString = "https://news.google.com/rss/search?q=$encodedKeyword+when:$whenWindow&hl=$hl&gl=$gl&ceid=$ceid&cb=$cacheBuster"
        val fetched = fetchRssFromUrl(urlString, defaultSource = "Google News", requireValidTimestamp = true)
        SourceHealth.record(SourceHealth.GOOGLE, "Google News", SourceHealth.Kind.SEARCH, fetched.error, fetched.articles)
        fetched.articles
            .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
            .map { article ->
                val cleanSource = if (article.source.isBlank() || article.source == "Unknown") "Google News" else article.source
                val formattedSource = if (cleanSource.startsWith("Google", ignoreCase = true)) {
                    cleanSource
                } else {
                    "Google • $cleanSource"
                }
                article.copy(
                    source = formattedSource,
                    matchedKeyword = keyword
                )
            }
    }

    /**
     * Fetches news from Microsoft Bing News RSS Search for any keyword sorted strictly by newest date (last 24h).
     */
    suspend fun fetchBingNews(
        keyword: String,
        market: String = "en-IN",
        matchedTag: String = keyword
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val cleanQuery = "$keyword -site:facebook.com -site:fb.com"
        val encodedKeyword = URLEncoder.encode(cleanQuery, "UTF-8")
        val cacheBuster = System.currentTimeMillis() / 30_000L
        val urlString = "https://www.bing.com/news/search?q=$encodedKeyword&qft=interval%3d%227%22+sortbydate%3d%221%22&setmkt=$market&format=rss&cb=$cacheBuster"
        val fetched = fetchRssFromUrl(urlString, defaultSource = "Bing News", requireValidTimestamp = true)
        // Bing states Pacific time under a "GMT" label: without this every item looks 8 hours old
        val articles = FeedClock.fixBing(fetched.articles)
        SourceHealth.record(SourceHealth.BING, "Bing News", SourceHealth.Kind.SEARCH, fetched.error, articles)
        articles
            .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
            .map { article ->
                val cleanSource = if (article.source.isBlank() || article.source == "Unknown") "Bing News" else article.source
                val formattedSource = if (cleanSource.startsWith("Bing", ignoreCase = true)) {
                    cleanSource
                } else {
                    "Bing • $cleanSource"
                }
                article.copy(
                    source = formattedSource,
                    matchedKeyword = matchedTag
                )
            }
    }

    /**
     * Fetches articles directly from a user-configured Custom RSS or Atom feed URL.
     */
    suspend fun fetchCustomRssFeed(feed: RssFeedItem): List<NewsArticle> = withContext(Dispatchers.IO) {
        val fetched = fetchRssFromUrl(feed.url, defaultSource = feed.title, requireValidTimestamp = false)
        val now = System.currentTimeMillis()
        SourceHealth.record(SourceHealth.customKey(feed.url), feed.title, SourceHealth.Kind.CUSTOM, fetched.error, fetched.articles, now)
        FeedClock.fixLocalLabelledAsUtc(
            feedUrl = feed.url,
            knownBroken = false,
            localOffsetMillis = FeedClock.BANGLADESH_OFFSET_MILLIS,
            articles = datedOrStamped(fetched.articles, now),
            now = now
        )
            .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
            .map { article ->
                val src = if (article.source.isBlank() || article.source == "Unknown") {
                    feed.title
                } else {
                    article.source
                }
                article.copy(source = src, category = "rss", matchedKeyword = feed.title)
            }
    }

    /**
     * Undated items of a custom feed. In a feed that normally dates its items, an undated one is of
     * unknown age (it can be months old), so it is left out instead of being shown as "just now".
     * A feed that gives no dates at all keeps working: its items are stamped with the fetch time.
     */
    internal fun datedOrStamped(articles: List<NewsArticle>, now: Long): List<NewsArticle> {
        val dated = articles.filter { it.timestamp > 0L }
        return if (dated.size * 2 >= articles.size) dated else articles.map { if (it.timestamp > 0L) it else it.copy(timestamp = now) }
    }

    /**
     * Reads every built-in newspaper RSS feed of [region] directly (free, no search engine in between)
     * and keeps the articles whose headline matches one of [keywords] or its English variants.
     */
    suspend fun fetchNewspaperFeeds(
        region: NewsRegion,
        keywords: List<String>
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        if (keywords.isEmpty()) return@withContext emptyList()
        val feeds = NewsSources.newspaperFeeds.filter { it.region == region }
        coroutineScope {
            feeds.map { feed ->
                async {
                    fetchFeedCached(feed)
                        .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
                        .mapNotNull { article ->
                            val matched = KeywordMatcher.matchingKeyword(article.title, keywords) ?: return@mapNotNull null
                            article.copy(
                                source = "${NewsSources.NEWSPAPER_SOURCE_PREFIX} • ${feed.name}",
                                matchedKeyword = matched
                            )
                        }
                }
            }.awaitAll().flatten()
        }
    }

    /**
     * Downloads one built-in feed, at most [FEED_PARALLELISM] at a time across the app, and reuses the
     * result for [FEED_CACHE_MS] so the 5-minute auto refresh and the background worker don't re-download
     * 100+ feeds back to back.
     */
    private suspend fun fetchFeedCached(feed: NewspaperFeed): List<NewsArticle> {
        val now = System.currentTimeMillis()
        feedCache[feed.url]?.let { (fetchedAt, articles) ->
            if (now - fetchedAt < FEED_CACHE_MS) return articles
        }
        val fetched = feedSemaphore.withPermit {
            fetchRssFromUrl(feed.url, defaultSource = feed.name, requireValidTimestamp = true)
        }
        // Some sites write local time under a UTC label; see FeedClock
        val articles = FeedClock.fixLocalLabelledAsUtc(
            feedUrl = feed.url,
            knownBroken = feed.localTimeLabelledUtc,
            localOffsetMillis = FeedClock.localOffsetOf(feed.region),
            articles = fetched.articles,
            now = now
        )
        SourceHealth.record(SourceHealth.feedKey(feed.url), feed.name, SourceHealth.Kind.NEWSPAPER, fetched.error, articles, now)
        if (articles.isNotEmpty()) feedCache[feed.url] = now to articles
        return articles
    }

    private companion object {
        const val FEED_CACHE_MS = 4L * 60L * 1000L
        const val FEED_PARALLELISM = 16
        val feedCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<NewsArticle>>>()
        val feedSemaphore = kotlinx.coroutines.sync.Semaphore(FEED_PARALLELISM)
        const val MIN_VALID_FEED_TIME = 946_684_800_000L // 2000-01-01 UTC
        val TWO_DIGIT_YEAR = Regex("""(\d{1,2} [A-Za-z]{3,9}),? (\d{2}),? (\d{1,2}:\d{2})""")
    }

    /**
     * Expands Bengali keywords with their natural English search queries so Indian English & International
     * news engines return all relevant news even when the user's saved keyword is in Bengali script.
     */
    fun getEnglishSearchVariants(keyword: String): List<String> {
        val trimmed = keyword.trim()
        val variants = mutableSetOf<String>()
        val dictionary = mapOf(
            "বিজিবি" to "BGB Bangladesh",
            "বর্ডার গার্ড বাংলাদেশ" to "Border Guard Bangladesh",
            "বিএসএফ" to "BSF Bangladesh border",
            "সীমান্ত" to "Bangladesh border",
            "বাংলাদেশ ভারত সীমান্ত" to "India Bangladesh border",
            "সীমান্ত সংবাদ" to "Bangladesh border news",
            "রোহিঙ্গা" to "Rohingya Bangladesh border",
            "মিয়ানমার" to "Myanmar Bangladesh border",
            "মায়ানমার" to "Myanmar Bangladesh border",
            "আরাকান" to "Arakan Army Bangladesh border",
            "অনুপ্রবেশ" to "Bangladesh border infiltration",
            "চোরাচালান" to "Bangladesh border smuggling",
            "পুশইন" to "BSF BGB border push in",
            "টেকনাফ" to "Teknaf border",
            "তুমব্রু" to "Tumbru border",
            "সেন্টমার্টিন" to "St Martin Island Bangladesh",
            "তিস্তা" to "Teesta water treaty Bangladesh India"
        )
        dictionary[trimmed]?.let { variants.add(it) }
        if (variants.isEmpty()) {
            // Check partial matches for custom Bengali phrases
            for ((bn, en) in dictionary) {
                if (trimmed.contains(bn)) {
                    variants.add(en)
                }
            }
        }
        // Always include the original keyword if it contains Latin characters or if no mapping matched
        if (trimmed.any { it in 'A'..'Z' || it in 'a'..'z' } || variants.isEmpty()) {
            variants.add(trimmed)
        }
        return variants.toList()
    }

    /**
     * Expands any user keyword into Myanmar-specific search queries so Myanmar portals and global engines
     * return Myanmar & Rakhine/Arakan border coverage based on the user's keywords.
     */
    fun getMyanmarSearchVariants(keyword: String): List<String> {
        val trimmed = keyword.trim()
        val variants = mutableSetOf<String>()
        val myanmarDictionary = mapOf(
            "বিজিবি" to "Myanmar Bangladesh border BGB",
            "বর্ডার গার্ড বাংলাদেশ" to "Myanmar Border Guard Bangladesh",
            "বিএসএফ" to "Myanmar India Bangladesh border",
            "সীমান্ত" to "Myanmar Bangladesh border Rakhine",
            "বাংলাদেশ ভারত সীমান্ত" to "Myanmar Bangladesh border",
            "সীমান্ত সংবাদ" to "Myanmar Rakhine Arakan border news",
            "রোহিঙ্গা" to "Rohingya Myanmar Rakhine Maungdaw",
            "মিয়ানমার" to "Myanmar Arakan Army Rakhine border",
            "মায়ানমার" to "Myanmar Arakan Army Rakhine border",
            "আরাকান" to "Arakan Army Rakhine Myanmar border",
            "অনুপ্রবেশ" to "Myanmar Rakhine border infiltration Rohingya",
            "চোরাচালান" to "Myanmar border smuggling Rakhine Teknaf",
            "টেকনাফ" to "Teknaf Maungdaw Myanmar border",
            "তুমব্রু" to "Tumbru Myanmar border Rakhine"
        )
        myanmarDictionary[trimmed]?.let { variants.add(it) }
        if (variants.isEmpty()) {
            for ((bn, en) in myanmarDictionary) {
                if (trimmed.contains(bn)) {
                    variants.add(en)
                }
            }
        }
        if (variants.isEmpty()) {
            if (trimmed.any { it in 'A'..'Z' || it in 'a'..'z' }) {
                if (trimmed.contains("myanmar", ignoreCase = true) ||
                    trimmed.contains("arakan", ignoreCase = true) ||
                    trimmed.contains("rakhine", ignoreCase = true) ||
                    trimmed.contains("rohingya", ignoreCase = true)
                ) {
                    variants.add(trimmed)
                } else {
                    variants.add("$trimmed Myanmar")
                }
            } else {
                variants.add("$trimmed মিয়ানমার")
                variants.add("Myanmar Rakhine Bangladesh border")
            }
        }
        return variants.toList()
    }

    /**
     * Public Facebook posts indexed by Google News that match [query] (a Google query, normally
     * intitle: terms) within [whenWindow]. Tagged category = "facebook". Google names the publisher
     * of every such post just "Facebook"; the page's own name is filled in later, once the link has
     * been decoded (see [NewsRepository.fillMissingImages]).
     */
    suspend fun fetchFacebookPosts(query: String, whenWindow: String, tag: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode("site:facebook.com $query", "UTF-8")
        val cacheBuster = System.currentTimeMillis() / 30_000L
        val urlString = "https://news.google.com/rss/search?q=$encodedQuery+when:$whenWindow&hl=bn&gl=BD&ceid=BD:bn&cb=$cacheBuster"
        val fetched = fetchRssFromUrl(urlString, defaultSource = NewsFilters.FACEBOOK_SOURCE, requireValidTimestamp = true)
        SourceHealth.record(SourceHealth.FACEBOOK, "Facebook", SourceHealth.Kind.SEARCH, fetched.error, fetched.articles)
        fetched.articles
            .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
            .map { it.copy(source = NewsFilters.FACEBOOK_SOURCE, category = NewsFilters.CATEGORY_FACEBOOK, matchedKeyword = tag) }
    }

    /** Parsed items of one feed request, or why there are none. */
    private class FeedFetch(val articles: List<NewsArticle>, val error: String?)

    private fun fetchRssFromUrl(
        urlString: String,
        defaultSource: String = "Unknown",
        requireValidTimestamp: Boolean = true
    ): FeedFetch {
        return try {
            val request = Request.Builder()
                .url(urlString)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
                .header("Accept", "application/rss+xml, application/rdf+xml, application/atom+xml, application/xml, text/xml, */*")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return FeedFetch(emptyList(), "HTTP ${response.code}")
                val bodyStream = response.body?.byteStream() ?: return FeedFetch(emptyList(), "empty reply")
                val parsed = parseUniversalFeed(skipLeadingBlanks(bodyStream), defaultSource, requireValidTimestamp)
                val error = when {
                    parsed.itemsSeen == 0 -> "no items"
                    parsed.articles.isEmpty() -> "no dated items"
                    else -> null
                }
                FeedFetch(parsed.articles, error)
            }
        } catch (e: Exception) {
            FeedFetch(emptyList(), describe(e))
        }
    }

    /**
     * Some sites send blank lines or a byte-order mark before `<?xml`; Android's parser rejects
     * such a document outright, so they are skipped.
     */
    private fun skipLeadingBlanks(stream: InputStream): InputStream {
        val pushback = java.io.PushbackInputStream(stream, 1)
        while (true) {
            val byte = pushback.read()
            if (byte == -1) break
            // whitespace, or the three bytes of a UTF-8 byte-order mark
            if (byte == 0x20 || byte == 0x09 || byte == 0x0A || byte == 0x0D || byte == 0xEF || byte == 0xBB || byte == 0xBF) continue
            pushback.unread(byte)
            break
        }
        return pushback
    }

    private fun describe(e: Exception): String = when (e) {
        is java.net.SocketTimeoutException, is java.io.InterruptedIOException -> "timeout"
        is java.net.UnknownHostException -> "no connection"
        is javax.net.ssl.SSLException -> "SSL error"
        else -> e.javaClass.simpleName
    }

    private class ParsedFeed(val articles: List<NewsArticle>, val itemsSeen: Int)

    /**
     * Universal XML Feed Parser supporting RSS 2.0 (<rss>/<channel>/<item>),
     * Atom 1.0 (<feed>/<entry>), RDF (<rdf:RDF>/<item>), Bing News, Yahoo News, and Custom RSS Feeds.
     * Enforces strict timestamp verification so dateless/old articles are never given fake current timestamps.
     */
    private fun parseUniversalFeed(
        inputStream: InputStream,
        defaultSource: String,
        requireValidTimestamp: Boolean = true
    ): ParsedFeed {
        val articles = mutableListOf<NewsArticle>()
        var itemsSeen = 0
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var insideItem = false
            var insideAuthor = false
            var channelTitle = ""

            var title = ""
            var link = ""
            var guid = ""
            var pubDate = ""
            var source = ""
            var description = ""
            var imageUrl: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val rawName = parser.name ?: ""
                        val tagName = rawName.substringAfterLast(':').lowercase(Locale.ROOT)

                        if (tagName == "item" || tagName == "entry") {
                            insideItem = true
                            insideAuthor = false
                            title = ""
                            link = ""
                            guid = ""
                            pubDate = ""
                            source = ""
                            description = ""
                            imageUrl = null
                        } else if (!insideItem) {
                            if (tagName == "title" && channelTitle.isBlank()) {
                                channelTitle = safeNextText(parser).trim()
                            }
                        } else {
                            when {
                                tagName == "author" -> insideAuthor = true
                                insideAuthor && tagName == "name" -> {
                                    val authorName = safeNextText(parser).trim()
                                    if (source.isBlank() && authorName.isNotBlank()) {
                                        source = authorName
                                    }
                                }
                                tagName == "title" -> {
                                    // Some feeds (The Daily Star) wrap the headline in an <a> element
                                    val text = innerText(parser).trim()
                                    if (title.isBlank()) title = text
                                }
                                tagName == "link" -> {
                                    val hrefAttr = parser.getAttributeValue(null, "href")
                                    val relAttr = parser.getAttributeValue(null, "rel")
                                    if (!hrefAttr.isNullOrBlank()) {
                                        if (link.isBlank() || relAttr == "alternate" || relAttr == null) {
                                            link = hrefAttr.trim()
                                        }
                                    } else {
                                        val linkText = safeNextText(parser).trim()
                                        if (linkText.isNotBlank() && link.isBlank()) {
                                            link = linkText
                                        }
                                    }
                                }
                                tagName == "guid" || tagName == "id" -> {
                                    val idText = safeNextText(parser).trim()
                                    if (idText.startsWith("http://") || idText.startsWith("https://")) {
                                        guid = idText
                                    }
                                }
                                tagName == "pubdate" || tagName == "published" || tagName == "updated" || tagName == "date" -> {
                                    val dText = safeNextText(parser).trim()
                                    if (pubDate.isBlank() && dText.isNotBlank()) {
                                        pubDate = dText
                                    }
                                }
                                tagName == "source" || tagName == "creator" -> {
                                    val sText = safeNextText(parser).trim()
                                    if (sText.isNotBlank()) {
                                        source = sText
                                    }
                                }
                                tagName == "description" || tagName == "summary" || tagName == "content" || tagName == "encoded" -> {
                                    val descText = safeNextText(parser)
                                    if (description.isBlank()) {
                                        description = descText
                                    }
                                    if (imageUrl.isNullOrBlank() && descText.isNotBlank()) {
                                        imageUrl = extractImgFromHtml(descText)
                                    }
                                }
                                tagName == "thumbnail" || tagName == "content" || tagName == "enclosure" -> {
                                    val mediaUrl = parser.getAttributeValue(null, "url")
                                    if (!mediaUrl.isNullOrBlank() && imageUrl.isNullOrBlank()) {
                                        imageUrl = mediaUrl.trim()
                                    }
                                }
                                rawName.equals("News:Image", ignoreCase = true) || tagName == "image" -> {
                                    val urlAttr = parser.getAttributeValue(null, "url")
                                    if (!urlAttr.isNullOrBlank() && imageUrl.isNullOrBlank()) {
                                        imageUrl = urlAttr.trim()
                                    } else {
                                        val imgText = safeNextText(parser).trim()
                                        if (imgText.startsWith("http") && imageUrl.isNullOrBlank()) {
                                            imageUrl = imgText
                                        }
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val rawName = parser.name ?: ""
                        val tagName = rawName.substringAfterLast(':').lowercase(Locale.ROOT)
                        if (tagName == "author") {
                            insideAuthor = false
                        } else if (tagName == "item" || tagName == "entry") {
                            insideItem = false
                            itemsSeen++
                            val finalLink = link.ifBlank { guid }
                            val cleanTitle = decodeHtmlEntities(title)
                            if (finalLink.isNotBlank() && cleanTitle.isNotBlank()) {
                                val resolvedDirectLink = extractDirectUrlIfWrapped(finalLink)
                                val finalSource = decodeHtmlEntities(
                                    when {
                                        source.isNotBlank() -> source
                                        defaultSource.isNotBlank() && defaultSource != "Google News" && defaultSource != "Bing News" && defaultSource != "Yahoo News" -> defaultSource
                                        channelTitle.isNotBlank() && !channelTitle.contains("Google News", ignoreCase = true) && !channelTitle.contains("Bing News", ignoreCase = true) -> channelTitle
                                        else -> extractPublisherFromTitleOrUrl(cleanTitle, resolvedDirectLink ?: finalLink, defaultSource)
                                    }
                                )
                                val parsedTime = parseFeedTimestamp(pubDate)
                                if (parsedTime > 0L || !requireValidTimestamp) {
                                    articles.add(
                                        NewsArticle(
                                            link = finalLink,
                                            // the raw title was needed above to find the publisher
                                            title = HeadlineCleaner.clean(cleanTitle, finalSource),
                                            pubDate = pubDate,
                                            source = finalSource,
                                            // 0 = no readable date (only kept when dates are optional)
                                            timestamp = parsedTime,
                                            imageUrl = imageUrl,
                                            originalLink = resolvedDirectLink ?: finalLink
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
            // Return articles collected so far
        }
        return ParsedFeed(articles, itemsSeen)
    }

    /**
     * Detects old archive years (e.g., /2018/, /2021/, /2022/, /2023/, /2024/, /2025/) in URL slugs or titles
     * so re-crawled old news articles are strictly blocked even if a search engine assigns a fresh crawl date.
     */
    private fun hasOldArchiveYear(url: String, title: String = ""): Boolean =
        NewsFilters.hasOldArchiveYear(url, title)

    /**
     * All text inside the current element, including text of elements nested in it
     * (`<title><a href="…">Headline</a></title>`). Leaves the parser on the element's end tag.
     */
    private fun innerText(parser: XmlPullParser): String {
        val text = StringBuilder()
        var depth = 1
        try {
            while (depth > 0) {
                when (parser.next()) {
                    XmlPullParser.TEXT -> text.append(parser.text.orEmpty())
                    XmlPullParser.START_TAG -> depth++
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.END_DOCUMENT -> depth = 0
                }
            }
        } catch (_: Exception) {
            // keep what was read
        }
        return text.toString()
    }

    private fun safeNextText(parser: XmlPullParser): String {
        return try {
            parser.nextText() ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractImgFromHtml(html: String): String? {
        val imgRegex = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
        val match = imgRegex.find(html)
        val url = match?.groupValues?.getOrNull(1)?.replace("&amp;", "&")
        return if (!url.isNullOrBlank() && url.startsWith("http")) url else null
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#8217;", "’")
            .replace("&#8216;", "‘")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .trim()
    }

    private fun extractDirectUrlIfWrapped(rawUrl: String): String? {
        try {
            // Unwrap DuckDuckGo redirect: //duckduckgo.com/l/?uddg=https%3A%2F%2F...
            if (rawUrl.contains("uddg=", ignoreCase = true)) {
                val encodedTarget = rawUrl.substringAfter("uddg=").substringBefore("&")
                val decoded = URLDecoder.decode(encodedTarget, "UTF-8")
                if (decoded.startsWith("http")) return decoded
            }
            // Unwrap Bing News redirect: apiclick.aspx?...&url=https%3a%2f%2f...
            if (rawUrl.contains("bing.com/news/apiclick.aspx", ignoreCase = true) && rawUrl.contains("url=")) {
                val queryPart = rawUrl.substringAfter("url=")
                val encodedTarget = queryPart.substringBefore("&")
                val decoded = URLDecoder.decode(encodedTarget, "UTF-8")
                if (decoded.startsWith("http")) return decoded
            }
            // Unwrap Yahoo News redirect if wrapped with RU=https...
            if (rawUrl.contains("/RU=http", ignoreCase = true)) {
                val encodedTarget = rawUrl.substringAfter("/RU=").substringBefore("/RK=")
                val decoded = URLDecoder.decode(encodedTarget, "UTF-8")
                if (decoded.startsWith("http")) return decoded
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractPublisherFromTitleOrUrl(title: String, url: String, fallback: String): String {
        if (title.contains(" - ")) {
            val tail = title.substringAfterLast(" - ").trim()
            if (tail.length in 2..35) return tail
        }
        return try {
            val host = java.net.URI(url).host?.removePrefix("www.") ?: fallback
            host.ifBlank { fallback }
        } catch (_: Exception) {
            fallback
        }
    }

    internal fun parseFeedTimestamp(rawDate: String): Long {
        if (rawDate.isBlank()) return 0L
        // "Wed, 11 Nov 26 00:58:22 +0600": Android's date parser does not reliably accept a
        // two-digit year, so it is written out in full first
        val cleaned = TWO_DIGIT_YEAR.replace(rawDate.trim()) { "${it.groupValues[1]} 20${it.groupValues[2]} ${it.groupValues[3]}" }
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss z",
            "EEE, d MMM yyyy HH:mm:ss Z",
            // Some Bangladeshi CMSs put a comma after the year: "Sun, 08 Nov 2026, 07:14:21 +0600"
            "EEE, d MMM yyyy, HH:mm:ss Z",
            "EEE, d MMM yyyy, HH:mm:ss z",
            "EEE, d MMM yyyy HH:mm:ss",
            // Two-digit years ("Sun, 08 Nov 26 09:42:44 +0600") — tried only after the 4-digit forms fail
            "EEE, d MMM yy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm z",
            "EEE, d MMM yyyy HH:mm z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss",
            "dd MMM yyyy HH:mm:ss z",
            "dd MMM yyyy HH:mm:ss Z"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val parsed = sdf.parse(cleaned)
                // A 2-digit year read by a "yyyy" pattern lands in year 0026; reject it and keep trying
                if (parsed != null && parsed.time > MIN_VALID_FEED_TIME) {
                    return parsed.time
                }
            } catch (_: Exception) {}
        }
        return 0L
    }

}

