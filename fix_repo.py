import os

filepath = "app/src/main/java/com/example/data/NewsRepository.kt"
with open(filepath, "r") as f:
    content = f.read()

# Make sure NewsRepository class encapsulates the method
content = content.replace("    suspend fun fetchFromApi(apiUrl: String)", "    suspend fun fetchFromApi(apiUrl: String)")

# Need to check if the new method was added inside or outside the class
# It seems I appended it to the file instead of putting it inside the class.

with open(filepath, "r") as f:
    lines = f.readlines()
    
# Remove the method from the end of the file
lines_to_keep = []
in_fetch_from_api = False
for line in lines:
    if "suspend fun fetchFromApi" in line:
        in_fetch_from_api = True
    if not in_fetch_from_api:
        lines_to_keep.append(line)
        
# Find the last closing brace and insert the method before it
new_method = """
    suspend fun fetchFromApi(apiUrl: String): List<NewsArticle> = withContext(Dispatchers.IO) {
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
}
"""

final_content = "".join(lines_to_keep).rstrip()
if final_content.endswith("}"):
    final_content = final_content[:-1] + new_method
    
with open(filepath, "w") as f:
    f.write(final_content)
