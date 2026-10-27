package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.net.URLEncoder

object GoogleNewsDecoder {
    private val httpClient = SharedHttpClient.client
    private val sigRegex = "data-n-a-sg=\"([^\"]+)\"".toRegex()
    private val tsRegex = "data-n-a-ts=\"([^\"]+)\"".toRegex()
    private val articleIdRegex = "articles/([A-Za-z0-9_\\-]+)".toRegex()
    private val urlRegex = "(https?://[^\"\\\\]+)".toRegex()

    suspend fun decodeUrl(googleUrl: String): String = withContext(Dispatchers.IO) {
        try {
            if (!googleUrl.contains("news.google.com/rss/articles/") && !googleUrl.contains("news.google.com/articles/")) {
                return@withContext googleUrl
            }

            val match = articleIdRegex.find(googleUrl) ?: return@withContext googleUrl
            val articleId = match.groupValues[1]

            val getReq = Request.Builder()
                .url(googleUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()

            val html = httpClient.newCall(getReq).execute().use { resp ->
                resp.body?.string() ?: ""
            }
            if (html.isBlank()) return@withContext googleUrl

            val sigMatch = sigRegex.find(html)
            val tsMatch = tsRegex.find(html)

            if (sigMatch != null && tsMatch != null) {
                val sig = sigMatch.groupValues[1]
                val ts = tsMatch.groupValues[1]

                val innerPayload = "[\"garturlreq\",[[\"X\",\"X\",[\"X\",\"X\"],null,null,1,1,\"US:en\",null,1,null,null,null,null,null,0,1],\"X\",\"X\",1,[1,1,1],1,1,null,0,0,null,0],\"$articleId\",$ts,\"$sig\"]"
                val payloadArray = JSONArray().apply {
                    put(JSONArray().apply {
                        put(JSONArray().apply {
                            put("Fbv4je")
                            put(innerPayload)
                        })
                    })
                }

                val postBodyStr = "f.req=${URLEncoder.encode(payloadArray.toString(), "UTF-8")}"
                val postReq = Request.Builder()
                    .url("https://news.google.com/_/DotsSplashUi/data/batchexecute")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(postBodyStr.toRequestBody("application/x-www-form-urlencoded;charset=utf-8".toMediaType()))
                    .build()

                val responseHtml = httpClient.newCall(postReq).execute().use { resp ->
                    resp.body?.string() ?: ""
                }

                val matches = urlRegex.findAll(responseHtml)
                for (m in matches) {
                    val finalUrl = m.groupValues[1].replace("\\/", "/")
                    if (!finalUrl.contains("news.google.com") && !finalUrl.contains("gstatic.com")) {
                        return@withContext finalUrl
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to original URL
        }
        return@withContext googleUrl
    }
}
