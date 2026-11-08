package com.example.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    /** Videos published after [sinceMillis], newest first. */
    @Query("SELECT * FROM videos WHERE publishedMillis >= :sinceMillis ORDER BY publishedMillis DESC LIMIT 1500")
    fun observeSince(sinceMillis: Long): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE videoId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<VideoEntity>

    @Upsert
    suspend fun upsertAll(videos: List<VideoEntity>)

    /** Stores [videos], merging each with any stored copy (see [VideoEntity.mergedWith]). */
    @Transaction
    suspend fun mergeAll(videos: List<VideoEntity>) {
        if (videos.isEmpty()) return
        val stored = videos.map { it.videoId }.chunked(500).flatMap { getByIds(it) }.associateBy { it.videoId }
        upsertAll(videos.map { fresh -> stored[fresh.videoId]?.mergedWith(fresh) ?: fresh })
    }

    @Query("DELETE FROM videos WHERE publishedMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    /** When the last video was stored (0 if never): the cache's freshness. */
    @Query("SELECT COALESCE(MAX(fetchedAt), 0) FROM videos")
    suspend fun lastFetchedAt(): Long

    @Query("DELETE FROM videos")
    suspend fun clear()
}
