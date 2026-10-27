import os

filepath = "app/src/main/java/com/example/data/NewsRepository.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add moshi import
import_statement = "import kotlinx.coroutines.withContext\nimport org.json.JSONObject\nimport org.json.JSONArray\n"
content = content.replace("import kotlinx.coroutines.withContext", import_statement)

# Add fetchFromApi method
new_method = """    suspend fun fetchFromApi(apiUrl: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()
        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = 10000
            connection.connectTimeout = 10000
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(responseString)
                
                // Parse BGB News
                if (jsonObject.has("bgb")) {
                    val bgbArray = jsonObject.getJSONArray("bgb")
                    for (i in 0 until bgbArray.length()) {
                        val item = bgbArray.getJSONObject(i)
                        articles.add(
                            NewsArticle(
                                title = item.getString("title"),
                                link = item.getString("link"),
                                pubDate = item.getString("pubDate"),
                                source = item.getString("source"),
                                category = item.getString("category"),
                                timestamp = item.getLong("timestamp")
                            )
                        )
                    }
                }

                // Parse National News
                if (jsonObject.has("national")) {
                    val nationalArray = jsonObject.getJSONArray("national")
                    for (i in 0 until nationalArray.length()) {
                        val item = nationalArray.getJSONObject(i)
                        articles.add(
                            NewsArticle(
                                title = item.getString("title"),
                                link = item.getString("link"),
                                pubDate = item.getString("pubDate"),
                                source = item.getString("source"),
                                category = item.getString("category"),
                                timestamp = item.getLong("timestamp")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        articles
    }
"""

content = content + "\n" + new_method

with open(filepath, "w") as f:
    f.write(content)
