package com.example.data

import com.example.data.local.NewsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for news. Network results are cleaned and written to Room; every screen
 * observes Room through the Flows below, so a refresh shows up everywhere as soon as each
 * country's fetch lands, instead of after all of them finish.
 */
@Singleton
class NewsRepository @Inject constructor(
    private val dao: NewsDao,
    private val planner: NewsSearchPlanner,
    private val remote: NewsRemoteDataSource,
    private val cloudBackup: FirebaseManager,
    private val imageFetcher: ArticleImageFetcher,
    private val linkDecoder: GoogleNewsDecoder,
    private val settingsRepository: com.example.data.settings.SettingsRepository
) {
    /** At most this many requests in flight across all regions, so refreshes don't starve the device. */
    private val requestPermits = Semaphore(MAX_PARALLEL_REQUESTS)

    /** News for the country tabs (fresh, non-social), newest first. */
    val news: Flow<List<NewsArticle>> = observeFresh(NewsFilters.NEWS_CATEGORIES) {
        !NewsFilters.isFacebookPost(it) && !NewsFilters.isSocialMediaPost(it)
    }

    val facebookNews: Flow<List<NewsArticle>> = observeFresh(listOf(NewsFilters.CATEGORY_FACEBOOK)) {
        !NewsFilters.isBlockedSource(it)
    }

    val customFeedNews: Flow<List<NewsArticle>> = observeFresh(listOf(NewsFilters.CATEGORY_RSS)) {
        !NewsFilters.isBlockedSource(it)
    }

    val savedNews: Flow<List<NewsArticle>> = dao.observeSaved().retryReads()

    /**
     * Refreshes every tab in two waves so the most important news arrives first and the device is
     * never parsing hundreds of responses at once: Bangladesh + social + the user's own feeds, then
     * India, Myanmar and international. Each part is written to Room the moment it completes.
     */
    suspend fun syncAll(
        keywords: List<String>,
        customFeeds: List<RssFeedItem>,
        onPriorityWaveDone: () -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        repairStoredData()
        SourceHealth.startRound()
        // New keywords: everything is searched in full again
        if (keywords != lastSyncedKeywords) {
            lastDeepAt.clear()
            lastBingAt.clear()
            lastSyncedKeywords = keywords
        }
        dao.deleteOldNews(System.currentTimeMillis() - NewsFilters.MAX_NEWS_AGE_MILLIS)
        supervisorScope {
            launch { syncRegion(NewsRegion.BANGLADESH, keywords) }
            launch { syncFacebook(keywords) }
            launch { syncCustomFeeds(customFeeds) }
        }
        onPriorityWaveDone()
        supervisorScope {
            listOf(NewsRegion.INDIA, NewsRegion.MYANMAR, NewsRegion.INTERNATIONAL)
                .forEach { region -> launch { syncRegion(region, keywords) } }
        }
    }

    /** When the slower-changing parts of each plan last ran (per process); see [NewsSearchPlanner.Depth]. */
    private val lastDeepAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val lastBingAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    @Volatile private var lastSyncedKeywords: List<String> = emptyList()

    private fun depthFor(key: String, now: Long) = NewsSearchPlanner.Depth(
        deep = now - (lastDeepAt[key] ?: 0L) >= DEEP_INTERVAL_MILLIS,
        bing = now - (lastBingAt[key] ?: 0L) >= BING_INTERVAL_MILLIS
    )

    /** The slower parts count as done only when the round brought something (not while offline). */
    private fun markDone(key: String, depth: NewsSearchPlanner.Depth, now: Long, gotResults: Boolean) {
        if (!gotResults) return
        if (depth.deep) lastDeepAt[key] = now
        if (depth.bing) lastBingAt[key] = now
    }

    suspend fun syncRegion(region: NewsRegion, keywords: List<String>) {
        val now = System.currentTimeMillis()
        val depth = depthFor(region.name, now)
        val fetched = runAll(planner.plan(region, keywords, depth))
        markDone(region.name, depth, now, fetched.isNotEmpty())
        val cleaned = NewsFilters.cleanRegionResults(
            articles = fetched,
            category = region.category,
            keywords = keywords
        )
        store(cleaned)
    }

    suspend fun syncFacebook(keywords: List<String>) {
        val now = System.currentTimeMillis()
        val depth = depthFor(FACEBOOK_KEY, now)
        val raw = runAll(planner.planFacebook(keywords, depth))
        markDone(FACEBOOK_KEY, depth, now, raw.isNotEmpty())
        val fetched = raw
            .filter { NewsFilters.isFresh(it) && !NewsFilters.isSports(it) && !NewsFilters.isBlockedSource(it) }
        val cleaned = NewsFilters.onlyAboutKeywords(fetched, keywords)
            .distinctBy { it.link }
            .distinctBy { it.title.trim().lowercase() }
            .map { it.copy(category = NewsFilters.CATEGORY_FACEBOOK) }
        store(cleaned)
    }

    private val repairLock = kotlinx.coroutines.sync.Mutex()

    /**
     * One-time repairs of rows stored by earlier versions; runs before the first sync so repaired
     * and freshly fetched rows never mix. v1: publish times of Bing rows (Pacific time labelled
     * GMT) and of the feeds that label local time as UTC.
     */
    suspend fun repairStoredData() = repairLock.withLock {
        if (settingsRepository.dataFixVersion() >= DATA_FIX_VERSION) return@withLock
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val pacificBehindUtc = -java.util.TimeZone.getTimeZone("America/Los_Angeles").getOffset(now).toLong()
            dao.shiftBingTimesForward(pacificBehindUtc, now)
            val mislabelled = NewsSources.newspaperFeeds.filter { it.localTimeLabelledUtc }
                .map { "${NewsSources.NEWSPAPER_SOURCE_PREFIX} • ${it.name}" }
            if (mislabelled.isNotEmpty()) dao.shiftTimesBack(mislabelled, FeedClock.BANGLADESH_OFFSET_MILLIS)
        }
        settingsRepository.setDataFixVersion(DATA_FIX_VERSION)
    }

    /**
     * Decodes Google links of [articles] (ten per request) and stores the publisher addresses, so
     * copy/share are instant and Facebook posts show which page they came from.
     */
    suspend fun resolveLinks(articles: List<NewsArticle>): Map<String, String> = withContext(Dispatchers.IO) {
        val google = articles.filter { imageFetcher.needsGoogleDecode(it) }
        if (google.isEmpty()) return@withContext emptyMap()
        val decoded = linkDecoder.resolveAll(google.map { it.originalLink ?: it.link })
        dao.setDecoded(google.mapNotNull { article ->
            val target = decoded[article.originalLink ?: article.link]?.takeIf { !linkDecoder.isGoogleOwned(it) }
                ?: return@mapNotNull null
            val pageSource = if (article.category == NewsFilters.CATEGORY_FACEBOOK) FacebookPages.sourceFor(target) else null
            com.example.data.local.DecodedLink(article.link, target, pageSource)
        })
        decoded
    }

    suspend fun syncCustomFeeds(feeds: List<RssFeedItem>) {
        if (feeds.isEmpty()) return
        val cleaned = runAll(planner.planCustomFeeds(feeds))
            .filter { NewsFilters.isFresh(it) && !NewsFilters.isBlockedSource(it) }
            .distinctBy { it.link }
            .map { it.copy(category = NewsFilters.CATEGORY_RSS) }
        store(cleaned)
    }

    /**
     * Gives rows saved before DB v6 their country tab, in batches, in the background. Until then the
     * screen shows them under the tab they were fetched for.
     */
    suspend fun backfillRegions(storedClassifierVersion: Int) = withContext(Dispatchers.Default) {
        // Regions stored by an older version of the rules are recomputed from scratch
        if (storedClassifierVersion != NewsRegionClassifier.VERSION) dao.clearRegions()
        while (true) {
            val batch = dao.getWithoutRegion(limit = 200)
            if (batch.isEmpty()) break
            dao.setRegions(batch.associate { it.link to NewsRegionClassifier.regionOf(it).name })
        }
    }

    /**
     * Deletes stored articles whose headline is not about any of [keywords] (bookmarks are kept).
     * They can never be shown, and rows saved under older, looser rules made every screen update slower.
     */
    suspend fun pruneOffTopic(keywords: List<String>) = withContext(Dispatchers.Default) {
        if (keywords.isEmpty()) return@withContext
        val offTopic = dao.getUnsavedHeadlines(NewsFilters.NEWS_CATEGORIES + NewsFilters.CATEGORY_FACEBOOK)
            .filterNot { KeywordMatcher.matches(it.title, keywords) }
            .map { it.link }
        // SQLite caps the number of bound parameters per statement
        offTopic.chunked(500).forEach { dao.deleteUnsaved(it) }
    }

    /**
     * Finds pictures for the newest articles that have none (Google News items never do), Bangladesh
     * first, a few at a time. Results are written in small batches so the screen fills in steadily
     * without recomputing after every single picture.
     */
    suspend fun fillMissingImages() = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - NewsFilters.MAX_NEWS_AGE_MILLIS
        val missing = (dao.getWithoutImage(listOf(NewsFilters.CATEGORY_BANGLADESH), since, IMAGE_BATCH_LIMIT) +
            dao.getWithoutImage(NewsFilters.NEWS_CATEGORIES + NewsFilters.CATEGORY_FACEBOOK, since, IMAGE_BATCH_LIMIT))
            .distinctBy { it.link }

        // 1) Free: the same story often arrives via Bing or a paper's own feed with a picture
        val borrowed = SameStoryImages.borrow(missing, dao.getRecentWithImage(since))
        if (borrowed.isNotEmpty()) dao.setImageUrls(borrowed)

        // 2) Network: direct publisher pages freely; Google links only a few per run, because
        //    their decoding is rate-limited and the same decoding powers "copy link" and sharing
        val (google, direct) = missing.filter { it.link !in borrowed }.partition { imageFetcher.needsGoogleDecode(it) }
        val (facebook, googleNews) = google.partition { it.category == NewsFilters.CATEGORY_FACEBOOK }
        val toDecode = googleNews.take(MAX_GOOGLE_NEWS_DECODES) + facebook.take(MAX_FACEBOOK_DECODES)
        val decoded = resolveLinks(toDecode)
        // Only links that were decoded can be looked up now; the rest are tried on a later refresh
        val queue = direct + toDecode.filter { (it.originalLink ?: it.link) in decoded }
        val permits = Semaphore(IMAGE_PARALLELISM)
        val started = System.currentTimeMillis()
        android.util.Log.d(IMAGE_TAG, "looking for pictures: ${queue.size} articles")
        queue.distinctBy { it.link }.chunked(IMAGE_WRITE_BATCH).forEach { chunk ->
            val found = coroutineScope {
                chunk.map { article ->
                    async { permits.withPermit { article.link to imageFetcher.findImage(article) } }
                }.awaitAll()
            }
            // Found: store it. No picture on the page: store "" so it isn't asked again.
            // Failed (network/decoding): leave it unset, so the next refresh tries again.
            dao.setImageUrls(found.mapNotNull { (link, result) ->
                when (result) {
                    is ArticleImageFetcher.Result.Found -> link to result.url
                    ArticleImageFetcher.Result.NoImage -> link to ""
                    ArticleImageFetcher.Result.Failed -> null
                }
            }.toMap())
            android.util.Log.d(IMAGE_TAG, "batch: ${found.count { it.second is ArticleImageFetcher.Result.Found }}/${found.size} found, ${System.currentTimeMillis() - started} ms so far")
        }
    }

    /** Writes [articles] with their country tab resolved once, here, off the UI path. */
    private suspend fun store(articles: List<NewsArticle>) {
        if (articles.isEmpty()) return
        dao.upsertKeepingBookmarks(articles.map { it.copy(region = NewsRegionClassifier.regionOf(it).name) })
    }

    /** Flips the bookmark and mirrors it to the cloud backup (when configured). */
    suspend fun toggleBookmark(article: NewsArticle) = withContext(Dispatchers.IO) {
        val nowSaved = !(dao.getByLink(article.link)?.isSaved ?: article.isSaved)
        dao.insertAll(listOf(article.copy(isSaved = nowSaved)))
        runCatching { cloudBackup.syncSavedNews(article, nowSaved) }
    }

    /** Pulls bookmarks saved on this device before a reinstall back from the cloud backup. */
    suspend fun restoreBookmarksFromCloud() = withContext(Dispatchers.IO) {
        val restored = runCatching { cloudBackup.restoreSavedNews() }.getOrDefault(emptyList())
        if (restored.isNotEmpty()) dao.insertAll(restored)
    }

    /** Drops every cached article except bookmarks. */
    suspend fun clearCache() = withContext(Dispatchers.IO) { dao.deleteAllUnsaved() }

    /** Runs [tasks] concurrently under the shared permit limit; a failing task just contributes nothing. */
    private suspend fun runAll(tasks: List<SearchTask>): List<NewsArticle> = coroutineScope {
        tasks.map { task ->
            async {
                requestPermits.withPermit {
                    try {
                        task()
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
            }
        }.awaitAll().flatten()
    }

    /**
     * Rows from [categories] newer than the freshness window, re-checked in memory (the window keeps
     * moving while the app is open). Only cheap checks run here; the expensive ones (archive years,
     * relevance, country) already ran when the rows were written. Bursts of writes during a refresh are conflated so the UI
     * recomputes once per burst instead of once per write.
     */
    private fun observeFresh(categories: List<String>, keep: (NewsArticle) -> Boolean): Flow<List<NewsArticle>> =
        dao.observeByCategories(categories, since = System.currentTimeMillis() - NewsFilters.MAX_NEWS_AGE_MILLIS)
            .retryReads()
            .conflate()
            .map { list ->
                val cutoff = System.currentTimeMillis() - NewsFilters.MAX_NEWS_AGE_MILLIS
                list.filter { it.timestamp >= cutoff && keep(it) }
            }
            .flowOn(Dispatchers.Default)

    /** A failed database read is retried (briefly backing off) instead of crashing the app. */
    private fun <T> Flow<T>.retryReads(): Flow<T> = retryWhen { cause, attempt ->
        android.util.Log.w("NewsRepository", "Database read failed (attempt ${attempt + 1})", cause)
        delay(300L * (attempt + 1).coerceAtMost(10))
        cause !is kotlinx.coroutines.CancellationException
    }

    private val NewsRegion.category: String
        get() = when (this) {
            NewsRegion.BANGLADESH -> NewsFilters.CATEGORY_BANGLADESH
            NewsRegion.INDIA -> NewsFilters.CATEGORY_INDIA
            NewsRegion.MYANMAR -> NewsFilters.CATEGORY_MYANMAR
            NewsRegion.INTERNATIONAL -> NewsFilters.CATEGORY_INTERNATIONAL
        }

    private companion object {
        const val MAX_PARALLEL_REQUESTS = 8
        const val IMAGE_TAG = "ArticleImages"
        const val IMAGE_PARALLELISM = 4
        const val IMAGE_WRITE_BATCH = 12
        const val IMAGE_BATCH_LIMIT = 250
        /** Google links decoded per run (ten per request): news first, then Facebook posts. */
        const val MAX_GOOGLE_NEWS_DECODES = 30
        const val MAX_FACEBOOK_DECODES = 20
        const val FACEBOOK_KEY = "facebook"
        /** Whole-day searches change slowly; the newest-headlines searches run on every refresh. */
        const val DEEP_INTERVAL_MILLIS = 30L * 60 * 1000
        const val BING_INTERVAL_MILLIS = 9L * 60 * 1000
        const val DATA_FIX_VERSION = 1
    }
}
