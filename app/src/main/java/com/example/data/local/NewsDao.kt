package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.NewsArticle
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles ORDER BY timestamp DESC")
    suspend fun getAllNews(): List<NewsArticle>
    
    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getNewsByCategory(category: String): List<NewsArticle>

    @Query("SELECT * FROM news_articles WHERE isSaved = 1 ORDER BY timestamp DESC")
    suspend fun getSavedNews(): List<NewsArticle>

    @Query("SELECT * FROM news_articles WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    suspend fun getNewsByTimeRange(startTime: Long, endTime: Long): List<NewsArticle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<NewsArticle>)

    @Query("UPDATE news_articles SET isSaved = :isSaved WHERE link = :link")
    suspend fun updateSavedStatus(link: String, isSaved: Boolean)

    @Query("DELETE FROM news_articles WHERE timestamp < :olderThanMillis AND isSaved = 0")
    suspend fun deleteOldNews(olderThanMillis: Long)
}
