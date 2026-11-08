package com.example.data

import com.google.mlkit.nl.translate.TranslateLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NewFeaturesTest {

    private fun article(link: String, title: String, timestamp: Long, image: String? = null) =
        NewsArticle(link = link, title = title, pubDate = "", source = "পত্রিকা • test", timestamp = timestamp, imageUrl = image)

    @Test
    fun `alerts need an alert word and the main topic, once per story`() {
        val now = 10_000_000L
        val recent = listOf(
            article("a", "চুয়াডাঙ্গা সীমান্তে বিএসএফের গুলিতে যুবক নিহত", now - 1_000),
            article("b", "রাজধানীতে ছিনতাইকারীর গুলিতে আহত ১", now - 2_000),          // গুলি, but not border news
            article("c", "সীমান্তে বিজিবির টহল জোরদার", now - 3_000),                   // border, no alert word
            article("d", "লালমনিরহাট সীমান্তে পুশইনের চেষ্টা ব্যর্থ", now - 4_000)
        )
        val picked = NewsAlerts.pickAlerts(
            recent = recent,
            alertKeywords = listOf("গুলি", "পুশইন"),
            topicKeywords = listOf("সীমান্ত", "বিজিবি", "বিএসএফ"),
            alreadyNotified = setOf("d"),
            now = now
        )
        assertEquals(listOf("a"), picked.map { it.link })
    }

    @Test
    fun `reports of the same event become one card with the others behind it`() {
        val t = 5_000_000L
        val clusters = StoryClusters.cluster(
            listOf(
                article("1", "টেকনাফ সীমান্তে ৫০ হাজার ইয়াবা জব্দ করেছে বিজিবি", t),
                article("2", "টেকনাফ সীমান্তে ৫০ হাজার ইয়াবা জব্দ করল বিজিবি", t - 60_000, image = "img"),
                article("3", "সাতক্ষীরায় বিজিবির অভিযানে ভারতীয় শাড়ি উদ্ধার", t - 120_000)
            )
        )
        assertEquals(2, clusters.size)
        // the report with a picture leads its event
        assertEquals("2", clusters[0].lead.link)
        assertEquals(listOf("1"), clusters[0].others.map { it.link })
        assertEquals("3", clusters[1].lead.link)
    }

    @Test
    fun `only Hindi and English headlines are translated`() {
        assertEquals(TranslateLanguage.HINDI, HeadlineTranslator.sourceLanguageOf("बीएसएफ ने घुसपैठिए को पकड़ा"))
        assertEquals(TranslateLanguage.ENGLISH, HeadlineTranslator.sourceLanguageOf("BSF foils smuggling bid at Bangladesh border"))
        assertNull(HeadlineTranslator.sourceLanguageOf("সীমান্তে বিজিবির টহল"))
        assertNull(HeadlineTranslator.sourceLanguageOf("BGB টহল"))
    }
}
