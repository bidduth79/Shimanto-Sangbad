package com.example.data

import com.example.ui.groupNewsByRegion
import org.junit.Assert.assertEquals
import org.junit.Test

class NewsRegionClassifierTest {

    // Each article gets a globally unique link: the classifier caches results per link for the whole process
    private fun article(
        title: String = "শিরোনাম",
        source: String = "",
        link: String = "https://news.google.com/rss/articles/${java.util.UUID.randomUUID()}",
        category: String = "bgb",
        timestamp: Long = 1L
    ) = NewsArticle(link = link, title = title, pubDate = "", source = source, timestamp = timestamp, category = category)

    @Test
    fun builtInFeed_usesItsOwnRegion() {
        // Purber Kalom is a Kolkata paper, so it belongs to India even though it was fetched for Bangladesh
        assertEquals(NewsRegion.INDIA, NewsRegionClassifier.regionOf(article(source = "পত্রিকা • পুবের কলম")))
        assertEquals(NewsRegion.BANGLADESH, NewsRegionClassifier.regionOf(article(source = "পত্রিকা • প্রথম আলো")))
    }

    @Test
    fun indianPublisherFoundByBangladeshSearch_goesToIndia() {
        val a = article(title = "সীমান্তে বিএসএফ - TV9 Bangla", source = "Google • TV9 Bangla", category = "bgb")
        assertEquals(NewsRegion.INDIA, NewsRegionClassifier.regionOf(a))
    }

    @Test
    fun bangladeshiPublisherFoundByIndiaSearch_goesToBangladesh() {
        val a = article(title = "সীমান্ত হত্যা - প্রথম আলো", source = "Google • প্রথম আলো", category = "indian")
        assertEquals(NewsRegion.BANGLADESH, NewsRegionClassifier.regionOf(a))
    }

    @Test
    fun myanmarPublisher_goesToMyanmar() {
        val a = article(title = "Arakan Army - The Irrawaddy", source = "Google • The Irrawaddy", category = "international")
        assertEquals(NewsRegion.MYANMAR, NewsRegionClassifier.regionOf(a))
    }

    @Test
    fun googleNewsLinkIdNeverDecidesRegion() {
        // The random article ID happens to contain "tv9" (an Indian marker) and "gnlm" (a Myanmar one)
        val a = article(
            title = "Two more children die - Bangladesh Sangbad Sangstha (BSS)",
            source = "Google • Bangladesh Sangbad Sangstha (BSS)",
            link = "https://news.google.com/rss/articles/CBMiXtv9Qgnlm${java.util.UUID.randomUUID()}",
            category = "bgb"
        )
        assertEquals(NewsRegion.BANGLADESH, NewsRegionClassifier.regionOf(a))
    }

    @Test
    fun indianDomain_goesToIndia() {
        val a = article(source = "Bing • Example", link = "https://www.example.in/news/1", category = "bgb")
        assertEquals(NewsRegion.INDIA, NewsRegionClassifier.regionOf(a))
    }

    @Test
    fun unknownPublisher_keepsFetchedTab() {
        assertEquals(NewsRegion.INTERNATIONAL, NewsRegionClassifier.regionOf(article(source = "Google • Reuters", category = "international")))
        assertEquals(NewsRegion.BANGLADESH, NewsRegionClassifier.regionOf(article(source = "Google • Reuters", category = "bgb")))
    }

    @Test
    fun headlineWordsDoNotDecideRegion() {
        // "কলকাতা" in the headline of a Bangladeshi paper must not move it to India
        val a = article(title = "কলকাতা থেকে ফিরলেন - যুগান্তর", source = "Google • যুগান্তর", category = "bgb")
        assertEquals(NewsRegion.BANGLADESH, NewsRegionClassifier.regionOf(a))
    }

    @Test
    fun grouping_putsEachArticleInAllOnceAndInItsRegion() {
        // Grouping uses the region stored at write time
        val bd = article(title = "এক - প্রথম আলো", source = "Google • প্রথম আলো", link = "l1", timestamp = 3)
            .copy(region = "BANGLADESH")
        val inArticle = article(title = "দুই - আনন্দবাজার", source = "Google • আনন্দবাজার", link = "l2", category = "bgb", timestamp = 2)
            .copy(region = NewsRegionClassifier.regionOf(article(source = "Google • আনন্দবাজার")).name)
        val dup = bd.copy(category = "national")
        val grouped = groupNewsByRegion(listOf(bd, inArticle), listOf(dup))
        assertEquals(listOf("l1", "l2"), grouped.all.map { it.link })
        assertEquals(listOf("l1"), grouped.bangladesh.map { it.link })
        assertEquals(listOf("l2"), grouped.india.map { it.link })
    }
}
