package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PublisherLinksTest {

    @Test
    fun `search engine and aggregator addresses are never publisher links`() {
        assertFalse(PublisherLinks.isPublisherLink("https://news.google.com/rss/articles/CBMiU0FV?oc=5"))
        assertFalse(PublisherLinks.isPublisherLink("https://www.msn.com/en-in/news/other/x/ar-AA2h8pP6"))
        assertFalse(PublisherLinks.isPublisherLink("http://www.bing.com/news/apiclick.aspx?url=x"))
        assertFalse(PublisherLinks.isPublisherLink("https://feeds.feedburner.com/x"))
        assertTrue(PublisherLinks.isPublisherLink("https://www.prothomalo.com/bangladesh/fhpwwcfiur"))
        assertTrue(PublisherLinks.isPublisherLink("https://www.facebook.com/BorderGuardBangladeshOfficial/posts/1"))
        assertTrue(PublisherLinks.isMsnCopy("https://www.msn.com/en-in/news/other/x/ar-AA2h8pP6"))
    }

    @Test
    fun `tracking parameters are removed, the rest of the address stays`() {
        assertEquals(
            "https://www.ndtv.com/india-news/meghalaya-12308479",
            PublisherLinks.withoutTracking("https://www.ndtv.com/india-news/meghalaya-12308479?utm_source=NDTV&utm_medium=English&utm_campaign=MSN")
        )
        assertEquals(
            "https://paper.com/story?id=5#top",
            PublisherLinks.withoutTracking("https://paper.com/story?id=5&fbclid=abc#top")
        )
        assertEquals("https://paper.com/a", PublisherLinks.withoutTracking("https://paper.com/a"))
    }

    @Test
    fun `without a link the headline and paper's name are sent`() {
        val article = NewsArticle(link = "l", title = "টেকনাফে বিজিবির অভিযান", pubDate = "", source = "Google • Jugantor", timestamp = 1)
        assertEquals("টেকনাফে বিজিবির অভিযান\nJugantor", PublisherLinks.fallbackText(article))
        val unknown = article.copy(source = "Google News")
        assertEquals("টেকনাফে বিজিবির অভিযান", PublisherLinks.fallbackText(unknown))
    }

    @Test
    fun `widget lists only today's BGB and BSF stories`() {
        val now = 1_794_322_800_000L
        val start = NewsFilters.startOfToday(now)
        fun story(link: String, title: String, ts: Long) = NewsArticle(link = link, title = title, pubDate = "", source = "s", timestamp = ts)
        val picked = WidgetStories.select(
            listOf(
                story("a", "সীমান্তে বিজিবির টহল", now - 60_000),
                story("b", "BSF jawan injured near Bangladesh border in Tripura", now - 120_000),
                story("c", "সীমান্তে চোরাচালান বেড়েছে", now - 60_000),      // no BGB/BSF
                story("d", "বিজিবির অভিযান", start - 60_000)                 // yesterday
            ),
            now
        )
        assertEquals(listOf("a", "b"), picked.map { it.link })
    }
}
