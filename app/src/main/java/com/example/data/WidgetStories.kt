package com.example.data

/** The stories the home-screen widget lists: today's, about BGB or BSF, one per headline. */
object WidgetStories {
    /** Every fresh story is listed; the cap only guards the system's size limit for widgets. */
    const val MAX_ITEMS = 100
    val KEYWORDS = listOf("বিজিবি", "বিএসএফ")

    fun select(recent: List<NewsArticle>, now: Long = System.currentTimeMillis()): List<NewsArticle> = recent
        .asSequence()
        .filter { it.timestamp >= NewsFilters.startOfToday(now) && it.timestamp <= now + 10 * 60_000 }
        .filter { KeywordMatcher.matches(it.title, KEYWORDS) }
        .distinctBy { NewsFilters.headlineKey(it.title) }
        .take(MAX_ITEMS)
        .toList()
}
