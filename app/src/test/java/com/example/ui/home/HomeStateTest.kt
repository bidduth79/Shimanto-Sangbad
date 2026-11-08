package com.example.ui.home

import com.example.data.NewsArticle
import com.example.data.SearchMedium
import com.example.ui.groupNewsByRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {

    private fun article(link: String, title: String, source: String, region: String, timestamp: Long) =
        NewsArticle(link = link, title = title, pubDate = "", source = source, timestamp = timestamp, region = region)

    private val bdPaper = article("bd", "সীমান্তে টহল", "পত্রিকা • ইত্তেফাক", "BANGLADESH", 3)
    private val inGoogle = article("in", "BSF patrol near Bangladesh border", "Google • The Hindu", "INDIA", 2)
    private val bdBing = article("bd2", "BGB seizes yaba in Teknaf", "Bing • Dhaka Tribune", "BANGLADESH", 1)

    private fun inputs(visited: Set<String> = emptySet()) = HomeInputs(
        regional = groupNewsByRegion(listOf(bdPaper, inGoogle, bdBing)),
        facebookNews = emptyList(),
        customFeedNews = emptyList(),
        savedNews = emptyList(),
        keywords = listOf("BGB"),
        rssFeeds = emptyList(),
        visitedLinks = visited
    )

    @Test
    fun `all tab shows every region newest first with region labels`() {
        val state = buildHomeState(inputs(), HomeSelection(), isRefreshing = false)

        assertEquals(listOf("bd", "in", "bd2"), state.visibleArticles.map { it.link })
        assertEquals(3, state.tabCounts.all)
        assertEquals(2, state.tabCounts.bangladesh)
        assertEquals(1, state.tabCounts.india)
        assertTrue(state.showRegionLabels)
    }

    @Test
    fun `country tab shows only that country without labels`() {
        val state = buildHomeState(inputs(), HomeSelection(tab = HomeTab.INDIA), isRefreshing = false)

        assertEquals(listOf("in"), state.visibleArticles.map { it.link })
        assertFalse(state.showRegionLabels)
    }

    @Test
    fun `source chip and unread filter narrow the list`() {
        val newspaper = buildHomeState(inputs(), HomeSelection(filter = SearchMedium.NEWSPAPER), false)
        assertEquals(listOf("bd"), newspaper.visibleArticles.map { it.link })
        assertEquals(1, newspaper.filterCounts.newspaper)

        val unread = buildHomeState(inputs(visited = setOf("bd")), HomeSelection(filter = HomeFilter.UNREAD), false)
        assertEquals(listOf("in", "bd2"), unread.visibleArticles.map { it.link })
        assertEquals(1, unread.filterCounts.read)
    }

    @Test
    fun `search matches title or source and hides the hero carousel`() {
        val state = buildHomeState(inputs(), HomeSelection(searchQuery = "hindu"), false)

        assertEquals(listOf("in"), state.visibleArticles.map { it.link })
        assertTrue(state.heroArticles.isEmpty())
    }

    @Test
    fun `skeleton only while the very first refresh runs with nothing cached`() {
        val empty = inputs().copy(regional = groupNewsByRegion(emptyList()))
        assertTrue(buildHomeState(empty, HomeSelection(), isRefreshing = true).isLoading)
        assertFalse(buildHomeState(inputs(), HomeSelection(), isRefreshing = true).isLoading)
    }

    @Test
    fun `keyword menu filters by headline rules and counts per keyword`() {
        // "BGB seizes yaba in Teknaf" is about বিজিবি through its English alias (the place gives the context)
        val tagged = inputs().copy(keywords = listOf("বিজিবি", "BSF"))
        val state = buildHomeState(tagged, HomeSelection(keyword = "বিজিবি"), isRefreshing = false)

        assertEquals(listOf("bd2"), state.visibleArticles.map { it.link })
        assertEquals(1, state.keywordCounts["বিজিবি"])
        assertEquals(1, state.keywordCounts["BSF"])
    }

    @Test
    fun `hour filter keeps only recent stories and updates tab counts`() {
        val hour = 3_600_000L
        val now = 10 * hour
        val recent = inputs().copy(
            regional = groupNewsByRegion(
                listOf(
                    bdPaper.copy(timestamp = now - hour / 2),      // 30 minutes ago
                    inGoogle.copy(timestamp = now - 2 * hour),     // 2 hours ago
                    bdBing.copy(timestamp = now - 5 * hour)        // 5 hours ago
                )
            )
        )

        val lastHour = buildHomeState(recent, HomeSelection(maxAgeHours = 1), false, now = now)
        assertEquals(listOf("bd"), lastHour.visibleArticles.map { it.link })
        assertEquals(1, lastHour.tabCounts.all)
        assertEquals(0, lastHour.tabCounts.india)

        val threeHours = buildHomeState(recent, HomeSelection(maxAgeHours = 3), false, now = now)
        assertEquals(listOf("bd", "in"), threeHours.visibleArticles.map { it.link })

        val everything = buildHomeState(recent, HomeSelection(maxAgeHours = null), false, now = now)
        assertEquals(3, everything.visibleArticles.size)
    }

    @Test
    fun `country tab chips split TV, national and local outlets and can hide read stories`() {
        val bd = listOf(
            article("tv", "সীমান্তে বিজিবির টহল", "Google • Somoy TV", "BANGLADESH", 5),
            article("nat", "বিজিবির অভিযানে মাদক জব্দ", "পত্রিকা • প্রথম আলো", "BANGLADESH", 4),
            article("loc", "চুয়াডাঙ্গা সীমান্তে বিজিবি", "পত্রিকা • দৈনিক মাথাভাঙ্গা", "BANGLADESH", 3)
        )
        val inputs = inputs(visited = setOf("nat")).copy(regional = groupNewsByRegion(bd), keywords = listOf("বিজিবি"))

        val all = buildHomeState(inputs, HomeSelection(tab = HomeTab.BANGLADESH), false)
        assertEquals(1, all.filterCounts.tv)
        assertEquals(1, all.filterCounts.national)
        assertEquals(1, all.filterCounts.local)

        val local = buildHomeState(inputs, HomeSelection(tab = HomeTab.BANGLADESH, filter = HomeFilter.LOCAL), false)
        assertEquals(listOf("loc"), local.visibleArticles.map { it.link })

        val unread = buildHomeState(inputs, HomeSelection(tab = HomeTab.BANGLADESH, onlyUnread = true), false)
        assertEquals(listOf("tv", "loc"), unread.visibleArticles.map { it.link })
    }
}
