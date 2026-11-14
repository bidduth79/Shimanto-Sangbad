package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.NewsArticle
import kotlinx.coroutines.flow.Flow

/** More than any screen can usefully show; keeps each read well bounded. */
const val MAX_ROWS = 3000

@Dao
interface NewsDao {

    /**
     * Live list of the newest [limit] articles in [categories] published after [since].
     * Re-emits whenever the table changes.
     *
     * @Transaction: a result bigger than one CursorWindow (~2 MB) is read in several chunks; without a
     * transaction, a write between chunks shifts the rows and the read fails ("Couldn't read row …").
     */
    @Transaction
    @Query("SELECT * FROM news_articles WHERE category IN (:categories) AND timestamp >= :since ORDER BY timestamp DESC LIMIT :limit")
    fun observeByCategories(categories: List<String>, since: Long, limit: Int = MAX_ROWS): Flow<List<NewsArticle>>

    /** Newest stories published after [since] (for breaking-news alerts and the widget). */
    @Query("SELECT * FROM news_articles WHERE timestamp >= :since ORDER BY timestamp DESC LIMIT 300")
    suspend fun getRecent(since: Long): List<NewsArticle>

    @Transaction
    @Query("SELECT * FROM news_articles WHERE isSaved = 1 ORDER BY timestamp DESC")
    fun observeSaved(): Flow<List<NewsArticle>>

    @Query("SELECT link FROM news_articles WHERE isSaved = 1")
    suspend fun getSavedLinks(): List<String>

    @Query("SELECT * FROM news_articles WHERE link = :link LIMIT 1")
    suspend fun getByLink(link: String): NewsArticle?

    /** Rows stored before DB v6, which have no country tab yet. */
    @Query("SELECT * FROM news_articles WHERE region = '' LIMIT :limit")
    suspend fun getWithoutRegion(limit: Int): List<NewsArticle>

    @Query("UPDATE news_articles SET region = ''")
    suspend fun clearRegions()

    @Query("UPDATE news_articles SET region = :region WHERE link = :link")
    suspend fun setRegion(link: String, region: String)

    @Transaction
    suspend fun setRegions(regions: Map<String, String>) {
        regions.forEach { (link, region) -> setRegion(link, region) }
    }

    /** Link and headline of every non-bookmarked row in [categories] (no other columns, to stay light). */
    @Query("SELECT link, title FROM news_articles WHERE isSaved = 0 AND category IN (:categories)")
    suspend fun getUnsavedHeadlines(categories: List<String>): List<LinkAndTitle>

    @Query("DELETE FROM news_articles WHERE isSaved = 0 AND link IN (:links)")
    suspend fun deleteUnsaved(links: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<NewsArticle>)

    @Query("UPDATE news_articles SET isSaved = :isSaved WHERE link = :link")
    suspend fun updateSavedStatus(link: String, isSaved: Boolean)

    @Query("DELETE FROM news_articles WHERE timestamp < :olderThanMillis AND isSaved = 0")
    suspend fun deleteOldNews(olderThanMillis: Long)

    @Query("DELETE FROM news_articles WHERE isSaved = 0")
    suspend fun deleteAllUnsaved()

    /**
     * Inserts fresh articles without un-bookmarking ones the user saved: a re-fetched article
     * replaces the old row but keeps its `isSaved` flag.
     */
    @Transaction
    suspend fun upsertKeepingBookmarks(articles: List<NewsArticle>) {
        if (articles.isEmpty()) return
        val saved = getSavedLinks().toHashSet()
        // What was learned about a story after it was first stored must survive a re-fetch:
        //  - its picture (feeds such as Google News never include one),
        //  - its first-seen time (some feeds move pubDate forward on every edit, which kept old
        //    stories pinned to the top as "minutes ago"),
        //  - its decoded publisher link and Facebook page name (a re-fetch brings Google's again).
        val stored = articles.map { it.link }.chunked(500)
            .flatMap { getStoredParts(it) }
            .associateBy { it.link }
        val now = System.currentTimeMillis()
        insertAll(articles.map { article ->
            val old = stored[article.link]
            article.copy(
                isSaved = article.isSaved || article.link in saved,
                imageUrl = article.imageUrl ?: old?.imageUrl,
                timestamp = stableTimestamp(old?.timestamp, article.timestamp, now),
                originalLink = keptOriginalLink(old?.originalLink, article.originalLink),
                source = keptSource(old?.source, article.source)
            )
        })
    }

