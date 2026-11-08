package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL
import com.example.data.NewsArticle

/**
 * Migration policy:
 *  - Versions 1–4 shipped before schemas were exported, so they cannot be migrated and are rebuilt.
 *  - From version 5 on, every schema change MUST add a Migration (or an @AutoMigration) and the
 *    exported JSON in app/schemas, so bookmarks survive app updates. There is deliberately no
 *    blanket destructive fallback: a missing migration fails loudly in testing instead of wiping data.
 */
@Database(entities = [NewsArticle::class, VideoEntity::class], version = 7, exportSchema = true)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun videoDao(): VideoDao

    companion object {
        const val NAME = "news_database"

        fun build(context: Context): NewsDatabase =
            Room.databaseBuilder(context, NewsDatabase::class.java, NAME)
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4)
                .build()

        /** v6: stores each article's country tab so it is computed once, not on every screen update. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            private val sql = "ALTER TABLE news_articles ADD COLUMN region TEXT NOT NULL DEFAULT ''"
            override fun migrate(db: SupportSQLiteDatabase) = db.execSQL(sql)
            override fun migrate(connection: SQLiteConnection) = connection.execSQL(sql)
        }
    
        /** v7: caches news videos so the video tab opens instantly. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            private val statements = listOf(
                "CREATE TABLE IF NOT EXISTS `videos` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, `channelTitle` TEXT NOT NULL, `channelId` TEXT, " +
                    "`channelCountry` TEXT, `publishedAt` TEXT, `publishedMillis` INTEGER NOT NULL, " +
                    "`timeIsEstimate` INTEGER NOT NULL, `thumbnailUrl` TEXT, `fetchedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`videoId`))",
                "CREATE INDEX IF NOT EXISTS `index_videos_publishedMillis` ON `videos` (`publishedMillis`)"
            )
            override fun migrate(db: SupportSQLiteDatabase) = statements.forEach { db.execSQL(it) }
            override fun migrate(connection: SQLiteConnection) = statements.forEach { connection.execSQL(it) }
        }
    }
}
