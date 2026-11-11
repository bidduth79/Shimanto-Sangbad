package com.example.data

/**
 * The page a Facebook post belongs to, read from the post's address. Google News names the
 * publisher of every Facebook post just "Facebook"; the decoded link says which page posted it:
 * `facebook.com/BorderGuardBangladeshOfficial/posts/…` → "BorderGuardBangladeshOfficial".
 */
object FacebookPages {

    private val ADDRESS = Regex(
        "^https?://(?:[a-z0-9-]+\\.)?(?:facebook|fb)\\.com/([^/?#]+)(?:/([^/?#]+))?",
        RegexOption.IGNORE_CASE
    )

    /** First path segments that are Facebook features, not page names. */
    private val NOT_PAGES = setOf(
        "story.php", "permalink.php", "photo.php", "photo", "photos", "video.php", "videos", "watch",
        "reel", "reels", "share", "groups", "events", "profile.php", "hashtag", "login", "sharer",
        "sharer.php", "posts", "p", "public", "marketplace", "gaming", "stories", "notes", "media",
        "plugins", "dialog", "l.php", "help", "policies", "search", "pg"
    )

    fun pageOf(url: String): String? {
        val match = ADDRESS.find(url.trim()) ?: return null
        val first = match.groupValues[1]
        val raw = when (first.lowercase()) {
            // facebook.com/people/Some-Name/1000123…, facebook.com/pages/Some-Name/123…
            "people", "pages" -> match.groupValues[2].ifBlank { return null }
            in NOT_PAGES -> return null
            else -> first
        }
        val name = runCatching { java.net.URLDecoder.decode(raw, "UTF-8") }.getOrDefault(raw).replace('-', ' ').trim()
        return name.takeIf { it.isNotBlank() && !it.all(Char::isDigit) }
    }

    /** "Facebook • <page>" for a post at [url], or null when the address names no page. */
    fun sourceFor(url: String): String? = pageOf(url)?.let { NewsFilters.FACEBOOK_SOURCE_PREFIX + it }
}
