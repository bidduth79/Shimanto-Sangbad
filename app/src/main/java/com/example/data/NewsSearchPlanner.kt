package com.example.data

import com.example.data.NewsFilters.isBengali
import javax.inject.Inject
import javax.inject.Singleton

/** One network request to make, tagged with the user keyword it serves. */
typealias SearchTask = suspend () -> List<NewsArticle>

/**
 * Decides WHICH requests to make for each country tab (search engine, edition, query wording).
 * Executing them, filtering and storing is [NewsRepository]'s job.
 *
 * Every refresh asks Google for the newest headlines: the keyword in the headline ([SearchQueries]),
 * last three hours, a handful of requests. The slower-changing parts ([Depth]) — the whole day's
 * results, plain keyword searches and Bing — are added only every so often.
 */
@Singleton
class NewsSearchPlanner @Inject constructor(
    private val remote: NewsRemoteDataSource
) {

    /** Which of the slower-changing parts of a plan to include this time. */
    data class Depth(val deep: Boolean = true, val bing: Boolean = true)

    fun plan(region: NewsRegion, keywords: List<String>, depth: Depth = Depth()): List<SearchTask> = when (region) {
        NewsRegion.BANGLADESH -> planBangladesh(keywords, depth)
        NewsRegion.INDIA -> planIndia(keywords, depth)
        NewsRegion.MYANMAR -> planMyanmar(keywords, depth)
        NewsRegion.INTERNATIONAL -> planInternational(keywords, depth)
    }

    /** Public Facebook posts with a keyword in their text's first line (Google indexes it as the title). */
    fun planFacebook(keywords: List<String>, depth: Depth = Depth()): List<SearchTask> = buildList {
        // "border" alone would bring posts from all over the world
        val terms = SearchQueries.bengaliTerms(keywords) +
            SearchQueries.englishTerms(keywords).filter { !KeywordMatcher.isGenericEnglishTerm(it.text) }
        terms.chunked(TERMS_PER_FRESH_QUERY).forEach { group ->
            add { remote.fetchFacebookPosts(SearchQueries.anyInTitle(group), FRESH_WINDOW, group[0].keyword) }
        }
        if (depth.deep) {
            for (term in terms) {
                add { remote.fetchFacebookPosts(SearchQueries.inTitle(term.text), DAY_WINDOW, term.keyword) }
            }
        }
    }

    fun planCustomFeeds(feeds: List<RssFeedItem>): List<SearchTask> =
        feeds.map { feed -> { remote.fetchCustomRssFeed(feed) } }

    /** Bangladeshi papers' own feeds, Google's Bangladesh editions (bn + en) and Bing. */
    private fun planBangladesh(keywords: List<String>, depth: Depth): List<SearchTask> = buildList {
        add { remote.fetchNewspaperFeeds(NewsRegion.BANGLADESH, keywords) }
        val bengali = SearchQueries.bengaliTerms(keywords)
        val english = SearchQueries.englishTerms(keywords)

        bengali.chunked(TERMS_PER_FRESH_QUERY).forEach { group ->
            add { google(group[0].keyword, "${SearchQueries.anyInTitle(group)} $NO_FACEBOOK", Edition.BD_BN, FRESH_WINDOW) }
        }
        english.chunked(TERMS_PER_FRESH_QUERY).forEach { group ->
            add { google(group[0].keyword, "${SearchQueries.english(group, global = false)} $NO_FACEBOOK", Edition.BD_EN, FRESH_WINDOW) }
        }
        if (depth.deep) {
            // The rest of the day, one term per search (each search is capped at 100 results)
            for (term in bengali) {
                val query = "${SearchQueries.inTitle(term.text)} $NO_FACEBOOK"
                add { google(term.keyword, query, Edition.BD_BN, HALF_DAY_WINDOW) }
                add { google(term.keyword, query, Edition.BD_BN, DAY_WINDOW) }
            }
            for (term in english) {
                add { google(term.keyword, "${SearchQueries.english(listOf(term), global = false)} $NO_FACEBOOK", Edition.BD_EN, DAY_WINDOW) }
            }
            // Plain keyword searches still find headlines that word it differently
            for (keyword in keywords) {
                add { google(keyword, "$keyword $NO_FACEBOOK", if (isBengali(keyword)) Edition.BD_BN else Edition.BD_EN, DAY_WINDOW) }
            }
        }
        if (depth.bing) {
            for (keyword in keywords) {
                if (isBengali(keyword)) {
                    addBing(keyword, query = keyword, bingMarket = "en-US")
                    for (en in remote.getEnglishSearchVariants(keyword)) addBing(keyword, query = en, bingMarket = "en-US")
                } else {
                    addBing(keyword, query = "$keyword Bangladesh", bingMarket = "en-US")
                }
            }
        }
    }

    /** Indian papers' feeds, Google's India editions (bn + en) and Bing India. */
    private fun planIndia(keywords: List<String>, depth: Depth): List<SearchTask> = buildList {
        if (keywords.isEmpty()) return@buildList
        add { remote.fetchNewspaperFeeds(NewsRegion.INDIA, keywords) }
        val bengali = SearchQueries.bengaliTerms(keywords)
        val english = SearchQueries.englishTerms(keywords)

        bengali.chunked(TERMS_PER_FRESH_QUERY).forEach { group ->
            add { google(group[0].keyword, "${SearchQueries.anyInTitle(group)} $NOT_BD_MEDIA_IN", Edition.IN_BN, FRESH_WINDOW) }
        }
        english.chunked(TERMS_PER_FRESH_QUERY).forEach { group ->
            add { google(group[0].keyword, "${SearchQueries.english(group, global = false)} $NOT_BD_MEDIA_IN", Edition.IN_EN, FRESH_WINDOW) }
        }
        if (depth.deep) {
            for (term in bengali) {
                add { google(term.keyword, "${SearchQueries.inTitle(term.text)} $NOT_BD_MEDIA_IN", Edition.IN_BN, DAY_WINDOW) }
            }
            for (term in english) {
                add { google(term.keyword, "${SearchQueries.english(listOf(term), global = false)} $NOT_BD_MEDIA_IN", Edition.IN_EN, DAY_WINDOW) }
            }
            for (keyword in keywords.filter { isBengali(it) }) {
                add { google(keyword, "$keyword ($INDIA_CONTEXT) $NOT_BD_MEDIA_IN", Edition.IN_BN, DAY_WINDOW) }
            }
        }
        if (depth.bing) {
            for (keyword in keywords) {
                if (isBengali(keyword)) {
                    add { remote.fetchBingNews("$keyword ভারত", market = "en-IN", matchedTag = keyword) }
                    for (en in remote.getEnglishSearchVariants(keyword)) addBing(keyword, query = "$en India", bingMarket = "en-IN")
                } else {
                    addBing(keyword, query = "$keyword India", bingMarket = "en-IN")
                }
            }
        }
    }

    /**
     * Myanmar outlets' feeds plus one Google search limited to Myanmar outlets. Most Myanmar-border
     * stories reach the app through the Bangladesh and international searches (the tab is chosen by
     * publisher).
     */
    private fun planMyanmar(keywords: List<String>, depth: Depth): List<SearchTask> = buildList {
        if (keywords.isEmpty()) return@buildList
        add { google("মিয়ানমার", MYANMAR_OUTLETS_QUERY, Edition.US_EN, DAY_WINDOW) }
        add { remote.fetchNewspaperFeeds(NewsRegion.MYANMAR, keywords) }
        if (depth.deep) {
            for (keyword in keywords) {
                val query = remote.getMyanmarSearchVariants(keyword).firstOrNull() ?: continue
                add { google(keyword, "$query $NOT_BD_MEDIA_GLOBAL", Edition.US_EN, DAY_WINDOW) }
            }
        }
    }

    /** World outlets' feeds, Google's US/UK editions and Bing. */
    private fun planInternational(keywords: List<String>, depth: Depth): List<SearchTask> = buildList {
        if (keywords.isEmpty()) return@buildList
        add { remote.fetchNewspaperFeeds(NewsRegion.INTERNATIONAL, keywords) }
        val bengali = SearchQueries.bengaliTerms(keywords)
        val english = SearchQueries.englishTerms(keywords)

        english.chunked(TERMS_PER_FRESH_QUERY).forEach { group ->
            val query = "${SearchQueries.english(group, global = true)} $NOT_BD_MEDIA_GLOBAL"
            add { google(group[0].keyword, query, Edition.US_EN, FRESH_WINDOW) }
            add { google(group[0].keyword, query, Edition.GB_EN, FRESH_WINDOW) }
        }
        if (depth.deep) {
            for (term in english) {
                val query = "${SearchQueries.english(listOf(term), global = true)} $NOT_BD_MEDIA_GLOBAL"
                add { google(term.keyword, query, Edition.US_EN, DAY_WINDOW) }
                add { google(term.keyword, query, Edition.GB_EN, DAY_WINDOW) }
            }
            // Bengali-language outlets abroad
            for (term in bengali) {
                add { google(term.keyword, "${SearchQueries.inTitle(term.text)} $NOT_BD_MEDIA_GLOBAL", Edition.US_BN, DAY_WINDOW) }
            }
        }
        if (depth.bing) {
            for (keyword in keywords) {
                if (isBengali(keyword)) add { remote.fetchBingNews(keyword, market = "en-US", matchedTag = keyword) }
                for (query in remote.getEnglishSearchVariants(keyword)) addBing(keyword, query = query, bingMarket = "en-US")
            }
        }
    }

    /**
     * Bing News for one query (its newest dozen stories). Yahoo, DuckDuckGo and Reddit were dropped:
     * measured over a full refresh they returned ~0 headlines about the keywords.
     */
    private fun MutableList<SearchTask>.addBing(keyword: String, query: String, bingMarket: String) {
        add { remote.fetchBingNews(query, market = bingMarket, matchedTag = keyword) }
    }

    private suspend fun google(keyword: String, query: String, edition: Edition, window: String): List<NewsArticle> =
        remote.fetchNews(query, hl = edition.hl, gl = edition.gl, ceid = edition.ceid, whenWindow = window)
            .map { it.copy(matchedKeyword = keyword) }

    private enum class Edition(val hl: String, val gl: String, val ceid: String) {
        BD_BN("bn", "BD", "BD:bn"),
        BD_EN("en-BD", "BD", "BD:en"),
        IN_BN("bn", "IN", "IN:bn"),
        IN_EN("en-IN", "IN", "IN:en"),
        US_EN("en-US", "US", "US:en"),
        US_BN("bn", "US", "US:bn"),
        GB_EN("en-GB", "GB", "GB:en")
    }

    private companion object {
        /** Newest headlines, asked on every refresh. */
        const val FRESH_WINDOW = "3h"
        const val HALF_DAY_WINDOW = "8h"
        const val DAY_WINDOW = "1d"
        /** Terms OR-ed into one newest-headlines search; more would risk Google's 100-result cap. */
        const val TERMS_PER_FRESH_QUERY = 2

        const val NO_FACEBOOK = "-site:facebook.com -site:fb.com"
        const val NOT_BD_MEDIA_IN = "-site:.bd -site:facebook.com -site:fb.com -site:prothomalo.com " +
            "-site:jugantor.com -site:kalerkantho.com -site:dhakapost.com -site:somoynews.tv " +
            "-site:jagonews24.com -site:banglatribune.com -site:bd-pratidin.com"
        const val NOT_BD_MEDIA_GLOBAL = "-site:.bd -site:facebook.com -site:fb.com -site:prothomalo.com " +
            "-site:jugantor.com -site:kalerkantho.com -site:dhakapost.com -site:thedailystar.net " +
            "-site:tbsnews.net -site:dhakatribune.com -site:somoynews.tv -site:jagonews24.com"
        const val INDIA_CONTEXT = "ভারত OR বিএসএফ OR পশ্চিমবঙ্গ OR ত্রিপুরা OR আসাম OR কলকাতা"
        const val MYANMAR_OUTLETS_QUERY = "(site:irrawaddy.com OR site:myanmar-now.org OR site:mizzima.com " +
            "OR site:narinjara.com OR site:bnionline.net OR site:frontiermyanmar.net OR site:dvb.no " +
            "OR site:elevenmyanmar.com) (Bangladesh OR Rakhine OR Arakan OR border OR Maungdaw OR Rohingya)"
    }
}
