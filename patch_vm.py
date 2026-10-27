import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Modify fetchBgbNews and fetchNationalNews in ViewModel to use the new API
new_fetch = """        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val apiUrl = "https://script.google.com/macros/s/AKfycbwdlKVUQig7h3a9HftRCWeOc_3j11CTxGIwqSbrW1u2d0hb9zObRziDg-zb25fIw638/exec"
            val allArticles = repository.fetchFromApi(apiUrl)
            
            val savedLinks = firebaseManager.getSavedNewsLinks().toSet()
            
            val bgbNews = allArticles.filter { it.category == "bgb" }
                .map { it.copy(isSaved = savedLinks.contains(it.link)) }
            
            val nationalNews = allArticles.filter { it.category == "national" }
                .map { it.copy(isSaved = savedLinks.contains(it.link)) }
                
            val latest = (bgbNews + nationalNews).sortedByDescending { it.timestamp }.take(10)

            _uiState.update { 
                it.copy(
                    bgbNews = bgbNews, 
                    nationalNews = nationalNews,
                    latestNews = latest,
                    isRefreshing = false 
                ) 
            }
            fetchVideos()
        }"""

old_fetch_block = """        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            fetchBgbNews()
            fetchNationalNews()
            fetchVideos()
            _uiState.update { it.copy(isRefreshing = false) }
        }"""

content = content.replace(old_fetch_block, new_fetch)

old_init_block = """        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchBgbNews()
            fetchNationalNews()
            fetchVideos()
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }"""
        
new_init_block = """        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val apiUrl = "https://script.google.com/macros/s/AKfycbwdlKVUQig7h3a9HftRCWeOc_3j11CTxGIwqSbrW1u2d0hb9zObRziDg-zb25fIw638/exec"
            val allArticles = repository.fetchFromApi(apiUrl)
            
            val savedLinks = firebaseManager.getSavedNewsLinks().toSet()
            
            val bgbNews = allArticles.filter { it.category == "bgb" }
                .map { it.copy(isSaved = savedLinks.contains(it.link)) }
            
            val nationalNews = allArticles.filter { it.category == "national" }
                .map { it.copy(isSaved = savedLinks.contains(it.link)) }
                
            val latest = (bgbNews + nationalNews).sortedByDescending { it.timestamp }.take(10)

            _uiState.update { 
                it.copy(
                    bgbNews = bgbNews, 
                    nationalNews = nationalNews,
                    latestNews = latest,
                    isLoading = false,
                    isRefreshing = false 
                ) 
            }
            fetchVideos()
        }"""

content = content.replace(old_init_block, new_init_block)


with open(filepath, "w") as f:
    f.write(content)
