package com.example.data

import com.example.data.NewsFilters.isBengali
import javax.inject.Inject
import javax.inject.Singleton

/** One network request to make, tagged with the user keyword it serves. */
typealias SearchTask = suspend () -> List<NewsArticle>

/**
 * Decides WHICH requests to make for each country tab (search engine, edition, query wording).
 * Executing them, filtering and storing is [NewsRepository]'s job.
 */
@Singleton
class NewsSearchPlanner @Inject constructor(
    private val remote: NewsRemoteDataSource
) {

    fun plan(region: NewsRegion, keywords: List<String>): List<SearchTask> = when (region) {
        NewsRegion.BANGLADESH -> planBangladesh(keywords)
        NewsRegion.INDIA -> planIndia(keywords)
        NewsRegion.MYANMAR -> planMyanmar(keywords)
        NewsRegion.INTERNATIONAL -> planInternational(keywords)
    }

    fun planFacebook(keywords: List<String>): List<SearchTask> =
        keywords.map { keyword -> { remote.fetchFacebookNews(keyword) } }

    fun planCustomFeeds(feeds: List<RssFeedItem>): List<SearchTask> =
        feeds.map { feed -> { remote.fetchCustomRssFeed(feed) } }

    /** Google (bn + en editions) and Bing, plus Bangladeshi papers' own feeds. */
    private fun planBangladesh(keywords: List<String>): List<SearchTask> = buildList {
        add { remote.fetchNewspaperFeeds(NewsRegion.BANGLADESH, keywords) }
        for (topic in keywords) {
            add { google(topic, "$topic $NO_FACEBOOK", Edition.BD_BN.takeIf { isBengali(topic) } ?: Edition.BD_EN) }
        }
        for (keyword in keywords) {
            if (isBengali(keyword)) {
                addBing(keyword, query = keyword, bingMarket = "en-US")
                for (en in remote.getEnglishSearchVariants(keyword)) {
                    add { google(keyword, "$en $NO_FACEBOOK", Edition.BD_EN) }
                    addBing(keyword, query = en, bingMarket = "en-US")
                }
            } else {
                addBing(keyword, query = "$keyword Bangladesh", bingMarket = "en-US")
            }
        }
    }

    /** Indian editions of Google News in Bengali and English, Bing India, plus Indian papers' feeds. */
    private fun planIndia(keywords: List<String>): List<SearchTask> = buildList {
        if (keywords.isEmpty()) return@buildList
        add { remote.fetchNewspaperFeeds(NewsRegion.INDIA, keywords) }
        for (keyword in keywords) {
            if (isBengali(keyword)) {
                add { google(keyword, "$keyword $NOT_BD_MEDIA_IN", Edition.IN_BN) }
                add { google(keyword, "$keyword ($INDIA_CONTEXT) $NOT_BD_MEDIA_IN", Edition.IN_BN) }
                add { remote.fetchBingNews("$keyword ভারত", market = "en-IN", matchedTag = keyword) }
                for (en in remote.getEnglishSearchVariants(keyword)) {
                    add { google(keyword, "$en $NOT_BD_MEDIA_IN", Edition.IN_EN) }
                    addBing(keyword, query = "$en India", bingMarket = "en-IN")
                }
            } else {
                add { google(keyword, "$keyword $NOT_BD_MEDIA_IN", Edition.IN_EN) }
                add { google(keyword, "$keyword $NOT_BD_MEDIA_IN", Edition.IN_BN) }
                addBing(keyword, query = "$keyword India", bingMarket = "en-IN")
            }
        }
    }

    /**
     * Myanmar outlets via Google plus The Irrawaddy feed. Most Myanmar-border stories reach the tab
     * through the Bangladesh and international searches (the tab is chosen by publisher); dozens of
     * extra Rakhine/Arakan queries kept none of their results, so only one per keyword remains.
     */
    private fun planMyanmar(keywords: List<String>): List<SearchTask> = buildList {
        if (keywords.isEmpty()) return@buildList
        add { google("মিয়ানমার", MYANMAR_OUTLETS_QUERY, Edition.US_EN) }
        add { remote.fetchNewspaperFeeds(NewsRegion.MYANMAR, keywords) }
        for (keyword in keywords) {
            val query = remote.getMyanmarSearchVariants(keyword).firstOrNull() ?: continue
            add { google(keyword, "$query $NOT_BD_MEDIA_GLOBAL", Edition.US_EN) }
        }
    }

    /** Google US/UK editions, global engines, Bengali-language world services, plus world outlets' feeds. */
    private fun planInternational(keywords: List<String>): List<SearchTask> = buildList {
        if (keywords.isEmpty()) return@buildList
        add { remote.fetchNewspaperFeeds(NewsRegion.INTERNATIONAL, keywords) }
        for (keyword in keywords) {
            if (isBengali(keyword)) {
                add { google(keyword, "$keyword $NOT_BD_MEDIA_GLOBAL", Edition.US_BN) }
                add { remote.fetchBingNews(keyword, market = "en-US", matchedTag = keyword) }
            }
            for (query in remote.getEnglishSearchVariants(keyword)) {
                add { google(keyword, "$query $NOT_BD_MEDIA_GLOBAL", Edition.US_EN) }
                add { google(keyword, "$query $NOT_BD_MEDIA_GLOBAL", Edition.GB_EN) }
                addBing(keyword, query = query, bingMarket = "en-US")
            }
        }
    }

    /**
     * Bing News for one query. Yahoo, DuckDuckGo and Reddit were dropped: measured over a full
     * refresh they returned ~0 headlines about the keywords while costing about half of all requests.
     */
    private fun MutableList<SearchTask>.addBing(keyword: String, query: String, bingMarket: String) {
        add { remote.fetchBingNews(query, market = bingMarket, matchedTag = keyword) }
    }

    private suspend fun google(keyword: String, query: String, edition: Edition): List<NewsArticle> =
        remote.fetchNews(query, hl = edition.hl, gl = edition.gl, ceid = edition.ceid, whenWindow = "1d")
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
