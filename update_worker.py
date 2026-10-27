import os

filepath = "app/src/main/java/com/example/data/NewsWorker.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add isBengali helper
bengali_helper = """
    private fun isBengali(text: String): Boolean {
        return text.any { it in '\u0980'..'\u09FF' }
    }
"""
if "private fun isBengali" not in content:
    content = content.replace("class NewsWorker(", bengali_helper + "\nclass NewsWorker(")

old_fetch = """            for (keyword in keywords) {
                bgbArticles.addAll(repository.fetchNews(keyword, hl = "bn", gl = "BD", ceid = "BD:bn"))
                bgbArticles.addAll(repository.fetchNews(keyword, hl = "en-BD", gl = "BD", ceid = "BD:en"))
                bgbArticles.addAll(repository.fetchNews(keyword, hl = "bn", gl = "IN", ceid = "IN:bn"))
                bgbArticles.addAll(repository.fetchNews(keyword, hl = "en-IN", gl = "IN", ceid = "IN:en"))
            }"""

new_fetch = """            for (keyword in keywords) {
                if (isBengali(keyword)) {
                    bgbArticles.addAll(repository.fetchNews(keyword, hl = "bn", gl = "BD", ceid = "BD:bn"))
                    bgbArticles.addAll(repository.fetchNews(keyword, hl = "bn", gl = "IN", ceid = "IN:bn"))
                } else {
                    bgbArticles.addAll(repository.fetchNews(keyword, hl = "en-US", gl = "US", ceid = "US:en"))
                    bgbArticles.addAll(repository.fetchNews(keyword, hl = "en-IN", gl = "IN", ceid = "IN:en"))
                    bgbArticles.addAll(repository.fetchNews(keyword, hl = "en-BD", gl = "BD", ceid = "BD:en"))
                }
            }"""

content = content.replace(old_fetch, new_fetch)

with open(filepath, "w") as f:
    f.write(content)

