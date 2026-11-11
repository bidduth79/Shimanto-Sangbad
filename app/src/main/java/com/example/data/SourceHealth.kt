package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What every news source last did, as seen from this phone. Fetch errors used to be swallowed
 * silently, so a source could stop delivering for hours without anyone noticing; the settings
 * screen now shows this record ("উৎসের অবস্থা").
 *
 * Kept in memory for the life of the process: the first refresh after a start fills it again.
 */
object SourceHealth {

    enum class Kind { SEARCH, NEWSPAPER, CUSTOM }

    enum class Level { OK, STALE, DOWN, UNKNOWN }

    data class Status(
        val key: String,
        val label: String,
        val kind: Kind,
        val lastAttemptAt: Long = 0L,
        /** Last time a response with at least one item arrived. */
        val lastSuccessAt: Long = 0L,
        /** Why the last attempt failed ("HTTP 403", "timeout", "no items"); null when it worked. */
        val lastError: String? = null,
        val consecutiveFailures: Int = 0,
        /** Requests in the current refresh round (a search engine is asked many questions per round). */
        val requests: Int = 0,
        val failedRequests: Int = 0,
        /** Publish time of the newest item this source has delivered. */
        val newestItemAt: Long = 0L
    )

    const val GOOGLE = "google"
    const val BING = "bing"
    const val FACEBOOK = "facebook"

    /** A search engine whose newest item is older than this is delivering stale results. */
    const val SEARCH_STALE_MILLIS = 4L * 60 * 60 * 1000
    /** A newspaper feed with nothing newer than this has probably been abandoned. */
    const val FEED_STALE_MILLIS = 5L * 24 * 60 * 60 * 1000

    private val _statuses = MutableStateFlow<Map<String, Status>>(emptyMap())
    val statuses: StateFlow<Map<String, Status>> = _statuses.asStateFlow()

    @Volatile private var roundStartedAt = 0L
    private val _lastRoundAt = MutableStateFlow(0L)
    /** When the latest full refresh began (0 = none yet in this run of the app). */
    val lastRoundAt: StateFlow<Long> = _lastRoundAt.asStateFlow()

    /** A refresh is starting: per-round request counters begin again from zero. */
    fun startRound(now: Long = System.currentTimeMillis()) {
        roundStartedAt = now
        _lastRoundAt.value = now
    }

    /**
     * Notes the outcome of one request to a source. [error] is null when the request worked;
     * a working request that carries no items counts as a failure for feeds ("no items").
     */
    fun record(
        key: String,
        label: String,
        kind: Kind,
        error: String?,
        items: List<NewsArticle>,
        now: Long = System.currentTimeMillis()
    ) {
        val failure = error ?: if (items.isEmpty() && kind != Kind.SEARCH) "no items" else null
        val newest = items.maxOfOrNull { it.timestamp }?.coerceAtMost(now) ?: 0L
        _statuses.update { all ->
            val old = all[key] ?: Status(key, label, kind)
            val newRound = old.lastAttemptAt < roundStartedAt
            all + (key to old.copy(
                label = label,
                lastAttemptAt = now,
                lastSuccessAt = if (failure == null && items.isNotEmpty()) now else old.lastSuccessAt,
                lastError = failure,
                consecutiveFailures = if (failure == null) 0 else old.consecutiveFailures + 1,
                requests = (if (newRound) 0 else old.requests) + 1,
                failedRequests = (if (newRound) 0 else old.failedRequests) + if (failure != null) 1 else 0,
                newestItemAt = maxOf(old.newestItemAt, newest)
            ))
        }
    }

    /** A custom feed the user removed should not linger in the list. */
    fun forget(key: String) = _statuses.update { it - key }

    fun levelOf(status: Status, now: Long = System.currentTimeMillis()): Level = when {
        status.lastAttemptAt == 0L -> Level.UNKNOWN
        status.kind == Kind.SEARCH -> when {
            status.requests > 0 && status.failedRequests == status.requests -> Level.DOWN
            status.lastSuccessAt == 0L -> Level.DOWN
            now - status.newestItemAt > SEARCH_STALE_MILLIS -> Level.STALE
            else -> Level.OK
        }
        // One failed attempt can be a hiccup; two in a row (or never having worked) is a problem
        status.consecutiveFailures >= 2 || (status.lastSuccessAt == 0L && status.consecutiveFailures >= 1) -> Level.DOWN
        status.newestItemAt > 0L && now - status.newestItemAt > FEED_STALE_MILLIS -> Level.STALE
        else -> Level.OK
    }

    fun customKey(feedUrl: String) = "custom:$feedUrl"
    fun feedKey(feedUrl: String) = "feed:$feedUrl"

    /** For tests. */
    internal fun resetForTest() {
        _statuses.value = emptyMap()
        roundStartedAt = 0L
        _lastRoundAt.value = 0L
    }
}
