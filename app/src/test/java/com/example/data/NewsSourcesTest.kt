package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsSourcesTest {

    @Test
    fun titleMatches_bengaliSubstring() {
        assertTrue(NewsSources.titleMatches("সীমান্তে বিজিবির টহল জোরদার", "বিজিবি"))
    }

    @Test
    fun titleMatches_englishVariantNeedsEveryWord() {
        assertTrue(NewsSources.titleMatches("Border tension rises, Bangladesh says", "Bangladesh border"))
        assertFalse(NewsSources.titleMatches("Bangladesh cricket team wins", "Bangladesh border"))
    }

    @Test
    fun titleMatches_isCaseInsensitive() {
        assertTrue(NewsSources.titleMatches("BGB seizes contraband", "bgb"))
    }

    @Test
    fun titleMatches_blankQueryNeverMatches() {
        assertFalse(NewsSources.titleMatches("Any headline", "  "))
    }

    // 2026-11-08 01:14:21 UTC
    private val expectedMillis = 1_794_100_461_000L

    @Test
    fun parseFeedTimestamp_standardRfc822() {
        assertEquals(expectedMillis, NewsRemoteDataSource(okhttp3.OkHttpClient()).parseFeedTimestamp("Sun, 08 Nov 2026 07:14:21 +0600"))
    }

    @Test
    fun parseFeedTimestamp_commaAfterYear() {
        assertEquals(expectedMillis, NewsRemoteDataSource(okhttp3.OkHttpClient()).parseFeedTimestamp("Sun, 08 Nov 2026, 07:14:21 +0600"))
        assertEquals(expectedMillis, NewsRemoteDataSource(okhttp3.OkHttpClient()).parseFeedTimestamp("Sunday, 8 November 2026, 07:14:21 +0600"))
    }

    @Test
    fun parseFeedTimestamp_twoDigitYear() {
        assertEquals(expectedMillis, NewsRemoteDataSource(okhttp3.OkHttpClient()).parseFeedTimestamp("Sun, 08 Nov 26 07:14:21 +0600"))
    }

    @Test
    fun builtInSources_haveNoDuplicates() {
        val urls = NewsSources.newspaperFeeds.map { it.url }
        assertTrue(urls.size == urls.toSet().size)
        val channels = NewsSources.youtubeNewsChannels
        assertTrue(channels.size == channels.toSet().size)
        assertTrue(channels.all { it.startsWith("UC") && it.length == 24 })
    }
}
