package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.provider.Settings
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseManager(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()

    private val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"


    suspend fun syncSavedNews(article: NewsArticle, isSaved: Boolean) {
        try {
            val documentRef = firestore.collection("users")
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

    suspend fun getSavedNewsLinks(): List<String> {
        return try {
            val snapshot = firestore.collection("users")
                .document(deviceId)
                .collection("saved_news")
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.getString("link") }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching saved links", e)
            emptyList()
        }
    }

    suspend fun restoreSavedNews(): List<NewsArticle> {
        return try {
            val snapshot = firestore.collection("users")
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
