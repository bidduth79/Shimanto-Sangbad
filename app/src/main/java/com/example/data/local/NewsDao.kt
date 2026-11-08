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

    @Transaction
    /** Newest stories published after [since] (for breaking-news alerts and the widget). */
    @Query("SELECT * FROM news_articles WHERE timestamp >= :since ORDER BY timestamp DESC LIMIT 300")
    suspend fun getRecent(since: Long): List<NewsArticle>

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
        // Keep a picture found earlier (feeds such as Google News never include one)
        val knownImages = articles.map { it.link }.chunked(500)
            .flatMap { getImageUrls(it) }
            .associate { it.link to it.imageUrl }
        // Some feeds move a story's pubDate forward every time they edit it, which kept old stories
        // pinned to the top as "minutes ago". A story keeps the time it was first seen with.
        val knownTimes = articles.map { it.link }.chunked(500)
            .flatMap { getTimestamps(it) }
            .associate { it.link to it.timestamp }
        val now = System.currentTimeMillis()
        insertAll(articles.map { article ->
            article.copy(
                isSaved = article.isSaved || article.link in saved,
                imageUrl = article.imageUrl ?: knownImages[article.link],
                timestamp = stableTimestamp(knownTimes[article.link], article.timestamp, now)
            )
        })
    }

    @Query("SELECT link, timestamp FROM news_articles WHERE link IN (:links)")
    suspend fun getTimestamps(links: List<String>): List<LinkAndTime>

    @Query("SELECT link, imageUrl FROM news_articles WHERE link IN (:links) AND imageUrl IS NOT NULL")
    suspend fun getImageUrls(links: List<String>): List<LinkAndImage>

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

data class LinkAndImage(val link: String, val imageUrl: String?)

data class LinkAndTime(val link: String, val timestamp: Long)

/**
 * The time to store for a story: never later than now (a wrong time zone in a feed must not show
 * "in 7 minutes"), and never later than the time it was first stored with.
 */
fun stableTimestamp(stored: Long?, fetched: Long, now: Long): Long {
    val clamped = minOf(fetched, now)
    return if (stored != null && stored > 0) minOf(stored, clamped) else clamped
}
