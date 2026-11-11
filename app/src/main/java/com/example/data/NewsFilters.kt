package com.example.data

import java.util.Calendar

/**
 * Pure rules deciding which fetched articles are worth showing. No Android or I/O, so every rule
 * is unit-tested directly.
 */
object NewsFilters {

    /**
     * Articles older than this are not stored. What is SHOWN is narrower: only stories published
     * today ([startOfToday]). Yesterday's stories stay stored but hidden on purpose: some feeds move
     * an old story's date forward when they edit it, and only a stored copy (which keeps the
     * first-seen time) stops such a story from coming back as "today's".
     */
    const val MAX_NEWS_AGE_MILLIS = 36L * 60 * 60 * 1000L

    /** How long hidden rows are kept as that memory before being deleted. */
    const val KEEP_STORED_MILLIS = 4L * 24 * 60 * 60 * 1000L

    /** Local midnight at the start of the day [now] falls in: nothing published before it is shown. */
    fun startOfToday(now: Long = System.currentTimeMillis(), zone: java.util.TimeZone = java.util.TimeZone.getDefault()): Long {
        val calendar = Calendar.getInstance(zone)
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private val URL_DATE_REGEX = Regex("(20[0-9]{2})[/-](0?[1-9]|1[0-2])[/-](0?[1-9]|[12][0-9]|3[01])(?![0-9])")
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000L

    /**
     * True when the address itself carries a date ("/2026/11/05/") from before yesterday: an old
     * page that a search engine or feed is presenting with a fresh date. Yesterday is tolerated
     * because sites that date their addresses in UTC are a day behind in the first hours of a day;
     * such a story is still judged by its publish time.
     */
    fun hasOldDateInUrl(url: String, now: Long = System.currentTimeMillis()): Boolean {
        val yesterday = startOfToday(now) - DAY_MILLIS
        return URL_DATE_REGEX.findAll(url).any { match ->
            val (year, month, day) = match.destructured
            val calendar = Calendar.getInstance()
            calendar.clear()
            calendar.set(year.toInt(), month.toInt() - 1, day.toInt())
            calendar.timeInMillis < yesterday
        }
    }

    /** Feeds sometimes stamp items a few minutes in the future; allow that much clock skew. */
    private const val FUTURE_TOLERANCE_MILLIS = 10L * 60 * 1000L

    private val SPORTS_KEYWORDS = listOf(
        "খেলা", "ক্রিকেট", "ফুটবল", "স্পোর্টস", "টি-টোয়েন্টি", "আইপিএল", "বিপিএল",
        "sports", "cricket", "football", "খেলোয়াড়", "টুর্নামেন্ট", "বিশ্বকাপ",
        "মেসি", "রোনালদো", "সাকিব", "তামিম", "বিসিবি", "বাফুফে", "হকি", "টেনিস", "অলিম্পিক"
    )

    /**
     * Social platforms whose posts leak into search results as "news" (e.g. "Google • Instagram").
     * Facebook is collected separately for the social feed; everything here is dropped.
     */
    private val SOCIAL_PLATFORM_NAMES = listOf(
        "instagram", "tiktok", "twitter", "x", "x (formerly twitter)", "pinterest", "linkedin", "threads", "snapchat",
        "youtube", "likee", "kwai", "quora", "telegram"
    )
    private val SOCIAL_PLATFORM_DOMAINS = listOf(
        "instagram.com", "tiktok.com", "twitter.com", "://x.com", ".x.com/", "pinterest.",
        "linkedin.com", "threads.net", "snapchat.com", "youtube.com", "youtu.be", "likee.video",
        "kwai.com", "quora.com", "t.me/"
    )

    private val FACEBOOK_MARKERS = listOf("facebook", "fb/social", "সোশ্যাল ফিড")
    private val FACEBOOK_DOMAINS = listOf("facebook.com", "fb.com", "fb.watch")

    fun isFresh(article: NewsArticle, now: Long = System.currentTimeMillis()): Boolean {
        if (article.timestamp <= 0L) return false
        val age = now - article.timestamp
        if (age < -FUTURE_TOLERANCE_MILLIS || age > MAX_NEWS_AGE_MILLIS) return false
        val url = article.originalLink ?: article.link
        return !hasOldArchiveYear(url, article.title) && !hasOldDateInUrl(url, now)
    }

    fun isSports(article: NewsArticle): Boolean = SPORTS_KEYWORDS.any {
        article.title.contains(it, ignoreCase = true) || article.source.contains(it, ignoreCase = true)
    }

    /** Content farms that repeatedly republished stale or off-topic items. */
    fun isBlockedSource(article: NewsArticle): Boolean {
        val source = article.source.lowercase()
        val link = article.link.lowercase()
        return source.contains("vietnam") || link.contains("vietnam") || article.title.lowercase().contains("vietnam.vn")
    }

    fun isFacebookPost(article: NewsArticle): Boolean {
        val source = article.source.lowercase()
        val link = (article.originalLink ?: article.link).lowercase()
        return article.category == CATEGORY_FACEBOOK ||
            FACEBOOK_MARKERS.any { source.contains(it) } ||
            FACEBOOK_DOMAINS.any { link.contains(it) }
    }

    /** Instagram/TikTok/X/YouTube etc. posts that a search engine returned as if they were news. */
    fun isSocialMediaPost(article: NewsArticle): Boolean {
        val publisher = article.source.substringAfter("•").trim().lowercase()
        val link = (article.originalLink ?: article.link).lowercase()
        return SOCIAL_PLATFORM_NAMES.any { publisher == it } ||
            SOCIAL_PLATFORM_DOMAINS.any { link.contains(it) }
    }

    @Volatile private var cachedYear = 0
    @Volatile private var cachedYearAt = 0L

    /** Calendar.getInstance() is slow on Android; the year is re-read at most once an hour. */
    private fun currentYear(): Int {
        val now = System.currentTimeMillis()
        if (now - cachedYearAt > 60 * 60 * 1000L) {
            cachedYear = Calendar.getInstance().get(Calendar.YEAR)
            cachedYearAt = now
        }
        return cachedYear
    }

    private val URL_YEAR_REGEX = "(?:/|-|_)(20[0-2][0-9])(?:/|-|_)".toRegex()
    private val TITLE_YEAR_REGEX = Regex("""\b(20[0-2][0-9])\b""")

    /** True when the URL or headline carries a past year (an archive page resurfacing as "new"). */
    fun hasOldArchiveYear(url: String, title: String = ""): Boolean {
        val currentYear = currentYear()
        if (URL_YEAR_REGEX.findAll(url).any { (it.groupValues[1].toIntOrNull() ?: 0) in 2005 until currentYear }) return true
        return TITLE_YEAR_REGEX.findAll(title).any { (it.groupValues[1].toIntOrNull() ?: 0) in 2005 until (currentYear - 1) }
    }

    /**
     * Identity of a story for de-duplication: the headline without the " - Publisher" tail Google
     * News adds, ignoring case, punctuation and spacing. The same story from a paper's own feed and
     * from a search engine then counts once.
     */
    fun headlineKey(title: String): String {
        val headline = if (title.contains(" - ")) title.substringBeforeLast(" - ") else title
        return headline.lowercase().filter { it.isLetterOrDigit() }
    }

    fun isBengali(text: String): Boolean = text.any { it in 'ঀ'..'৿' }

    /**
     * Shared clean-up for a region's results: fresh, headline actually about one of [keywords]
     * (tagged with that keyword), de-duplicated, newest first.
     */
    fun cleanRegionResults(
        articles: List<NewsArticle>,
        category: String,
        keywords: List<String>,
        now: Long = System.currentTimeMillis()
    ): List<NewsArticle> = articles
        .filter { isFresh(it, now) && !isSports(it) && !isBlockedSource(it) && !isFacebookPost(it) && !isSocialMediaPost(it) }
        .mapNotNull { article ->
            KeywordMatcher.matchingKeyword(article.title, keywords)?.let { article.copy(matchedKeyword = it, category = category) }
        }
        .distinctBy { it.link }
        .distinctBy { "${searchMediumOf(it)}|${headlineKey(it.title)}" }
        .sortedByDescending { it.timestamp }

    /** Keeps only [articles] whose headline is about one of [keywords], re-tagged with the keyword found. */
    fun onlyAboutKeywords(articles: List<NewsArticle>, keywords: List<String>): List<NewsArticle> =
        articles.mapNotNull { article ->
            KeywordMatcher.matchingKeyword(article.title, keywords)?.let { found ->
                if (found == article.matchedKeyword) article else article.copy(matchedKeyword = found)
            }
        }

    const val CATEGORY_BANGLADESH = "bgb"
    const val CATEGORY_NATIONAL = "national" // written by older versions; still read
    const val CATEGORY_INDIA = "indian"
    const val CATEGORY_MYANMAR = "myanmar"
    const val CATEGORY_INTERNATIONAL = "international"
    const val CATEGORY_FACEBOOK = "facebook"

    /**
     * Source of a Facebook post until its page is known; "Facebook • <page>" once it is.
     * Older versions stored the generic form as "Facebook • Facebook".
     */
    const val FACEBOOK_SOURCE = "Facebook"
    const val FACEBOOK_SOURCE_PREFIX = "Facebook • "
    const val LEGACY_FACEBOOK_SOURCE = "Facebook • Facebook"

    /** True while a Facebook post's source does not name its page yet. */
    fun isGenericFacebookSource(source: String): Boolean =
        source == FACEBOOK_SOURCE || source == LEGACY_FACEBOOK_SOURCE
    const val CATEGORY_RSS = "rss"

    val NEWS_CATEGORIES = listOf(
        CATEGORY_BANGLADESH, CATEGORY_NATIONAL, CATEGORY_INDIA, CATEGORY_MYANMAR, CATEGORY_INTERNATIONAL
    )
}

/**
 * Which search medium (or direct newspaper feed) produced an article, from its source prefix or link.
 * Used for the filter chips and for de-duplicating the same headline within one medium.
 */
fun searchMediumOf(article: NewsArticle): String {
    val src = article.source.trim().lowercase()
    val lnk = article.link.lowercase()
    return when {
        src.startsWith(NewsSources.NEWSPAPER_SOURCE_PREFIX) -> SearchMedium.NEWSPAPER
        article.category == NewsFilters.CATEGORY_FACEBOOK || src.startsWith("facebook") || src.startsWith("fb/") ||
            src.contains("সোশ্যাল ফিড") || lnk.contains("facebook.com") || lnk.contains("fb.com") -> SearchMedium.FACEBOOK
        src.startsWith("bing") || src.contains("bing news") || lnk.contains("bing.com") -> SearchMedium.BING
        src.startsWith("yahoo") || src.contains("yahoo news") || lnk.contains("yahoo.com") -> SearchMedium.YAHOO
        src.startsWith("duckduckgo") || lnk.contains("duckduckgo.com") -> SearchMedium.DUCKDUCKGO
        src.startsWith("reddit") || lnk.contains("reddit.com") -> SearchMedium.REDDIT
        else -> SearchMedium.GOOGLE
    }
}

object SearchMedium {
    const val NEWSPAPER = "পত্রিকা"
    const val GOOGLE = "Google"
    const val BING = "Bing"
    const val YAHOO = "Yahoo"
    const val DUCKDUCKGO = "DuckDuckGo"
    const val REDDIT = "Reddit"
    const val FACEBOOK = "ফেসবুক"
}
