package com.example.data

/**
 * Lends pictures between reports of the same story. Google News items never carry a picture, but
 * the same story usually also arrives through Bing or a newspaper's own feed with one; reusing it
 * costs no network at all.
 *
 * Two headlines count as the same story when most of their words overlap (Jaccard similarity on
 * word sets, ignoring the " - Publisher" tail, punctuation and very short words).
 */
object SameStoryImages {

    private const val MIN_SIMILARITY = 0.6

    /** link → borrowed image URL for every article in [missing] that matches one in [withImage]. */
    fun borrow(missing: List<NewsArticle>, withImage: List<NewsArticle>): Map<String, String> {
        if (missing.isEmpty() || withImage.isEmpty()) return emptyMap()
        val donors = withImage.mapNotNull { a -> a.imageUrl?.takeIf { it.isNotBlank() }?.let { words(a.title) to it } }
        return missing.mapNotNull { article ->
            val own = words(article.title)
            if (own.size < 3) return@mapNotNull null
            donors.maxByOrNull { (w, _) -> similarity(own, w) }
                ?.takeIf { (w, _) -> similarity(own, w) >= MIN_SIMILARITY }
                ?.let { article.link to it.second }
        }.toMap()
    }

    fun similarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / a.union(b).size
    }

    fun words(title: String): Set<String> {
        val headline = if (title.contains(" - ")) title.substringBeforeLast(" - ") else title
        return headline.lowercase()
            .split(Regex("[^\\p{L}\\p{M}\\p{N}]+"))
            .filter { it.length >= 2 }
            .toSet()
    }
}
