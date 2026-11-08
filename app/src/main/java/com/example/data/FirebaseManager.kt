package com.example.data

import android.content.Context
import android.util.Log
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs up bookmarks to Firestore, keyed by device. Does nothing (instead of crashing) when the
 * project has no Firebase configuration (google-services.json), which is the case today.
 */
@Singleton
class FirebaseManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val firestore: FirebaseFirestore? by lazy {
        runCatching { FirebaseFirestore.getInstance() }
            .onFailure { Log.w("FirebaseManager", "Firebase not configured; cloud backup disabled") }
            .getOrNull()
    }

    private val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"


    suspend fun syncSavedNews(article: NewsArticle, isSaved: Boolean) {
        try {
            val db = firestore ?: return
            val documentRef = db.collection("users")
                .document(deviceId)
                .collection("saved_news")
                // Encode the link to avoid invalid document paths
                .document(android.util.Base64.encodeToString(article.link.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP))

            if (isSaved) {
                
                val articleMap = hashMapOf(
                    "link" to article.link,
                    "title" to article.title,
                    "pubDate" to article.pubDate,
                    "source" to article.source,
                    "timestamp" to article.timestamp,
                    "category" to article.category,
                    "isSaved" to article.isSaved,
                    "matchedKeyword" to article.matchedKeyword,
                    "imageUrl" to article.imageUrl,
                    "originalLink" to article.originalLink
                )
                documentRef.set(articleMap).await()

                Log.d("FirebaseManager", "Article saved to Firestore: ${article.title}")
            } else {
                documentRef.delete().await()
                Log.d("FirebaseManager", "Article removed from Firestore: ${article.title}")
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error syncing with Firestore", e)
        }
    }

    suspend fun restoreSavedNews(): List<NewsArticle> {
        val db = firestore ?: return emptyList()
        return try {
            val snapshot = db.collection("users")
                .document(deviceId)
                .collection("saved_news")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                val link = doc.getString("link") ?: return@mapNotNull null
                val title = doc.getString("title") ?: ""
                val pubDate = doc.getString("pubDate") ?: ""
                val source = doc.getString("source") ?: ""
                val timestamp = doc.getLong("timestamp") ?: 0L
                val category = doc.getString("category") ?: ""
                val matchedKeyword = doc.getString("matchedKeyword") ?: ""
                val imageUrl = doc.getString("imageUrl")
                val originalLink = doc.getString("originalLink")

                NewsArticle(
                    link = link,
                    title = title,
                    pubDate = pubDate,
                    source = source,
                    timestamp = timestamp,
                    category = category,
                    isSaved = true,
                    matchedKeyword = matchedKeyword,
                    imageUrl = imageUrl,
                    originalLink = originalLink
                )
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error restoring news: ${e.message}")
            emptyList()
        }
    }
}
