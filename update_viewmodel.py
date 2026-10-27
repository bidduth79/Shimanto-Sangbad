import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add isBengali helper if not exists
bengali_helper = """
    private fun isBengali(text: String): Boolean {
        return text.any { it in '\u0980'..'\u09FF' }
    }
"""
if "private fun isBengali" not in content:
    content = content.replace("private fun isToday", bengali_helper + "\n    private fun isToday")

# Update fetchBgbNews
old_bgb_fetch = """            for (keyword in _uiState.value.keywords) {
                val fetched = mutableListOf<NewsArticle>()
                // Bangladesh (Bengali)
                fetched.addAll(repository.fetchNews(keyword, hl = "bn", gl = "BD", ceid = "BD:bn"))
                delay(500)
                // Bangladesh (English)
                fetched.addAll(repository.fetchNews(keyword, hl = "en-BD", gl = "BD", ceid = "BD:en"))
                delay(500)
                // India (Bengali)
                fetched.addAll(repository.fetchNews(keyword, hl = "bn", gl = "IN", ceid = "IN:bn"))
                delay(500)
                // India (English)
                fetched.addAll(repository.fetchNews(keyword, hl = "en-IN", gl = "IN", ceid = "IN:en"))
                delay(500)
                
                for (art in fetched) {"""

new_bgb_fetch = """            for (keyword in _uiState.value.keywords) {
                val fetched = mutableListOf<NewsArticle>()
                if (isBengali(keyword)) {
                    // Bangladesh (Bengali)
                    fetched.addAll(repository.fetchNews(keyword, hl = "bn", gl = "BD", ceid = "BD:bn"))
                    delay(300)
                    // India (Bengali)
                    fetched.addAll(repository.fetchNews(keyword, hl = "bn", gl = "IN", ceid = "IN:bn"))
                    delay(300)
                } else {
                    // International (US)
                    fetched.addAll(repository.fetchNews(keyword, hl = "en-US", gl = "US", ceid = "US:en"))
                    delay(300)
                    // India (English)
                    fetched.addAll(repository.fetchNews(keyword, hl = "en-IN", gl = "IN", ceid = "IN:en"))
                    delay(300)
                    // Bangladesh (English)
                    fetched.addAll(repository.fetchNews(keyword, hl = "en-BD", gl = "BD", ceid = "BD:en"))
                    delay(300)
                }
                
                for (art in fetched) {"""

content = content.replace(old_bgb_fetch, new_bgb_fetch)

# Update fetchNationalNews
old_national_fetch = """            for (keyword in nationalKeywords) {
                val fetched = mutableListOf<NewsArticle>()
                fetched.addAll(repository.fetchNews(keyword, hl = "bn", gl = "BD", ceid = "BD:bn"))
                delay(500)
                fetched.addAll(repository.fetchNews(keyword, hl = "en-BD", gl = "BD", ceid = "BD:en"))
                delay(500)
                fetched.addAll(repository.fetchNews(keyword, hl = "bn", gl = "IN", ceid = "IN:bn"))
                delay(500)
                fetched.addAll(repository.fetchNews(keyword, hl = "en-IN", gl = "IN", ceid = "IN:en"))
                delay(500)

                for (art in fetched) {"""

new_national_fetch = """            for (keyword in nationalKeywords) {
                val fetched = mutableListOf<NewsArticle>()
                if (isBengali(keyword)) {
                    fetched.addAll(repository.fetchNews(keyword, hl = "bn", gl = "BD", ceid = "BD:bn"))
                    delay(300)
                    fetched.addAll(repository.fetchNews(keyword, hl = "bn", gl = "IN", ceid = "IN:bn"))
                    delay(300)
                } else {
                    fetched.addAll(repository.fetchNews(keyword, hl = "en-US", gl = "US", ceid = "US:en"))
                    delay(300)
                    fetched.addAll(repository.fetchNews(keyword, hl = "en-IN", gl = "IN", ceid = "IN:en"))
                    delay(300)
                    fetched.addAll(repository.fetchNews(keyword, hl = "en-BD", gl = "BD", ceid = "BD:en"))
                    delay(300)
                }

                for (art in fetched) {"""

content = content.replace(old_national_fetch, new_national_fetch)

with open(filepath, "w") as f:
    f.write(content)