    @Query("SELECT link, imageUrl, timestamp, originalLink, source FROM news_articles WHERE link IN (:links)")
    suspend fun getStoredParts(links: List<String>): List<StoredParts>

    @Query("UPDATE news_articles SET originalLink = :originalLink WHERE link = :link")
    suspend fun setOriginalLink(link: String, originalLink: String)

    @Query("SELECT link, title, source FROM news_articles")
    suspend fun getAllTitles(): List<LinkTitleSource>

    @Query("UPDATE news_articles SET title = :title WHERE link = :link")
    suspend fun setTitle(link: String, title: String)

    @Transaction
    suspend fun setTitles(titles: Map<String, String>) {
        titles.forEach { (link, title) -> setTitle(link, title) }
    }

    @Query("UPDATE news_articles SET source = :source WHERE link = :link")
    suspend fun setSource(link: String, source: String)

    /** Stores decoded publisher links (and, for Facebook posts, the page name as source). */
    @Transaction
    suspend fun setDecoded(decoded: List<DecodedLink>) {
        decoded.forEach {
            setOriginalLink(it.link, it.originalLink)
            if (it.source != null) setSource(it.link, it.source)
        }
    }

    /** One-time repair: Bing rows stored before its time-zone mistake was known (see FeedClock). */
    @Query("UPDATE news_articles SET timestamp = timestamp + :shiftMillis WHERE source LIKE 'Bing • %' AND timestamp + :shiftMillis <= :now")
    suspend fun shiftBingTimesForward(shiftMillis: Long, now: Long)

    /** One-time repair: rows of feeds that label local time as UTC. */
    @Query("UPDATE news_articles SET timestamp = timestamp - :shiftMillis WHERE source IN (:sources)")
    suspend fun shiftTimesBack(sources: List<String>, shiftMillis: Long)

    /**
     * Newest articles in [categories] whose picture hasn't been looked for yet. `imageUrl` is null
     * until tried; an empty string means the page had none, so it isn't tried again.
     */
    @Query("SELECT * FROM news_articles WHERE imageUrl IS NULL AND category IN (:categories) AND timestamp >= :since ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getWithoutImage(categories: List<String>, since: Long, limit: Int): List<NewsArticle>

    /** Recent articles that already have a picture, to lend it to the same story from other sources. */
    @Query("SELECT * FROM news_articles WHERE imageUrl IS NOT NULL AND imageUrl != '' AND timestamp >= :since")
    suspend fun getRecentWithImage(since: Long): List<NewsArticle>

    @Query("UPDATE news_articles SET imageUrl = :imageUrl WHERE link = :link")
    suspend fun setImageUrl(link: String, imageUrl: String)

    @Transaction
    suspend fun setImageUrls(images: Map<String, String>) {
        images.forEach { (link, url) -> setImageUrl(link, url) }
    }
}

data class LinkAndTitle(val link: String, val title: String)

data class StoredParts(val link: String, val imageUrl: String?, val timestamp: Long, val originalLink: String?, val source: String)

data class LinkTitleSource(val link: String, val title: String, val source: String)

data class DecodedLink(val link: String, val originalLink: String, val source: String? = null)

/** A publisher address decoded earlier is not replaced by the Google address a re-fetch brings. */
fun keptOriginalLink(stored: String?, fetched: String?): String? {
    val storedIsDecoded = stored != null && !stored.contains("news.google.com")
    val fetchedIsGoogle = fetched == null || fetched.contains("news.google.com")
    return if (storedIsDecoded && fetchedIsGoogle) stored else fetched
}

/** A Facebook page name found earlier is not replaced by the plain "Facebook" of a re-fetch. */
fun keptSource(stored: String?, fetched: String): String {
    val storedNamesPage = stored != null && stored.startsWith(com.example.data.NewsFilters.FACEBOOK_SOURCE_PREFIX) &&
        !com.example.data.NewsFilters.isGenericFacebookSource(stored)
    return if (storedNamesPage && com.example.data.NewsFilters.isGenericFacebookSource(fetched)) stored!! else fetched
}

/**
 * The time to store for a story: never later than now (a wrong time zone in a feed must not show
 * "in 7 minutes"), and never later than the time it was first stored with.
 */
fun stableTimestamp(stored: Long?, fetched: Long, now: Long): Long {
    val clamped = minOf(fetched, now)
    return if (stored != null && stored > 0) minOf(stored, clamped) else clamped
}
