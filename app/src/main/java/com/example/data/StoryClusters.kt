package com.example.data

/**
 * Groups reports of the same event from different outlets (like Google News "full coverage"):
 * the list shows one card per event with "+N sources", instead of ten near-identical cards.
 *
 * Two headlines are the same event when at least [MIN_SIMILARITY] of their words overlap
 * (Jaccard, see [SameStoryImages.words]) and they were published within [MAX_GAP_MILLIS].
 * The threshold is deliberately strict: merging two different events is worse than missing a pair.
 */
object StoryClusters {
    const val MIN_SIMILARITY = 0.5
    private const val MAX_GAP_MILLIS = 36L * 60 * 60 * 1000

    data class Cluster(val lead: NewsArticle, val others: List<NewsArticle>)

    /**
     * [articles] newest first. Clusters keep the order of their newest report; the lead is the
     * newest report that has a picture (the newest overall when none has one).
     */
    fun cluster(articles: List<NewsArticle>): List<Cluster> {
        class Group(val words: Set<String>, val firstTime: Long, val members: MutableList<NewsArticle>)

        val groups = ArrayList<Group>()
        for (article in articles) {
            val words = SameStoryImages.words(article.title)
            val match = groups.firstOrNull { group ->
                kotlin.math.abs(group.firstTime - article.timestamp) <= MAX_GAP_MILLIS &&
                    SameStoryImages.similarity(group.words, words) >= MIN_SIMILARITY
            }
            if (match != null) match.members += article
            else groups += Group(words, article.timestamp, mutableListOf(article))
        }
        return groups.map { group ->
            val lead = group.members.firstOrNull { !it.imageUrl.isNullOrBlank() } ?: group.members.first()
            Cluster(lead, group.members - lead)
        }
    }
}
