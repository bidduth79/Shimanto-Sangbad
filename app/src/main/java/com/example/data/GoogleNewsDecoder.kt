package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns news.google.com redirect links into the publishers' real article URLs.
 *
 * Decoding takes two steps: the article page carries a signature, and Google's "batchexecute"
 * endpoint trades it for the address. That endpoint is rate-limited (HTTP 429 after bursts), so up
 * to [BATCH_SIZE] links share ONE batchexecute request (checked on 2026-11-10: ten links, one
 * request, ten answers). Results are cached for the app's lifetime.
 */
@Singleton
class GoogleNewsDecoder @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val resolved = ConcurrentHashMap<String, String>()
    private val decodeLock = Mutex()
    private val pagePermits = Semaphore(PAGE_PARALLELISM)
    @Volatile private var blockedUntil = 0L

    /** Cached: the publisher URL, or [googleUrl] itself when it cannot be decoded. */
    suspend fun resolve(googleUrl: String): String = resolveAll(listOf(googleUrl))[googleUrl] ?: googleUrl

    /**
     * Publisher URLs for [googleUrls]; links that could not be decoded (or while Google is
     * throttling) are simply missing from the result. Links that are not Google's map to themselves.
     */
    suspend fun resolveAll(googleUrls: List<String>): Map<String, String> {
        val result = HashMap<String, String>()
        val pending = ArrayList<String>()
        for (url in googleUrls.distinct()) {
            val known = resolved[url]
            when {
                known != null -> result[url] = known
                !isGoogleNewsArticle(url) -> result[url] = url
                else -> pending += url
            }
        }
        for (batch in pending.chunked(BATCH_SIZE)) {
            if (isRateLimited()) break
            // The lock is taken per batch, so a tap on "copy link" never waits behind a long queue
            decodeLock.withLock {
                val fresh = batch.filter { url -> resolved[url]?.also { result[url] = it } == null }
                if (fresh.isNotEmpty() && !isRateLimited()) {
                    decodeBatch(fresh).forEach { (url, target) ->
                        resolved[url] = target
                        result[url] = target
                    }
                    delay(BATCH_GAP_MILLIS)
                }
            }
        }
        return result
    }

    fun clearCache() = resolved.clear()

    /** True while Google is throttling decode requests; background work should wait. */
    fun isRateLimited(): Boolean = System.currentTimeMillis() < blockedUntil

    private fun backOff() {
        blockedUntil = System.currentTimeMillis() + RATE_LIMIT_BACKOFF_MILLIS
    }

    /** Google, Google static/content and YouTube domains: never the publisher's article. */
    internal fun isGoogleOwned(url: String): Boolean {
        val host = runCatching { java.net.URI(url).host }.getOrNull()?.lowercase() ?: return true
        return GOOGLE_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    private fun isGoogleNewsArticle(url: String): Boolean =
        url.contains("news.google.com/rss/articles/") || url.contains("news.google.com/articles/")

    private class Signed(val url: String, val articleId: String, val timestamp: String, val signature: String)

    private suspend fun decodeBatch(urls: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            val signed = coroutineScope {
                urls.map { url -> async { pagePermits.withPermit { signatureOf(url) } } }.awaitAll().filterNotNull()
            }
            if (signed.isEmpty()) return@withContext emptyMap()

            val calls = JSONArray()
            signed.forEachIndexed { index, s ->
                val inner = "[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1]," +
                    "\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"${s.articleId}\",${s.timestamp},\"${s.signature}\"]"
                calls.put(JSONArray().put("Fbv4je").put(inner).put(JSONObject.NULL).put((index + 1).toString()))
            }
            val body = "f.req=${URLEncoder.encode(JSONArray().put(calls).toString(), "UTF-8")}"
            val request = Request.Builder()
                .url("https://news.google.com/_/DotsSplashUi/data/batchexecute")
                .header("User-Agent", USER_AGENT)
                .post(body.toRequestBody("application/x-www-form-urlencoded;charset=utf-8".toMediaType()))
                .build()
            val reply = httpClient.newCall(request).execute().use { resp ->
                if (resp.code == 429) backOff()
                if (!resp.isSuccessful) "" else resp.body?.string().orEmpty()
            }
            // A bot check instead of an answer means we are being throttled
            if (reply.contains("recaptcha", ignoreCase = true) && !reply.contains("garturlres")) backOff()

            parseBatchReply(reply).mapNotNull { (index, target) ->
                val source = signed.getOrNull(index - 1) ?: return@mapNotNull null
                if (isGoogleOwned(target)) null else source.url to target
            }.toMap()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** The article page carries the signature and timestamp its decode request needs. */
    private fun signatureOf(googleUrl: String): Signed? = try {
        val articleId = ARTICLE_ID.find(googleUrl)?.groupValues?.get(1)
        val html = httpClient.newCall(
            Request.Builder().url(googleUrl).header("User-Agent", USER_AGENT).get().build()
        ).execute().use { resp ->
            if (resp.code == 429) backOff()
            if (!resp.isSuccessful) "" else resp.body?.string().orEmpty()
        }
        val signature = SIGNATURE.find(html)?.groupValues?.get(1)
        val timestamp = TIMESTAMP.find(html)?.groupValues?.get(1)
        if (articleId != null && signature != null && timestamp != null) Signed(googleUrl, articleId, timestamp, signature) else null
    } catch (_: Exception) {
        null
    }

    companion object {
        /** Links decoded by one batchexecute request. */
        const val BATCH_SIZE = 10
        private const val PAGE_PARALLELISM = 3
        private const val BATCH_GAP_MILLIS = 400L
        private const val RATE_LIMIT_BACKOFF_MILLIS = 20L * 60 * 1000
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        private val SIGNATURE = "data-n-a-sg=\"([^\"]+)\"".toRegex()
        private val TIMESTAMP = "data-n-a-ts=\"([^\"]+)\"".toRegex()
        private val ARTICLE_ID = "articles/([A-Za-z0-9_\\-]+)".toRegex()
        private val GOOGLE_HOSTS = listOf(
            "google.com", "gstatic.com", "googleapis.com", "googleusercontent.com", "googletagmanager.com",
            "google-analytics.com", "doubleclick.net", "youtube.com", "ggpht.com"
        )

        /**
         * Answers of one batchexecute reply, by the 1-based number each request was sent with.
         * The reply is `)]}'` followed by a JSON array of
         * `["wrb.fr","Fbv4je","[\"garturlres\",\"<url>\",1]",null,null,null,"<number>"]` entries, in any order.
         */
        internal fun parseBatchReply(reply: String): Map<Int, String> {
            val out = HashMap<Int, String>()
            val start = reply.indexOf('[')
            if (start < 0) return out
            val entries = runCatching { JSONArray(reply.substring(start)) }.getOrNull() ?: return out
            for (i in 0 until entries.length()) {
                val entry = entries.optJSONArray(i) ?: continue
                if (entry.optString(0) != "wrb.fr" || entry.isNull(2)) continue
                val number = entry.optString(entry.length() - 1).toIntOrNull() ?: continue
                val inner = runCatching { JSONArray(entry.optString(2)) }.getOrNull() ?: continue
                val url = inner.optString(1)
                if (inner.optString(0) == "garturlres" && url.startsWith("http")) out[number] = url
            }
            return out
        }
    }
}
