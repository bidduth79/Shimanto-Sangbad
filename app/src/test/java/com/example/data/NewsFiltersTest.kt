package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsFiltersTest {
    private val now = 1_800_000_000_000L
    private val hour = 60 * 60 * 1000L

    private fun article(
        title: String = "সীমান্তে বিজিবির টহল",
        source: String = "Google • প্রথম আলো",
        link: String = "https://news.google.com/rss/articles/x",
        timestamp: Long = now - hour,
        matchedKeyword: String = ""
    ) = NewsArticle(link = link, title = title, pubDate = "", source = source, timestamp = timestamp, matchedKeyword = matchedKeyword)

    @Test
    fun `instagram and tiktok results are social posts, newspapers are not`() {
        assertTrue(NewsFilters.isSocialMediaPost(article(source = "Google • Instagram")))
        assertTrue(NewsFilters.isSocialMediaPost(article(link = "https://www.tiktok.com/@x/video/1")))
        // Google News hides the real link, so X posts are recognised by the publisher name alone
        assertTrue(NewsFilters.isSocialMediaPost(article(source = "Google • X")))
        assertFalse(NewsFilters.isSocialMediaPost(article(source = "Google • Xinhua")))
        assertFalse(NewsFilters.isSocialMediaPost(article(source = "Google • প্রথম আলো")))
        // A paper whose name merely contains a platform word is not a social post
        assertFalse(NewsFilters.isSocialMediaPost(article(source = "Google • Instagram Times Weekly News")))
    }

    @Test
    fun `freshness window is 36 hours with small clock skew allowed`() {
        assertTrue(NewsFilters.isFresh(article(timestamp = now - 35 * hour), now))
        assertFalse(NewsFilters.isFresh(article(timestamp = now - 37 * hour), now))
        assertTrue(NewsFilters.isFresh(article(timestamp = now + 5 * 60 * 1000), now))
        assertFalse(NewsFilters.isFresh(article(timestamp = 0), now))
    }

    @Test
    fun `cleaning drops stale, sports and social items and tags the category`() {
        val keep = article(title = "সীমান্তে বিজিবির টহল জোরদার", link = "l1")
        val stale = article(title = "পুরনো সীমান্ত খবর বিজিবি", link = "l2", timestamp = now - 48 * hour)
        val sports = article(title = "বিজিবি ক্রিকেট দল জয়ী", link = "l3")
        val social = article(title = "বিজিবি ভিডিও", link = "l4", source = "Google • Instagram")

        val offTopic = article(title = "পাংশায় ৫৫তম জাতীয় সমবায় দিবস উদযাপন", link = "l5", matchedKeyword = "বিজিবি")

        val cleaned = NewsFilters.cleanRegionResults(
            listOf(keep, stale, sports, social, offTopic), category = "bgb",
            keywords = listOf("বিজিবি"), now = now
        )

        assertEquals(listOf("l1"), cleaned.map { it.link })
        assertEquals("bgb", cleaned.single().category)
        assertEquals("বিজিবি", cleaned.single().matchedKeyword)
    }

    @Test
    fun `search medium is read from the source prefix`() {
        assertEquals(SearchMedium.NEWSPAPER, searchMediumOf(article(source = "পত্রিকা • ইত্তেফাক")))
        assertEquals(SearchMedium.BING, searchMediumOf(article(source = "Bing • Reuters")))
        assertEquals(SearchMedium.GOOGLE, searchMediumOf(article(source = "Google • প্রথম আলো")))
    }
}
