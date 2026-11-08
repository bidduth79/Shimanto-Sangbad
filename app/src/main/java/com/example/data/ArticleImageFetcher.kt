package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds the picture a publisher chose for an article: the `og:image` / `twitter:image` that social
 * networks show when the link is shared. Google News feeds carry no pictures at all, so this is how
 * their cards get one.
 */
@Singleton
class ArticleImageFetcher @Inject constructor(
    private val httpClient: OkHttpClient,
    private val linkDecoder: GoogleNewsDecoder
) {

    /** Outcome of looking for an article's picture. */
    sealed interface Result {
        data class Found(val url: String) : Result
        /** The page was read and declares no share image: no point asking again. */
        data object NoImage : Result
        /** Network or decoding trouble: worth retrying on a later refresh. */
        data object Failed : Result
    }

    /** Whether finding this article's page needs Google's (rate-limited) link decoding. */
    fun needsGoogleDecode(article: NewsArticle): Boolean =
        (article.originalLink ?: article.link).contains("news.google.com")

    suspend fun findImage(article: NewsArticle): Result = withContext(Dispatchers.IO) {
        if (needsGoogleDecode(article) && linkDecoder.isRateLimited()) return@withContext Result.Failed
        val pageUrl = runCatching { linkDecoder.resolve(article.originalLink ?: article.link) }.getOrNull()
            ?.takeIf { it.startsWith("http") && !linkDecoder.isGoogleOwned(it) }
            ?: return@withContext Result.Failed
        val head = runCatching { readHead(pageUrl) }.getOrNull() ?: return@withContext Result.Failed
        extractShareImage(head, pageUrl)?.let { Result.Found(it) } ?: Result.NoImage
    }

    /** Reads only the start of the page: share-image tags live in <head>. */
    private fun readHead(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val source = response.body?.source() ?: return null
            source.request(MAX_HEAD_BYTES)
            val bytes = source.buffer.readByteArray(minOf(source.buffer.size, MAX_HEAD_BYTES))
            return String(bytes, Charsets.UTF_8)
        }
    }

    companion object {
        private const val MAX_HEAD_BYTES = 512L * 1024
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

        private val META_TAG = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
        private val ATTRIBUTE = Regex("([a-zA-Z:_-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)')")
        private val IMAGE_SRC_LINK = Regex("<link\\b[^>]*rel\\s*=\\s*[\"']image_src[\"'][^>]*>", RegexOption.IGNORE_CASE)
        private val PREFERRED_KEYS = listOf("og:image:secure_url", "og:image", "og:image:url", "twitter:image", "twitter:image:src")

        /**
         * The best share image declared in [html] (attribute order and quoting vary between sites),
         * made absolute against [pageUrl]. Pure, so it is unit-tested directly.
         */
        fun extractShareImage(html: String, pageUrl: String): String? {
            val found = mutableMapOf<String, String>()
            for (tag in META_TAG.findAll(html)) {
                val attrs = ATTRIBUTE.findAll(tag.value).associate { m ->
                    m.groupValues[1].lowercase() to (m.groups[3]?.value ?: m.groups[4]?.value.orEmpty())
                }
                val key = (attrs["property"] ?: attrs["name"])?.lowercase() ?: continue
                val content = attrs["content"]?.trim().orEmpty()
                if (key in PREFERRED_KEYS && content.isNotBlank() && key !in found) found[key] = content
            }
            val raw = PREFERRED_KEYS.firstNotNullOfOrNull { found[it] }
                ?: IMAGE_SRC_LINK.find(html)?.value?.let { link ->
                    ATTRIBUTE.findAll(link).firstOrNull { it.groupValues[1].equals("href", true) }
                        ?.let { it.groups[3]?.value ?: it.groups[4]?.value }
                }
                ?: return null
            val decoded = raw.replace("&amp;", "&")
            return runCatching { URI(pageUrl).resolve(decoded).toString() }.getOrNull()
                ?.takeIf { it.startsWith("http") && !isPlaceholder(it) }
        }

        /** Site logos and blank pixels that some papers declare when an article has no picture. */
        private fun isPlaceholder(url: String): Boolean {
            // Only the file name: folders such as /default/ often hold real photos
            val fileName = url.substringBefore('?').substringAfterLast('/').lowercase()
            return listOf("logo", "favicon", "placeholder", "default", "blank", "1x1").any { fileName.contains(it) }
        }
    }
}
