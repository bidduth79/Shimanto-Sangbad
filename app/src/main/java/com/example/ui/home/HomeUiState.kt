package com.example.ui.home

import com.example.data.NewsArticle
import com.example.data.RssFeedItem
import com.example.data.SearchMedium
import com.example.data.SourceKind
import com.example.data.searchMediumOf
import com.example.ui.RegionalNews

/** Country tabs on the home screen, in display order. */
enum class HomeTab { ALL, BANGLADESH, INDIA, MYANMAR, INTERNATIONAL }

/**
 * Filter chip keys. The "all" tab offers read/unread; a country tab offers the kind of source
 * (TV, national, local, other) plus a separate "only unread" toggle.
 */
object HomeFilter {
    const val ALL = "সকল"
    const val UNREAD = "অপঠিত"
    const val READ = "পঠিত"
    const val SAVED = "সংরক্ষিত"
    const val TV = "টিভি"
    const val NATIONAL = "জাতীয়"
    const val LOCAL = "লোকাল"
    const val OTHER = "অন্যান্য"
}

/** What the user has picked on the home screen. */
data class HomeSelection(
    val tab: HomeTab = HomeTab.ALL,
    val filter: String = HomeFilter.ALL,
    val searchQuery: String = "",
    /** Keyword picked in the header menu (null = all news). Separate from the free-text search. */
    val keyword: String? = null,
    /** Only stories from the last N hours (null = everything kept, i.e. 36 hours). */
    val maxAgeHours: Int? = null,
    /** Country tabs: hide stories already read. */
    val onlyUnread: Boolean = false,
    val areFilterChipsVisible: Boolean = true
)

data class TabCounts(
    val all: Int = 0,
    val bangladesh: Int = 0,
    val india: Int = 0,
    val myanmar: Int = 0,
    val international: Int = 0
)

data class FilterCounts(
    val total: Int = 0,
    val unread: Int = 0,
    val read: Int = 0,
    val newspaper: Int = 0,
    val google: Int = 0,
    val bing: Int = 0,
    val yahoo: Int = 0,
    val duckDuckGo: Int = 0,
    val reddit: Int = 0,
    val facebook: Int = 0,
    val tv: Int = 0,
    val national: Int = 0,
    val local: Int = 0,
    val other: Int = 0
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selection: HomeSelection = HomeSelection(),
    val tabCounts: TabCounts = TabCounts(),
    val filterCounts: FilterCounts = FilterCounts(),
    /** The list to show for the current tab, filter and search: one card per event. */
    val visibleArticles: List<NewsArticle> = emptyList(),
    /** Other outlets' reports of the same event, by the shown card's link ("+N sources"). */
    val relatedReports: Map<String, List<NewsArticle>> = emptyMap(),
    /** Publish times of every on-topic story (all tabs, no filters): for the "new" badge. */
    val allTimestamps: List<Long> = emptyList(),
    /** Top stories carousel; only on the unfiltered view. */
    val heroArticles: List<NewsArticle> = emptyList(),
    val facebookNews: List<NewsArticle> = emptyList(),
    val customFeedNews: List<NewsArticle> = emptyList(),
    val savedNews: List<NewsArticle> = emptyList(),
    val keywords: List<String> = emptyList(),
    val rssFeeds: List<RssFeedItem> = emptyList(),
    val visitedLinks: Set<String> = emptySet(),
    /** Stories per keyword in the current tab, before the keyword filter is applied. */
    val keywordCounts: Map<String, Int> = emptyMap()
) {
    val showRegionLabels: Boolean get() = selection.tab == HomeTab.ALL
}

/** Everything the home screen derives from. Kept separate so [buildHomeState] stays a pure function. */
data class HomeInputs(
    val regional: RegionalNews,
    val facebookNews: List<NewsArticle>,
    val customFeedNews: List<NewsArticle>,
    val savedNews: List<NewsArticle>,
    val keywords: List<String>,
    val rssFeeds: List<RssFeedItem>,
    val visitedLinks: Set<String>
)

