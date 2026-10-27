import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix sorting in loadNewsFromDatabase
old_load = """
            val allBgb = database.newsDao().getNewsByCategory("bgb").filter { !isBlockedNews(it) }
            val allNational = database.newsDao().getNewsByCategory("national").filter { !isBlockedNews(it) }
            val saved = database.newsDao().getSavedNews().filter { !isBlockedNews(it) }
"""

new_load = """
            val allBgb = database.newsDao().getNewsByCategory("bgb").filter { !isBlockedNews(it) }.sortedByDescending { it.timestamp }
            val allNational = database.newsDao().getNewsByCategory("national").filter { !isBlockedNews(it) }.sortedByDescending { it.timestamp }
            val saved = database.newsDao().getSavedNews().filter { !isBlockedNews(it) }
"""
content = content.replace(old_load, new_load)

# Fix sorting when updating state in fetchBgbNews
old_bgb_update = """
        val updatedBgb = database.newsDao().getNewsByCategory("bgb").filter { !isBlockedNews(it) && isValidBorderNews(it) }
        _uiState.update { state -> 
            val allLatest = (updatedBgb + state.nationalNews)
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }
            state.copy(bgbNews = updatedBgb, latestNews = allLatest)
"""

new_bgb_update = """
        val updatedBgb = database.newsDao().getNewsByCategory("bgb").filter { !isBlockedNews(it) && isValidBorderNews(it) }.sortedByDescending { it.timestamp }
        _uiState.update { state -> 
            val allLatest = (updatedBgb + state.nationalNews)
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }
            state.copy(bgbNews = updatedBgb, latestNews = allLatest)
"""
content = content.replace(old_bgb_update, new_bgb_update)

# Fix sorting when updating state in fetchNationalNews
old_national_update = """
        val updatedNational = database.newsDao().getNewsByCategory("national").filter { !isBlockedNews(it) && isValidBorderNews(it) }
        _uiState.update { state -> 
            val allLatest = (state.bgbNews + updatedNational)
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }
            state.copy(nationalNews = updatedNational, latestNews = allLatest)
"""

new_national_update = """
        val updatedNational = database.newsDao().getNewsByCategory("national").filter { !isBlockedNews(it) && isValidBorderNews(it) }.sortedByDescending { it.timestamp }
        _uiState.update { state -> 
            val allLatest = (state.bgbNews + updatedNational)
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }
            state.copy(nationalNews = updatedNational, latestNews = allLatest)
"""
content = content.replace(old_national_update, new_national_update)

with open(filepath, "w") as f:
    f.write(content)
