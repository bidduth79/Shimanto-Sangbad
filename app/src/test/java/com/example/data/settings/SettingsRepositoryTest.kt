package com.example.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Fresh files per test: DataStore allows only one active instance per file
    private fun newRepository(): SettingsRepository {
        val id = UUID.randomUUID().toString()
        return SettingsRepository(
            SettingsRepository.createSettingsStore(context, scope, "settings-$id"),
            SettingsRepository.createSecretStore(context, scope, "secret-$id")
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        context.getSharedPreferences("keywords_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("secret_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `settings saved by the SharedPreferences version are carried over`() = runBlocking {
        context.getSharedPreferences("keywords_prefs", Context.MODE_PRIVATE).edit()
            .putStringSet("keywords_set", setOf("টেকনাফ", "BGB"))
            .putString("custom_rss_feeds_json", """[{"title":"Feed","url":"https://example.com/rss"}]""")
            .putString("visited_links_json", """["https://example.com/a"]""")
            .commit()

        val settings = newRepository().settings.first()

        assertEquals(setOf("টেকনাফ", "BGB"), settings.keywords.toSet())
        assertEquals("https://example.com/rss", settings.rssFeeds.single().url)
        assertTrue("https://example.com/a" in settings.visitedLinks)
    }

    @Test
    fun `api key moves from the backed-up file into the secret store`() = runBlocking {
        val legacy = context.getSharedPreferences("keywords_prefs", Context.MODE_PRIVATE)
        legacy.edit().putString("youtube_api_key", "legacy-key").commit()

        assertEquals("legacy-key", newRepository().youtubeApiKey.first())
        assertFalse(legacy.contains("youtube_api_key"))
    }

    @Test
    fun `defaults apply when nothing was ever saved`() = runBlocking {
        assertEquals(SettingsRepository.DEFAULT_KEYWORDS, newRepository().settings.first().keywords)
    }

    @Test
    fun `visited links drop the oldest entries when trimmed`() = runBlocking {
        val repo = newRepository()
        repeat(1_501) { repo.markLinkVisited("https://example.com/$it") }

        val visited = repo.settings.first().visitedLinks
        assertEquals(1_200, visited.size)
        assertFalse("https://example.com/0" in visited)
        assertTrue("https://example.com/1500" in visited)
    }

    @Test
    fun `rss feed urls are normalized and deduplicated`() = runBlocking {
        val repo = newRepository()
        repo.addRssFeed("", "example.com/feed")
        repo.addRssFeed("Again", "https://example.com/feed")

        val feed = repo.settings.first().rssFeeds.single()
        assertEquals("https://example.com/feed", feed.url)
        assertEquals("example.com", feed.title)
    }

    @Test
    fun `keywords can be added and removed`() = runBlocking {
        val repo = newRepository()
        repo.addKeyword("  টেকনাফ  ")
        repo.removeKeyword("BGB")

        val keywords = repo.settings.first().keywords
        assertTrue("টেকনাফ" in keywords)
        assertFalse("BGB" in keywords)
    }
}
