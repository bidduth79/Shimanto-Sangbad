import os

filepath = "app/src/main/java/com/example/data/local/NewsDao.kt"
with open(filepath, "r") as f:
    content = f.read()

new_query = """    @Query("SELECT * FROM news_articles WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    suspend fun getNewsByTimeRange(startTime: Long, endTime: Long): List<NewsArticle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)"""

content = content.replace("    @Insert(onConflict = OnConflictStrategy.REPLACE)", new_query)

with open(filepath, "w") as f:
    f.write(content)
