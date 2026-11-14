package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The newspaper's own address for a story. What gets shared or opened must never be a search
 * engine's or aggregator's link:
 *  - Google News links are decoded ([GoogleNewsDecoder]),
 *  - MSN copies (which Bing often points to) are traded for the original article MSN credits,
 *    through MSN's own content service (checked on 2026-11-14: an NDTV copy gave NDTV's address),
 *  - tracking parameters (utm_…, fbclid…) are removed.
 */
@Singleton
class PublisherLinks @Inject constructor(
    private val httpClient: OkHttpClient,
    private val googleDecoder: GoogleNewsDecoder
) {
    private val msnOriginals = ConcurrentHashMap<String, String>()

    /** The newspaper address behind [url], or null when it cannot be found right now. */
    suspend fun resolve(url: String): String? {
        val decoded = when {
            isGoogleNews(url) -> googleDecoder.resolve(url).takeIf { !googleDecoder.isGoogleOwned(it) }
            isMsnCopy(url) -> msnOriginal(url)
            else -> url
        } ?: return null
        val clean = withoutTracking(decoded)
        return clean.takeIf { isPublisherLink(it) }
    }

    /** Decodes many Google links in batches (ten per request); MSN copies one by one. */
    suspend fun resolveAll(urls: List<String>): Map<String, String> {
        val out = HashMap<String, String>()
        val google = urls.filter(::isGoogleNews)
        googleDecoder.resolveAll(google).forEach { (from, to) ->
            if (!googleDecoder.isGoogleOwned(to)) out[from] = withoutTracking(to)
        }
        for (url in urls.filter(::isMsnCopy)) msnOriginal(url)?.let { out[url] = withoutTracking(it) }
        for (url in urls) if (url !in out && isPublisherLink(url)) out[url] = withoutTracking(url)
        return out
    }

    private suspend fun msnOriginal(url: String): String? = withContext(Dispatchers.IO) {
        msnOriginals[url]?.let { return@withContext it }
        val id = MSN_ID.find(url)?.groupValues?.get(1) ?: return@withContext null
        try {
            val request = Request.Builder()
                .url("https://assets.msn.com/content/view/v2/Detail/en-us/$id")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/122 Mobile Safari/537.36")
                .get()
                .build()
            val body = httpClient.newCall(request).execute().use { if (it.isSuccessful) it.body?.string() else null }
                ?: return@withContext null
            JSONObject(body).optString("sourceHref").takeIf { it.startsWith("http") && !isMsnCopy(it) }
                ?.also { msnOriginals[url] = it }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val MSN_ID = Regex("/ar-([A-Za-z0-9]+)")
        private val TRACKING = Regex("^(utm_[a-z]+|fbclid|gclid|mc_cid|mc_eid|ocid|cvid|ei|ref|ref_src|igshid|_ga)$", RegexOption.IGNORE_CASE)

        fun isGoogleNews(url: String) = url.contains("news.google.com/")

        fun isMsnCopy(url: String) = hostOf(url).let { it == "msn.com" || it.endsWith(".msn.com") }

        /**
         * True for an address that belongs to a newspaper (or a Facebook page) rather than to a
         * search engine, aggregator or feed service.
         */
        fun isPublisherLink(url: String): Boolean {
            if (!url.startsWith("http")) return false
            val host = hostOf(url)
            if (host.isEmpty()) return false
            return NOT_PUBLISHERS.none { host == it || host.endsWith(".$it") }
        }

        private val NOT_PUBLISHERS = listOf(
            "google.com", "googleusercontent.com", "bing.com", "msn.com", "yahoo.com", "feedburner.com",
            "feedproxy.google.com", "duckduckgo.com", "news.yahoo.co.jp", "flipboard.com", "newsbreak.com"
        )

        private fun hostOf(url: String): String =
            runCatching { URI(url).host }.getOrNull()?.lowercase()?.removePrefix("www.").orEmpty()

        /** Drops utm_… and similar tracking parameters; everything else of the address stays. */
        fun withoutTracking(url: String): String {
            val q = url.indexOf('?')
            if (q < 0) return url
            val hash = url.indexOf('#', q).let { if (it < 0) url.length else it }
            val kept = url.substring(q + 1, hash).split('&')
                .filter { it.isNotBlank() && !TRACKING.matches(it.substringBefore('=')) }
            val base = url.substring(0, q)
            val fragment = url.substring(hash)
            return if (kept.isEmpty()) base + fragment else "$base?${kept.joinToString("&")}$fragment"
        }

        /** What to send when no newspaper address is known: the headline and the paper's name. */
        fun fallbackText(article: NewsArticle): String {
            val paper = NewsSourceKinds.publisherOf(article)
                .takeIf { it.isNotBlank() && !it.equals("Google News", true) && !it.equals("Bing News", true) }
            return if (paper == null) article.title else "${article.title}\n$paper"
        }
    }
}
