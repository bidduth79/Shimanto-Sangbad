import os

filepath = "app/src/main/java/com/example/data/FirebaseManager.kt"
with open(filepath, "r") as f:
    content = f.read()

new_set = """
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
"""

content = content.replace("documentRef.set(article).await()", new_set)

with open(filepath, "w") as f:
    f.write(content)
