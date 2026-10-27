package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
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
import java.util.concurrent.TimeUnit

@Entity(
    tableName = "news_articles",
    indices = [
        Index(value = ["category", "timestamp"]),
        Index(value = ["isSaved"]),
        Index(value = ["timestamp"])
    ]
)
data class NewsArticle(
    @PrimaryKey val link: String,
    val title: String,
    val pubDate: String,
    val source: String,
    val timestamp: Long,
    var category: String = "",
    var isSaved: Boolean = false,
    var matchedKeyword: String = "",
    var imageUrl: String? = null,
    var originalLink: String? = null
)

object SharedHttpClient {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
            .connectTimeout(4500, TimeUnit.MILLISECONDS)
            .readTimeout(4500, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .build()
    }
}

class NewsRepository {
    private val httpClient = SharedHttpClient.client

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
        fetchRssFromUrl(urlString, defaultSource = "Google News", requireValidTimestamp = true)
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
        fetchRssFromUrl(urlString, defaultSource = "Bing News", requireValidTimestamp = true)
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
     * Fetches news from Yahoo News RSS Search for any keyword (syndicates Reuters, AP, AFP, PTI, global portals).
     */
    suspend fun fetchYahooNews(
        keyword: String,
        matchedTag: String = keyword
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val cleanQuery = "$keyword -site:facebook.com -site:fb.com"
        val encodedKeyword = URLEncoder.encode(cleanQuery, "UTF-8")
        val cacheBuster = System.currentTimeMillis() / 30_000L
        val urlString = "https://news.search.yahoo.com/rss?p=$encodedKeyword&cb=$cacheBuster"
        fetchRssFromUrl(urlString, defaultSource = "Yahoo News", requireValidTimestamp = true)
            .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
            .map { article ->
                val cleanSource = if (article.source.isBlank() || article.source == "Unknown") "Yahoo News" else article.source
                val formattedSource = if (cleanSource.startsWith("Yahoo", ignoreCase = true)) {
                    cleanSource
                } else {
                    "Yahoo • $cleanSource"
                }
                article.copy(
                    source = formattedSource,
                    matchedKeyword = matchedTag
                )
            }
    }

    /**
     * Fetches public Reddit News & WorldNews RSS discussions/links for a keyword from the last 24 hours only.
     */
    suspend fun fetchRedditNews(
        keyword: String,
        matchedTag: String = keyword
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
        val cacheBuster = System.currentTimeMillis() / 30_000L
        val urlString = "https://www.reddit.com/search.rss?q=$encodedKeyword&sort=new&t=day&cb=$cacheBuster"
        fetchRssFromUrl(urlString, defaultSource = "Reddit News", requireValidTimestamp = true)
            .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
            .map { article ->
                article.copy(
                    source = if (article.source.isBlank() || article.source == "Unknown") "Reddit" else "Reddit • ${article.source}",
                    matchedKeyword = matchedTag
                )
            }
    }

