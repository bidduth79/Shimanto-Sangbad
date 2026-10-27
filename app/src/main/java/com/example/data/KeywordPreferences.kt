package com.example.data

import android.content.Context
import android.content.SharedPreferences

class KeywordPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("keywords_prefs", Context.MODE_PRIVATE)
    
    fun getKeywords(): List<String> {
        val keywordsSet = prefs.getStringSet("keywords_set", null)
        return if (keywordsSet != null && keywordsSet.isNotEmpty()) {
            keywordsSet.toList()
        } else {
            listOf("বিজিবি", "বর্ডার গার্ড বাংলাদেশ", "BGB", "বিএসএফ", "BSF", "সীমান্ত", "বাংলাদেশ ভারত সীমান্ত", "সীমান্ত সংবাদ")
        }
    }
    
    fun removeKeyword(keyword: String) {
        val current = getKeywords().toMutableSet()
        current.remove(keyword)
        prefs.edit().putStringSet("keywords_set", current).apply()
    }

    fun addKeyword(keyword: String) {
        val current = getKeywords().toMutableSet()
        current.add(keyword)
        prefs.edit().putStringSet("keywords_set", current).apply()
    }




    fun getYoutubeApiKey(): String {
        val key = prefs.getString("youtube_api_key", null)
        if (!key.isNullOrBlank()) return key
        val buildKey = com.example.BuildConfig.YOUTUBE_API_KEY
        return if (buildKey != "MY_YOUTUBE_API_KEY") buildKey else ""
    }

    fun saveYoutubeApiKey(apiKey: String) {
        prefs.edit().putString("youtube_api_key", apiKey.trim()).apply()
    }

    fun getYoutubeChannelId(): String {
        val saved = prefs.getString("youtube_channel_id", null) ?: return ""
        // Clear legacy placeholder channel IDs if they were previously stored
        if (saved.contains("UCxAI1M5Wv1rE4XbT1gNnt7g")) {
            prefs.edit().remove("youtube_channel_id").apply()
            return ""
        }
        return saved.trim()
    }

    fun saveYoutubeChannelId(channelId: String) {
        prefs.edit().putString("youtube_channel_id", channelId.trim()).apply()
    }

    fun getVisitedLinks(): Set<String> {
        return prefs.getStringSet("visited_links_set", emptySet()) ?: emptySet()
    }

    fun markLinkVisited(link: String) {
        val current = getVisitedLinks().toMutableSet()
        current.add(link)
        // Keep max 500 links to manage storage
        if (current.size > 500) {
            val trimmed = current.toList().takeLast(400).toSet()
            prefs.edit().putStringSet("visited_links_set", trimmed).apply()
        } else {
            prefs.edit().putStringSet("visited_links_set", current).apply()
        }
    }

    fun getRssFeeds(): List<RssFeedItem> {
        val raw = prefs.getString("custom_rss_feeds_json", null) ?: return emptyList()
        val list = mutableListOf<RssFeedItem>()
        try {
            val arr = org.json.JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val title = obj.optString("title", "").trim()
                val url = obj.optString("url", "").trim()
                if (url.isNotBlank()) {
                    list.add(RssFeedItem(title = title.ifBlank { extractDomainName(url) }, url = url))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun addRssFeed(title: String, rawUrl: String) {
        var normalizedUrl = rawUrl.trim()
        if (normalizedUrl.isBlank()) return
        if (!normalizedUrl.startsWith("http://", ignoreCase = true) &&
            !normalizedUrl.startsWith("https://", ignoreCase = true)
        ) {
            normalizedUrl = "https://$normalizedUrl"
        }
        val current = getRssFeeds().toMutableList()
        if (current.any { it.url.equals(normalizedUrl, ignoreCase = true) }) return
        val finalTitle = title.trim().ifBlank { extractDomainName(normalizedUrl) }
        current.add(RssFeedItem(title = finalTitle, url = normalizedUrl))
        saveRssFeedsList(current)
    }

    fun removeRssFeed(url: String) {
        val current = getRssFeeds().filterNot { it.url.equals(url.trim(), ignoreCase = true) }
        saveRssFeedsList(current)
    }

    private fun saveRssFeedsList(feeds: List<RssFeedItem>) {
        val arr = org.json.JSONArray()
        for (feed in feeds) {
            val obj = org.json.JSONObject()
            obj.put("title", feed.title)
            obj.put("url", feed.url)
            arr.put(obj)
        }
        prefs.edit().putString("custom_rss_feeds_json", arr.toString()).apply()
    }

    private fun extractDomainName(url: String): String {
        return try {
            val host = java.net.URI(url).host ?: url
            host.removePrefix("www.").removePrefix("feeds.").removePrefix("rss.")
        } catch (_: Exception) {
            url
        }
    }
}

data class RssFeedItem(
    val title: String,
    val url: String
)

