package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.NewsArticle
import com.example.data.NewsRegion
import com.example.data.NewsRegionClassifier

/** Home-screen buckets: every article once in [all], and again under the tab of its publisher's country. */
data class RegionalNews(
    val all: List<NewsArticle> = emptyList(),
    val bangladesh: List<NewsArticle> = emptyList(),
    val india: List<NewsArticle> = emptyList(),
    val myanmar: List<NewsArticle> = emptyList(),
    val international: List<NewsArticle> = emptyList()
)

fun groupNewsByRegion(vararg sources: List<NewsArticle>): RegionalNews {
    val all = sources.asList().flatten()
        .distinctBy { it.link }
        .distinctBy { com.example.data.NewsFilters.headlineKey(it.title) }
        .sortedByDescending { it.timestamp }
    val byRegion = all.groupBy { it.displayRegion() }
    return RegionalNews(
        all = all,
        bangladesh = byRegion[NewsRegion.BANGLADESH].orEmpty(),
        india = byRegion[NewsRegion.INDIA].orEmpty(),
        myanmar = byRegion[NewsRegion.MYANMAR].orEmpty(),
        international = byRegion[NewsRegion.INTERNATIONAL].orEmpty()
    )
}

/**
 * The country tab for this article, as stored when it was written. Rows from before DB v6 have none
 * until the background backfill reaches them; meanwhile they use the tab they were fetched for, which
 * is cheap (classifying on the UI path took seconds for a full list).
 */
fun NewsArticle.displayRegion(): NewsRegion =
    region.takeIf { it.isNotBlank() }?.let { runCatching { NewsRegion.valueOf(it) }.getOrNull() }
        ?: NewsRegionClassifier.fallbackRegion(category)

/** Tab name for this article's country, shown as a tag on cards in the "all" tab. */
@Composable
fun NewsArticle.regionLabel(): String = stringResource(
    when (displayRegion()) {
        NewsRegion.BANGLADESH -> R.string.tab_bangladesh
        NewsRegion.INDIA -> R.string.tab_india
        NewsRegion.MYANMAR -> R.string.tab_myanmar
        NewsRegion.INTERNATIONAL -> R.string.tab_international
    }
)
