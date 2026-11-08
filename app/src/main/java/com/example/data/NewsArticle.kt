package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One news item. `category` records which fetch found it (bgb, indian, myanmar, international,
 * facebook, rss); `region` is the country tab it is shown under, decided from its publisher.
 */
@Entity(
    tableName = "news_articles",
    indices = [
        Index(value = ["category", "timestamp"]),
        Index(value = ["isSaved"]),
        Index(value = ["timestamp"])
    ]
)
data class NewsArticle(
    @PrimaryKey val link: String,
    val title: String,
    val pubDate: String,
    val source: String,
    val timestamp: Long,
    val category: String = "",
    val isSaved: Boolean = false,
    val matchedKeyword: String = "",
    val imageUrl: String? = null,
    val originalLink: String? = null,
    /** [NewsRegion] name of the publisher's country, computed once when stored (added in DB v6). */
    @ColumnInfo(defaultValue = "")
    val region: String = ""
)
