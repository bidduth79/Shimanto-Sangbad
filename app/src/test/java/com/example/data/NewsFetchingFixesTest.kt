package com.example.data

import com.example.data.local.keptOriginalLink
import com.example.data.local.keptSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewsFetchingFixesTest {

    private val hour = 3_600_000L
    // 2026-11-10 15:00 UTC (winter: Pacific time is UTC-8)
    private val now = 1_794_322_800_000L

    private fun article(link: String, timestamp: Long, pubDate: String = "", title: String = "বিজিবি $link") =
        NewsArticle(link = link, title = title, pubDate = pubDate, source = "s", timestamp = timestamp)

    @Before
    fun reset() {
        FeedClock.resetForTest()
        SourceHealth.resetForTest()
    }

    // ---- Bing: Pacific time labelled GMT ----

    @Test
    fun `bing times are read as pacific time`() {
        // Published 30 minutes ago; Bing states it 8 hours earlier
        val stated = now - 30 * 60_000 - 8 * hour
        val fixed = FeedClock.fixBing(listOf(article("a", stated), article("b", stated - hour)), now)
        assertEquals(now - 30 * 60_000, fixed[0].timestamp)
        assertEquals(now - 90 * 60_000, fixed[1].timestamp)
    }

    @Test
    fun `bing times are left alone once bing states real GMT`() {
        // A correctly stated recent item would land in the future if shifted: the feed is fixed
        val items = listOf(article("a", now - 10 * 60_000), article("b", now - 5 * hour))
        assertEquals(items, FeedClock.fixBing(items, now))
    }

    // ---- feeds writing local time under a UTC label ----

    @Test
    fun `a flagged feed is moved back by the local offset, only where the label says UTC`() {
        val items = listOf(
            article("utc", now + 5 * hour, "Tue, 10 Nov 2026 20:00:00 UTC"),
            article("local", now - hour, "Tue, 10 Nov 2026 20:00:00 +0600")
        )
        val fixed = FeedClock.fixLocalLabelledAsUtc("u", knownBroken = true, FeedClock.BANGLADESH_OFFSET_MILLIS, items, now)
        assertEquals(now - hour, fixed[0].timestamp)
        assertEquals(now - hour, fixed[1].timestamp)
    }

    @Test
    fun `an unknown feed is learned when several items come from the future`() {
        val broken = (1..5).map { article("f$it", now + it * hour, "Tue, 10 Nov 2026 2$it:00:00 GMT") }
        val fixed = FeedClock.fixLocalLabelledAsUtc("feed-x", false, FeedClock.BANGLADESH_OFFSET_MILLIS, broken, now)
        assertEquals(now - 5 * hour, fixed[0].timestamp)
        // Later, at night, nothing looks "future" any more: the feed is still corrected
        val quiet = listOf(article("old", now - 2 * hour, "Tue, 10 Nov 2026 13:00:00 GMT"))
        assertEquals(now - 8 * hour, FeedClock.fixLocalLabelledAsUtc("feed-x", false, FeedClock.BANGLADESH_OFFSET_MILLIS, quiet, now)[0].timestamp)
    }

    @Test
    fun `a healthy feed with one scheduled item is not touched`() {
        val items = (1..10).map { article("h$it", now - it * hour, "x GMT") } + article("soon", now + hour, "x GMT")
        assertEquals(items, FeedClock.fixLocalLabelledAsUtc("feed-y", false, FeedClock.BANGLADESH_OFFSET_MILLIS, items, now))
    }

    // ---- headline searches ----

    @Test
    fun `default keywords collapse to four bengali and three english search terms`() {
        val keywords = listOf("বিজিবি", "বর্ডার গার্ড বাংলাদেশ", "BGB", "বিএসএফ", "BSF", "সীমান্ত", "বাংলাদেশ ভারত সীমান্ত", "সীমান্ত সংবাদ")
        assertEquals(listOf("বিজিবি", "বর্ডার গার্ড", "বিএসএফ", "সীমান্ত"), SearchQueries.bengaliTerms(keywords).map { it.text })
        assertEquals(listOf("BGB", "BSF", "border"), SearchQueries.englishTerms(keywords).map { it.text })
    }

    @Test
    fun `queries ask for the term in the headline`() {
        assertEquals("intitle:বিজিবি", SearchQueries.inTitle("বিজিবি"))
        assertEquals("intitle:\"বর্ডার গার্ড\"", SearchQueries.inTitle("বর্ডার গার্ড"))
        val group = listOf(SearchQueries.Term("বিজিবি", "বিজিবি"), SearchQueries.Term("বর্ডার গার্ড", "বিজিবি"))
        assertEquals("(intitle:বিজিবি OR intitle:\"বর্ডার গার্ড\")", SearchQueries.anyInTitle(group))
    }

    @Test
    fun `generic english words always get region context, acronyms only in global editions`() {
        val border = listOf(SearchQueries.Term("border", "সীমান্ত"))
        val bgb = listOf(SearchQueries.Term("BGB", "বিজিবি"))
        assertTrue(SearchQueries.english(border, global = false).endsWith(SearchQueries.REGION_CONTEXT))
        assertEquals("intitle:BGB", SearchQueries.english(bgb, global = false))
        assertEquals("intitle:BGB ${SearchQueries.REGION_CONTEXT}", SearchQueries.english(bgb, global = true))
    }

    // ---- link decoding ----

    @Test
    fun `batch reply answers are matched to their request numbers`() {
        val reply = ")]}'\n\n[[\"wrb.fr\",\"Fbv4je\",\"[\\\"garturlres\\\",\\\"https://www.dhakatimes24.com/2026/11/10/416870\\\",1]\",null,null,null,\"2\"]," +
            "[\"wrb.fr\",\"Fbv4je\",\"[\\\"garturlres\\\",\\\"https://www.jagonews24.com/national/news/1177973\\\",1,\\\"https://amp.example/x\\\"]\",null,null,null,\"1\"]," +
            "[\"wrb.fr\",\"Fbv4je\",null,null,null,null,\"3\"],[\"di\",59],[\"af.httprm\",59,\"-1\",12]]"
        assertEquals(
            mapOf(1 to "https://www.jagonews24.com/national/news/1177973", 2 to "https://www.dhakatimes24.com/2026/11/10/416870"),
            GoogleNewsDecoder.parseBatchReply(reply)
        )
        assertTrue(GoogleNewsDecoder.parseBatchReply("<html>sorry</html>").isEmpty())
    }

    @Test
    fun `facebook page is read from the post address`() {
        assertEquals("BorderGuardBangladeshOfficial", FacebookPages.pageOf("https://www.facebook.com/BorderGuardBangladeshOfficial/posts/abc/123"))
        assertEquals("dbcnews.tv", FacebookPages.pageOf("https://m.facebook.com/dbcnews.tv/videos/1654"))
        assertEquals("Some Name", FacebookPages.pageOf("https://www.facebook.com/people/Some-Name/100012345/"))
        assertNull(FacebookPages.pageOf("https://www.facebook.com/story.php?story_fbid=1&id=2"))
        assertNull(FacebookPages.pageOf("https://www.facebook.com/watch/?v=123"))
        assertNull(FacebookPages.pageOf("https://www.prothomalo.com/bangladesh/x"))
    }

    @Test
    fun `a refetch keeps the decoded link and the facebook page name`() {
        val google = "https://news.google.com/rss/articles/CBMi"
        assertEquals("https://paper.com/a", keptOriginalLink("https://paper.com/a", google))
        assertEquals(google, keptOriginalLink(null, google))
        assertEquals("https://paper.com/b", keptOriginalLink("https://paper.com/a", "https://paper.com/b"))
        assertEquals("Facebook • DailyProthomAlo", keptSource("Facebook • DailyProthomAlo", "Facebook"))
        assertEquals("Facebook", keptSource("Facebook • Facebook", "Facebook"))
        assertEquals("Google • Jugantor", keptSource("Google • Old", "Google • Jugantor"))
    }

    // ---- custom feeds, source health ----

    @Test
    fun `undated items are dropped from a feed that normally dates its items`() {
        val remote = NewsRemoteDataSource(okhttp3.OkHttpClient())
        val mostlyDated = listOf(article("a", 5L), article("b", 6L), article("c", 0L))
        assertEquals(listOf("a", "b"), remote.datedOrStamped(mostlyDated, now).map { it.link })
        val undated = listOf(article("x", 0L), article("y", 0L))
        assertEquals(listOf(now, now), remote.datedOrStamped(undated, now).map { it.timestamp })
    }

    @Test
    fun `a search engine with only old items is flagged as stale, a failing feed as down`() {
        SourceHealth.startRound(now - 1000)
        SourceHealth.record(SourceHealth.BING, "Bing News", SourceHealth.Kind.SEARCH, null, listOf(article("a", now - 9 * hour)), now)
        SourceHealth.record(SourceHealth.GOOGLE, "Google News", SourceHealth.Kind.SEARCH, null, listOf(article("b", now - 60_000)), now)
        SourceHealth.record("feed:x", "X", SourceHealth.Kind.NEWSPAPER, "HTTP 403", emptyList(), now)
        SourceHealth.record("feed:y", "Y", SourceHealth.Kind.NEWSPAPER, null, listOf(article("c", now - hour)), now)
        SourceHealth.record("feed:y", "Y", SourceHealth.Kind.NEWSPAPER, "timeout", emptyList(), now)
        val all = SourceHealth.statuses.value
        assertEquals(SourceHealth.Level.STALE, SourceHealth.levelOf(all.getValue(SourceHealth.BING), now))
        assertEquals(SourceHealth.Level.OK, SourceHealth.levelOf(all.getValue(SourceHealth.GOOGLE), now))
        assertEquals(SourceHealth.Level.DOWN, SourceHealth.levelOf(all.getValue("feed:x"), now))
        // one hiccup after a success is not yet a problem
        assertEquals(SourceHealth.Level.OK, SourceHealth.levelOf(all.getValue("feed:y"), now))
        assertFalse(all.getValue("feed:y").lastError == null)
    }
}
