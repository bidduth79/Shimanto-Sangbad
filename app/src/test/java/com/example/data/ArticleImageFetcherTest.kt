package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticleImageFetcherTest {
    private val page = "https://www.example.com/bangladesh/2026/11/08/story"

    private fun extract(head: String) = ArticleImageFetcher.extractShareImage(head, page)

    @Test
    fun `og image is found whatever the attribute order and quotes`() {
        assertEquals("https://cdn.example.com/a.jpg", extract("""<meta property="og:image" content="https://cdn.example.com/a.jpg">"""))
        assertEquals("https://cdn.example.com/b.jpg", extract("""<meta content='https://cdn.example.com/b.jpg' property='og:image' />"""))
    }

    @Test
    fun `secure og image wins, twitter image is the fallback`() {
        val both = """<meta name="twitter:image" content="https://x.example.com/t.jpg">
            <meta property="og:image:secure_url" content="https://cdn.example.com/s.jpg">"""
        assertEquals("https://cdn.example.com/s.jpg", extract(both))
        assertEquals("https://x.example.com/t.jpg", extract("""<meta name="twitter:image" content="https://x.example.com/t.jpg">"""))
    }

    @Test
    fun `relative and escaped urls are made absolute`() {
        assertEquals(
            "https://www.example.com/uploads/p.jpg?w=800&h=450",
            extract("""<meta property="og:image" content="/uploads/p.jpg?w=800&amp;h=450">""")
        )
    }

    @Test
    fun `site logos are not used as article pictures`() {
        assertNull(extract("""<meta property="og:image" content="https://www.example.com/images/site-logo.png">"""))
        // a real photo that merely sits in a folder called "default"
        assertEquals(
            "https://cdn.example.com/default/2026/bgb-patrol.jpg",
            extract("""<meta property="og:image" content="https://cdn.example.com/default/2026/bgb-patrol.jpg">""")
        )
    }

    @Test
    fun `no share image means null`() {
        assertNull(extract("<html><head><title>No picture</title></head>"))
    }

    @Test
    fun `daily star share image is found`() {
        val head = """<meta name="description" content="under the cover of darkness and dense fog" /> <meta property="og:image" content="https://www.thedailystar.net/sites/default/files/styles/social_share/public/2026-11/moulvibazar%20%281%29%20%281%29.jpg" /> <meta property="og:image:url" content="https://www.thedailystar.net/sites/default/files/styles/social_share/public/2026-11/moulvibazar%20%281%29%20%281%29.jpg" />"""
        assertEquals(
            "https://www.thedailystar.net/sites/default/files/styles/social_share/public/2026-11/moulvibazar%20%281%29%20%281%29.jpg",
            ArticleImageFetcher.extractShareImage(head, "https://www.thedailystar.net/news/x")
        )
    }

    @Test
    fun `google own addresses are never taken for the article`() {
        val decoder = GoogleNewsDecoder(okhttp3.OkHttpClient())
        org.junit.Assert.assertTrue(decoder.isGoogleOwned("https://www.google.com/recaptcha/enterprise.js"))
        org.junit.Assert.assertTrue(decoder.isGoogleOwned("https://fonts.gstatic.com/x"))
        org.junit.Assert.assertFalse(decoder.isGoogleOwned("https://www.thedailystar.net/news/x"))
    }
}
