package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a news.google.com redirect link into the publisher's real article URL.
 * Results are cached for the app's lifetime, so copying or sharing the same article twice is instant.
 */
@Singleton
class GoogleNewsDecoder @Inject constructor(
    private val httpClient: OkHttpClient
) {
    private val resolved = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val decodeLock = kotlinx.coroutines.sync.Mutex()
    private val sigRegex = "data-n-a-sg=\"([^\"]+)\"".toRegex()
    private val tsRegex = "data-n-a-ts=\"([^\"]+)\"".toRegex()
    private val articleIdRegex = "articles/([A-Za-z0-9_\\-]+)".toRegex()
    private val DECODE_GAP_MILLIS = 300L
    private val RATE_LIMIT_BACKOFF_MILLIS = 20L * 60 * 1000
    private val GOOGLE_HOSTS = listOf(
        "google.com", "gstatic.com", "googleapis.com", "googleusercontent.com", "googletagmanager.com",
        "google-analytics.com", "doubleclick.net", "youtube.com", "ggpht.com"
    )
    private val urlRegex = "(https?://[^\"\\\\]+)".toRegex()

    /** Cached [decodeUrl]: returns the publisher URL, or [googleUrl] itself when it cannot be decoded. */
    suspend fun resolve(googleUrl: String): String {
        resolved[googleUrl]?.let { return it }
        // While Google is rate-limiting us, hand back its own link (it still opens the article)
        if (isRateLimited()) return googleUrl
        // Google answers bursts of decode requests with a bot check (reCAPTCHA) instead of the
        // link, so network decodes go one at a time with a short gap between them
        val decoded = decodeLock.withLock {
            resolved[googleUrl] ?: decodeUrl(googleUrl).also { delay(DECODE_GAP_MILLIS) }
        }
        if (decoded.isNotBlank()) resolved[googleUrl] = decoded
        return decoded.ifBlank { googleUrl }
    }

    fun clearCache() = resolved.clear()

    @Volatile private var blockedUntil = 0L

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

    private suspend fun decodeUrl(googleUrl: String): String = withContext(Dispatchers.IO) {
        try {
            if (!googleUrl.contains("news.google.com/rss/articles/") && !googleUrl.contains("news.google.com/articles/")) {
                return@withContext googleUrl
            }

            val match = articleIdRegex.find(googleUrl) ?: return@withContext googleUrl
            val articleId = match.groupValues[1]

            val getReq = Request.Builder()
                .url(googleUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()

            val html = httpClient.newCall(getReq).execute().use { resp ->
                if (resp.code == 429) backOff()
                if (!resp.isSuccessful) "" else resp.body?.string() ?: ""
            }
            if (html.isBlank()) return@withContext googleUrl

            val sigMatch = sigRegex.find(html)
            val tsMatch = tsRegex.find(html)

            if (sigMatch != null && tsMatch != null) {
                val sig = sigMatch.groupValues[1]
                val ts = tsMatch.groupValues[1]

                val innerPayload = "[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"$articleId\",$ts,\"$sig\"]"
                val payloadArray = JSONArray().apply {
                    put(JSONArray().apply {
                        put(JSONArray().apply {
                            put("Fbv4je")
                            put(innerPayload)
                        })
                    })
                }

                val postBodyStr = "f.req=${URLEncoder.encode(payloadArray.toString(), "UTF-8")}"
                val postReq = Request.Builder()
                    .url("https://news.google.com/_/DotsSplashUi/data/batchexecute")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(postBodyStr.toRequestBody("application/x-www-form-urlencoded;charset=utf-8".toMediaType()))
                    .build()

                val responseHtml = httpClient.newCall(postReq).execute().use { resp ->
                    if (resp.code == 429) backOff()
                    if (!resp.isSuccessful) "" else resp.body?.string() ?: ""
                }
                // A bot check instead of an answer means we are being throttled
                if (responseHtml.contains("recaptcha", ignoreCase = true) && !responseHtml.contains("garturlres")) backOff()

                val matches = urlRegex.findAll(responseHtml)
                for (m in matches) {
                    val finalUrl = m.groupValues[1].replace("\\/", "/")
                    // The reply also names Google's own resources (e.g. google.com/recaptcha/…);
                    // the article is the first address that isn't Google's
                    if (!isGoogleOwned(finalUrl)) {
                        return@withContext finalUrl
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to original URL
        }
        return@withContext googleUrl
    }
}
