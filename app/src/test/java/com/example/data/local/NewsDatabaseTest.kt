package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.NewsArticle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewsDatabaseTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(),
        NewsDatabase::class.java
    ).allowMainThreadQueries().build()
    private val dao = db.newsDao()

    @After
    fun tearDown() = db.close()

    private fun article(link: String, timestamp: Long = 1_000L, saved: Boolean = false) =
        NewsArticle(link = link, title = "t-$link", pubDate = "", source = "s", timestamp = timestamp, category = "bgb", isSaved = saved)

    @Test
    fun `re-fetching an article keeps its bookmark`() = runBlocking {
        dao.insertAll(listOf(article("a", saved = true)))

        dao.upsertKeepingBookmarks(listOf(article("a").copy(title = "updated")))

        val stored = dao.getByLink("a")!!
        assertTrue(stored.isSaved)
        assertEquals("updated", stored.title)
    }

    @Test
    fun `observe returns only rows newer than the cut-off`() = runBlocking {
        dao.insertAll(listOf(article("old", timestamp = 100), article("new", timestamp = 5_000)))

        val rows = dao.observeByCategories(listOf("bgb"), since = 1_000).first()

        assertEquals(listOf("new"), rows.map { it.link })
    }

    @Test
    fun `clearing the cache keeps bookmarks`() = runBlocking {
        dao.insertAll(listOf(article("keep", saved = true), article("drop")))

        dao.deleteAllUnsaved()

        assertEquals(listOf("keep"), dao.observeSaved().first().map { it.link })
        assertEquals(null, dao.getByLink("drop"))
    }

    @Test
    fun `migrations 5 to 7 keep bookmarks, add an empty region and the video table`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        createVersion5Database(context, TEST_DB) { sqlite ->
            sqlite.execSQL(
                "INSERT INTO news_articles (link, title, pubDate, source, timestamp, category, isSaved, matchedKeyword) " +
                    "VALUES ('saved-link', 'Saved story', '', 'src', 1, 'bgb', 1, '')"
            )
        }

        // Opening runs MIGRATION_5_6 and MIGRATION_6_7, then Room checks the result matches the current schema
        val migrated = Room.databaseBuilder(context, NewsDatabase::class.java, TEST_DB)
            .addMigrations(NewsDatabase.MIGRATION_5_6, NewsDatabase.MIGRATION_6_7)
            .allowMainThreadQueries()
            .build()
        val row = migrated.newsDao().getByLink("saved-link")!!
        val videos = migrated.videoDao().lastFetchedAt()
        migrated.close()
        assertEquals(0L, videos)

        assertTrue(row.isSaved)
        assertEquals("", row.region)
    }

    @Test
    fun `a fresh copy of a video keeps the exact time and the known channel`() = runBlocking {
        val videos = db.videoDao()
        val exact = VideoEntity(
            videoId = "v1", title = "BGB", description = "", channelTitle = "SOMOY TV", channelId = "UC_somoy",
            channelCountry = "BD", publishedAt = "2026-11-08T10:00:00Z", publishedMillis = 100L,
            timeIsEstimate = false, thumbnailUrl = null, fetchedAt = 1L
        )
        videos.mergeAll(listOf(exact))
        // the same video again from web search: estimated time, no channel ID or country
        videos.mergeAll(listOf(exact.copy(channelId = null, channelCountry = null, publishedMillis = 90L, timeIsEstimate = true, fetchedAt = 2L)))

        val stored = videos.getByIds(listOf("v1")).single()
        assertEquals(100L, stored.publishedMillis)
        assertEquals("UC_somoy", stored.channelId)
        assertEquals("BD", stored.channelCountry)
        assertEquals(2L, videos.lastFetchedAt())
    }

    /** Builds a database exactly as version 5 shipped it, from the exported schema file. */
    private fun createVersion5Database(context: Context, name: String, seed: (android.database.sqlite.SQLiteDatabase) -> Unit) {
        val schema = org.json.JSONObject(java.io.File("schemas/com.example.data.local.NewsDatabase/5.json").readText())
            .getJSONObject("database")
        context.deleteDatabase(name)
        val sqlite = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        val entity = schema.getJSONArray("entities").getJSONObject(0)
        sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
        val indices = entity.optJSONArray("indices")
        for (i in 0 until (indices?.length() ?: 0)) {
            sqlite.execSQL(indices!!.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) sqlite.execSQL(setup.getString(i))
        seed(sqlite)
        sqlite.version = 5
        sqlite.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
