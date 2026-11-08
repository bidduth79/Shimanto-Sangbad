package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SameStoryImagesTest {
    private fun article(link: String, title: String, image: String? = null) =
        NewsArticle(link = link, title = title, pubDate = "", source = "s", timestamp = 1, imageUrl = image)

    @Test
    fun `same story from another source lends its picture`() {
        val google = article("g", "সাতক্ষীরা সীমান্তে বিজিবির অভিযানে প্রায় ৯ লাখ টাকার ভারতীয় পণ্য জব্দ - Bd24live")
        val bing = article("b", "সাতক্ষীরা সীমান্তে বিজিবির অভিযানে ৯ লাখ টাকার ভারতীয় পণ্য জব্দ", "https://img/x.jpg")

        assertEquals(mapOf("g" to "https://img/x.jpg"), SameStoryImages.borrow(listOf(google), listOf(bing)))
    }

    @Test
    fun `different stories do not share pictures`() {
        val a = article("a", "লালমনিরহাট সীমান্তে পুশইন চেষ্টা রুখে দিল বিজিবি")
        val b = article("b", "গাজীপুরে শ্রমিক বিক্ষোভে মাঠে ১৩ প্লাটুন বিজিবি", "https://img/y.jpg")

        assertTrue(SameStoryImages.borrow(listOf(a), listOf(b)).isEmpty())
    }
}
