import os

filepath = "app/src/main/java/com/example/ui/NewsViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

old_fetch_all = """    private fun fetchAllNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val apiUrl = "https://script.google.com/macros/s/AKfycbwdlKVUQig7h3a9HftRCWeOc_3j11CTxGIwqSbrW1u2d0hb9zObRziDg-zb25fIw638/exec"
            val allArticles = repository.fetchFromApi(apiUrl)
            
            val savedLinks = firebaseManager.getSavedNewsLinks().toSet()
            
            val bgbNews = allArticles.filter { article -> article.category == "bgb" }
                .map { article -> article.copy(isSaved = savedLinks.contains(article.link)) }
            
            val nationalNews = allArticles.filter { article -> article.category == "national" }
                .map { article -> article.copy(isSaved = savedLinks.contains(article.link)) }
                
            val latest = (bgbNews + nationalNews).sortedByDescending { article -> article.timestamp }.take(10)

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
        }
    }"""

new_fetch_all = """    private fun fetchAllNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            fetchBgbNews()
            fetchNationalNews()
            fetchVideos()
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }"""

content = content.replace(old_fetch_all, new_fetch_all)

old_refresh = """    fun refreshNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val apiUrl = "https://script.google.com/macros/s/AKfycbwdlKVUQig7h3a9HftRCWeOc_3j11CTxGIwqSbrW1u2d0hb9zObRziDg-zb25fIw638/exec"
            val allArticles = repository.fetchFromApi(apiUrl)
            
            val savedLinks = firebaseManager.getSavedNewsLinks().toSet()
            
            val bgbNews = allArticles.filter { article -> article.category == "bgb" }
                .map { article -> article.copy(isSaved = savedLinks.contains(article.link)) }
            
            val nationalNews = allArticles.filter { article -> article.category == "national" }
                .map { article -> article.copy(isSaved = savedLinks.contains(article.link)) }
                
            val latest = (bgbNews + nationalNews).sortedByDescending { article -> article.timestamp }.take(10)

            _uiState.update { 
                it.copy(
                    bgbNews = bgbNews, 
                    nationalNews = nationalNews,
                    latestNews = latest,
                    isRefreshing = false 
                ) 
            }
            fetchVideos()
        }
    }"""

new_refresh = """    fun refreshNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            fetchBgbNews()
            fetchNationalNews()
            fetchVideos()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }"""
    
content = content.replace(old_refresh, new_refresh)

old_resolve = """    fun resolveAndCopyLink(article: NewsArticle, onLinkResolved: (String) -> Unit) {
        viewModelScope.launch {
            // Since we use direct API now, links are mostly direct, no decoding needed
            val finalUrl = article.originalLink ?: article.link
            onLinkResolved(finalUrl)
        }
    }"""
    
new_resolve = """    fun resolveAndCopyLink(article: NewsArticle, onLinkResolved: (String) -> Unit) {
        viewModelScope.launch {
            val articleLink = article.originalLink ?: article.link
            val finalUrl = com.example.data.GoogleNewsDecoder.decodeUrl(articleLink)
            onLinkResolved(finalUrl)
        }
    }"""
    
content = content.replace(old_resolve, new_resolve)

old_start_auto = """    private fun startAutoUpdate() {
        // BGB keywords update every 10 minutes
        viewModelScope.launch {
            while (true) {
                delay(10 * 60 * 1000) // 10 minutes
                if (_uiState.value.isAutoUpdating) {
                    fetchBgbNews()
                }
            }
        }
    }"""

new_start_auto = """    private fun startAutoUpdate() {
        // BGB keywords update every 10 minutes
        viewModelScope.launch {
            while (true) {
                delay(10 * 60 * 1000) // 10 minutes
                if (_uiState.value.isAutoUpdating) {
                    fetchBgbNews()
                }
            }
        }
        
        // National keywords update every 20 minutes
        viewModelScope.launch {
            while (true) {
                delay(20 * 60 * 1000) // 20 minutes
                if (_uiState.value.isAutoUpdating) {
                    fetchNationalNews()
                }
            }
        }
    }"""
    
content = content.replace(old_start_auto, new_start_auto)

with open(filepath, "w") as f:
    f.write(content)