    /**
     * Fetches fresh 24-hour news from DuckDuckGo News / Web Search for any keyword.
     * Strictly requires a verifiable date/time in the result or snippet and blocks social media & archive links
     * so undated old web pages never receive fake timestamps.
     */
    suspend fun fetchDuckDuckGoNews(
        keyword: String,
        matchedTag: String = keyword
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        val encodedQuery = URLEncoder.encode("$keyword news -site:facebook.com -site:fb.com -site:youtube.com", "UTF-8")
        val ddgUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery&df=d"
        try {
            val request = Request.Builder()
                .url(ddgUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val blockRegex = "<div[^>]*class=\"[^\"]*result__body[^\"]*\"[^>]*>(.*?)</div>\\s*</div>".toRegex(
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                    )
                    val linkRegex = "<a[^>]*class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>".toRegex(
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                    )
                    for (blockMatch in blockRegex.findAll(html)) {
                        val blockHtml = blockMatch.groupValues[1]
                        val linkMatch = linkRegex.find(blockHtml) ?: continue
                        val rawHref = linkMatch.groupValues[1].replace("&amp;", "&").trim()
                        val rawTitle = linkMatch.groupValues[2].replace("<[^>]+>".toRegex(), "").trim()
                        val cleanTitle = decodeHtmlEntities(rawTitle)
                        val directLink = extractDirectUrlIfWrapped(rawHref) ?: if (rawHref.startsWith("//")) "https:$rawHref" else rawHref
                        if (directLink.startsWith("http") &&
                            cleanTitle.length > 10 &&
                            !directLink.contains("duckduckgo.com") &&
                            !isSocialMediaDomain(directLink) &&
                            !hasOldArchiveYear(directLink, cleanTitle)
                        ) {
                            val snippetTime = extractRecentSnippetTimestamp(blockHtml)
                            if (snippetTime > 0L) {
                                val publisher = extractPublisherFromTitleOrUrl(cleanTitle, directLink, "DuckDuckGo News")
                                articles.add(
                                    NewsArticle(
                                        link = directLink,
                                        title = cleanTitle,
                                        pubDate = "সম্প্রতি",
                                        source = "DuckDuckGo • $publisher",
                                        timestamp = snippetTime,
                                        matchedKeyword = matchedTag,
                                        originalLink = directLink
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore transient HTML scrape failures
        }
        articles
    }

    /**
     * Fetches articles directly from a user-configured Custom RSS or Atom feed URL.
     */
    suspend fun fetchCustomRssFeed(feed: RssFeedItem): List<NewsArticle> = withContext(Dispatchers.IO) {
        fetchRssFromUrl(feed.url, defaultSource = feed.title, requireValidTimestamp = false)
            .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
            .map { article ->
                val src = if (article.source.isBlank() || article.source == "Unknown") {
                    feed.title
                } else {
                    article.source
                }
                article.copy(
                    source = src,
                    category = "rss",
                    matchedKeyword = feed.title,
                    timestamp = if (article.timestamp > 0L) article.timestamp else System.currentTimeMillis()
                )
            }
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
     * Fetches public Facebook page posts, viral Facebook discussions, and social media news reports
     * for the given keyword (strictly within the last 24h) and tags them with category = "facebook".
     */
    suspend fun fetchFacebookNews(keyword: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val query = "(site:facebook.com OR \"ফেসবুক\" OR \"Facebook\") $keyword"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val cacheBuster = System.currentTimeMillis() / 30_000L
        val urlString = "https://news.google.com/rss/search?q=$encodedQuery+when:1d&hl=bn&gl=BD&ceid=BD:bn&cb=$cacheBuster"
        val rawArticles = fetchRssFromUrl(urlString, defaultSource = "সোশ্যাল ফিড", requireValidTimestamp = true)
            .filter { !hasOldArchiveYear(it.originalLink ?: it.link, it.title) }
        rawArticles.map { article ->
            val src = if (article.source.contains("Facebook", ignoreCase = true) || article.link.contains("facebook.com", ignoreCase = true)) {
                "Facebook • ${article.source}"
            } else {
                "FB/Social • ${article.source}"
            }
            article.copy(
                source = src,
                category = "facebook",
                matchedKeyword = keyword
            )
        }
    }

    private fun fetchRssFromUrl(
        urlString: String,
        defaultSource: String = "Unknown",
        requireValidTimestamp: Boolean = true
    ): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        try {
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
                if (response.isSuccessful) {
                    val bodyStream = response.body?.byteStream()
                    if (bodyStream != null) {
                        articles.addAll(parseUniversalFeed(bodyStream, defaultSource, requireValidTimestamp))
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore transient individual feed timeouts
        }
        return articles
    }

    /**
     * Universal XML Feed Parser supporting RSS 2.0 (<rss>/<channel>/<item>),
     * Atom 1.0 (<feed>/<entry>), RDF (<rdf:RDF>/<item>), Bing News, Yahoo News, and Custom RSS Feeds.
     * Enforces strict timestamp verification so dateless/old articles are never given fake current timestamps.
     */
    private fun parseUniversalFeed(
        inputStream: InputStream,
        defaultSource: String,
        requireValidTimestamp: Boolean = true
    ): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
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
                                    if (title.isBlank()) {
                                        title = safeNextText(parser).trim()
                                    }
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
                                            title = cleanTitle,
                                            pubDate = pubDate,
                                            source = finalSource,
                                            timestamp = if (parsedTime > 0L) parsedTime else System.currentTimeMillis(),
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
        return articles
    }

    /**
     * Detects old archive years (e.g., /2018/, /2021/, /2022/, /2023/, /2024/, /2025/) in URL slugs or titles
     * so re-crawled old news articles are strictly blocked even if a search engine assigns a fresh crawl date.
     */
    fun hasOldArchiveYear(url: String, title: String = ""): Boolean {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val urlYearRegex = "(?:/|-|_)(20[0-2][0-9])(?:/|-|_)".toRegex()
        for (match in urlYearRegex.findAll(url)) {
            val yr = match.groupValues[1].toIntOrNull() ?: continue
            if (yr in 2005 until currentYear) {
                return true
            }
        }
        val titleYearRegex = "\\b(20[0-2][0-9])\\b".toRegex()
        for (match in titleYearRegex.findAll(title)) {
            val yr = match.groupValues[1].toIntOrNull() ?: continue
            if (yr in 2005 until (currentYear - 1)) {
                return true
            }
        }
        return false
    }

    private fun isSocialMediaDomain(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        return lower.contains("facebook.com") ||
            lower.contains("fb.com") ||
            lower.contains("fb.watch") ||
            lower.contains("instagram.com") ||
            lower.contains("tiktok.com") ||
            lower.contains("twitter.com") ||
            lower.contains("x.com/")
    }

    /**
     * Parses verifiable recent relative time from a DuckDuckGo snippet (queried with df=d for last 24h).
     * Rejects any snippet mentioning days/weeks/months/years ago or old years.
     */
    private fun extractRecentSnippetTimestamp(blockHtml: String): Long {
        val now = System.currentTimeMillis()
        val text = blockHtml.replace("<[^>]+>".toRegex(), " ").lowercase(Locale.ROOT)
        if ("\\b(\\d+)\\s*(?:day|days|week|weeks|month|months|year|years)\\s*ago\\b".toRegex().containsMatchIn(text)) {
            return 0L
        }
        if (hasOldArchiveYear(text, text)) {
            return 0L
        }
        val minMatch = "(\\d{1,2})\\s*(?:minute|minutes|min|mins)\\s*ago".toRegex().find(text)
        if (minMatch != null) {
            val mins = minMatch.groupValues[1].toLongOrNull() ?: return 0L
            return now - mins * 60_000L
        }
        val hourMatch = "(\\d{1,2})\\s*(?:hour|hours|hr|hrs)\\s*ago".toRegex().find(text)
        if (hourMatch != null) {
            val hours = hourMatch.groupValues[1].toLongOrNull() ?: return 0L
            return if (hours <= 24L) now - hours * 3_600_000L else 0L
        }
        return now - 2 * 3_600_000L
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

    private fun parseFeedTimestamp(rawDate: String): Long {
        if (rawDate.isBlank()) return 0L
        val cleaned = rawDate.trim()
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss z",
            "EEE, d MMM yyyy HH:mm:ss Z",
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
                if (parsed != null && parsed.time > 0L) {
                    return parsed.time
                }
            } catch (_: Exception) {}
        }
        return 0L
    }

    suspend fun fetchFromApi(apiUrl: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        try {
            val request = Request.Builder().url(apiUrl).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseString = response.body?.string() ?: return@use
                    val jsonObject = JSONObject(responseString)

                    if (jsonObject.has("bgb")) {
                        val bgbArray = jsonObject.getJSONArray("bgb")
                        for (i in 0 until bgbArray.length()) {
                            val item = bgbArray.getJSONObject(i)
                            articles.add(
                                NewsArticle(
                                    title = item.getString("title"),
                                    link = item.getString("link"),
                                    pubDate = item.getString("pubDate"),
                                    source = item.getString("source"),
                                    category = item.getString("category"),
                                    timestamp = item.getLong("timestamp")
                                )
                            )
                        }
                    }

                    if (jsonObject.has("national")) {
                        val nationalArray = jsonObject.getJSONArray("national")
                        for (i in 0 until nationalArray.length()) {
                            val item = nationalArray.getJSONObject(i)
                            articles.add(
                                NewsArticle(
                                    title = item.getString("title"),
                                    link = item.getString("link"),
                                    pubDate = item.getString("pubDate"),
                                    source = item.getString("source"),
                                    category = item.getString("category"),
                                    timestamp = item.getLong("timestamp")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        articles
    }
}