fun buildHomeState(
    inputs: HomeInputs,
    selection: HomeSelection,
    isRefreshing: Boolean,
    now: Long = System.currentTimeMillis()
): HomeUiState {
    val regional = inputs.regional
    val cutoff = selection.maxAgeHours?.let { now - it * 3_600_000L }
    val allTabArticles = when (selection.tab) {
        HomeTab.ALL -> regional.all
        HomeTab.BANGLADESH -> regional.bangladesh
        HomeTab.INDIA -> regional.india
        HomeTab.MYANMAR -> regional.myanmar
        HomeTab.INTERNATIONAL -> regional.international
    }.distinctBy { com.example.data.NewsFilters.headlineKey(it.title) }
        .filter { cutoff == null || it.timestamp >= cutoff }

    // How many stories each keyword has in this tab (for the header menu), then the keyword filter.
    // Matching uses the same headline rules as fetching, so "বিজিবি" also finds "BGB" headlines.
    val keywordsOf = allTabArticles.associateWith { article ->
        (com.example.data.KeywordMatcher.matchingKeywords(article.title, inputs.keywords) + article.matchedKeyword).toSet()
    }
    val keywordCounts = inputs.keywords.associateWith { kw -> keywordsOf.values.count { kw in it } }
    val tabArticles = selection.keyword?.let { kw -> allTabArticles.filter { kw in keywordsOf.getValue(it) } } ?: allTabArticles

    val byMedium = tabArticles.groupBy { searchMediumOf(it) }
    val kindOf = tabArticles.associateWith { com.example.data.NewsSourceKinds.kindOf(it) }
    val byKind = kindOf.values.groupingBy { it }.eachCount()
    val visited = inputs.visitedLinks
    val counts = FilterCounts(
        total = tabArticles.size,
        unread = tabArticles.count { it.link !in visited },
        read = tabArticles.count { it.link in visited },
        newspaper = byMedium[SearchMedium.NEWSPAPER].orEmpty().size,
        google = byMedium[SearchMedium.GOOGLE].orEmpty().size,
        bing = byMedium[SearchMedium.BING].orEmpty().size,
        yahoo = byMedium[SearchMedium.YAHOO].orEmpty().size,
        duckDuckGo = byMedium[SearchMedium.DUCKDUCKGO].orEmpty().size,
        reddit = byMedium[SearchMedium.REDDIT].orEmpty().size,
        facebook = inputs.facebookNews.size,
        tv = byKind[SourceKind.TV] ?: 0,
        national = byKind[SourceKind.NATIONAL] ?: 0,
        local = byKind[SourceKind.LOCAL] ?: 0,
        other = byKind[SourceKind.OTHER] ?: 0
    )

    val filtered = when (val filter = selection.filter) {
        HomeFilter.ALL -> tabArticles
        HomeFilter.UNREAD -> tabArticles.filter { it.link !in visited }
        HomeFilter.READ -> tabArticles.filter { it.link in visited }
        HomeFilter.SAVED -> inputs.savedNews
        HomeFilter.TV -> tabArticles.filter { kindOf[it] == SourceKind.TV }
        HomeFilter.NATIONAL -> tabArticles.filter { kindOf[it] == SourceKind.NATIONAL }
        HomeFilter.LOCAL -> tabArticles.filter { kindOf[it] == SourceKind.LOCAL }
        HomeFilter.OTHER -> tabArticles.filter { kindOf[it] == SourceKind.OTHER }
        SearchMedium.FACEBOOK -> inputs.facebookNews
        SearchMedium.NEWSPAPER, SearchMedium.GOOGLE, SearchMedium.BING, SearchMedium.YAHOO,
        SearchMedium.DUCKDUCKGO, SearchMedium.REDDIT -> byMedium[filter].orEmpty()
        // A keyword chip
        else -> tabArticles.filter {
            it.title.contains(filter, ignoreCase = true) || it.matchedKeyword.equals(filter, ignoreCase = true)
        }
    }

    // Country tabs can hide what was already read, on top of the kind chip
    val unreadOnly = if (selection.onlyUnread && selection.tab != HomeTab.ALL) {
        filtered.filter { it.link !in visited }
    } else filtered

    val query = selection.searchQuery.trim()
    val matching = if (query.isBlank()) unreadOnly else unreadOnly.filter {
        it.title.contains(query, ignoreCase = true) || it.source.contains(query, ignoreCase = true)
    }
    // Same event from several outlets: one card, the rest behind "+N sources"
    val clusters = com.example.data.StoryClusters.cluster(matching)
    val visible = clusters.map { it.lead }
    val related = clusters.filter { it.others.isNotEmpty() }.associate { it.lead.link to it.others }

    return HomeUiState(
        isLoading = isRefreshing && regional.all.isEmpty(),
        isRefreshing = isRefreshing,
        selection = selection,
        allTimestamps = regional.all.map { it.timestamp },
        tabCounts = TabCounts(
            all = regional.all.countRecent(cutoff),
            bangladesh = regional.bangladesh.countRecent(cutoff),
            india = regional.india.countRecent(cutoff),
            myanmar = regional.myanmar.countRecent(cutoff),
            international = regional.international.countRecent(cutoff)
        ),
        filterCounts = counts,
        visibleArticles = visible,
        relatedReports = related,
        heroArticles = if (selection.filter == HomeFilter.ALL && query.isBlank()) heroFrom(visible) else emptyList(),
        facebookNews = inputs.facebookNews,
        customFeedNews = inputs.customFeedNews,
        savedNews = inputs.savedNews,
        keywords = inputs.keywords,
        rssFeeds = inputs.rssFeeds,
        visitedLinks = visited,
        keywordCounts = keywordCounts
    )
}

private const val HERO_COUNT = 4

/** Top stories for the carousel: the newest ones with a picture, topped up with the newest overall. */
private fun heroFrom(visible: List<NewsArticle>): List<NewsArticle> {
    val recent = visible.take(HERO_POOL)
    return (recent.filter { !it.imageUrl.isNullOrBlank() } + recent).distinct().take(HERO_COUNT)
}

private const val HERO_POOL = 15

private fun List<NewsArticle>.countRecent(cutoff: Long?): Int =
    if (cutoff == null) size else count { it.timestamp >= cutoff }
