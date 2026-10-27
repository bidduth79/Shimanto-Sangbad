package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NewsArticle
import com.example.data.NewsRepository
import com.example.data.local.NewsDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

data class NewsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isAutoUpdating: Boolean = true,
    val bgbNews: List<NewsArticle> = emptyList(),
    val facebookNews: List<NewsArticle> = emptyList(),
    val indianNews: List<NewsArticle> = emptyList(),
    val myanmarNews: List<NewsArticle> = emptyList(),
    val internationalNews: List<NewsArticle> = emptyList(),
    val rssNews: List<NewsArticle> = emptyList(),
    val rssFeeds: List<com.example.data.RssFeedItem> = emptyList(),
    val bgbVideos: List<com.example.data.YouTubeSearchResult> = emptyList(),
    val videoErrorMessage: String? = null,
    val latestNews: List<NewsArticle> = emptyList(),
    val nationalNews: List<NewsArticle> = emptyList(),
    val savedNews: List<NewsArticle> = emptyList(),
    val selectedFilter: String = "সকল",
    val areFilterChipsVisible: Boolean = true,
    val selectedTab: Int = 0, // 0 = বাংলাদেশ, 1 = ভারতীয়, 2 = মায়ানমার, 3 = ইন্টারন্যাশনাল
    val selectedBottomTab: Int = 0,
    val searchQuery: String = "",
    val keywords: List<String> = emptyList(),
    val youtubeApiKey: String = "",
    val youtubeChannelId: String = "",
    val visitedLinks: Set<String> = emptySet(),
    val isDarkMode: Boolean = true
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository by lazy { NewsRepository() }
    private val youtubeRepository by lazy { com.example.data.YouTubeRepository() }
    private val database by lazy { NewsDatabase.getDatabase(application) }
    private val keywordPrefs by lazy { com.example.data.KeywordPreferences(application) }
    private val firebaseManager by lazy { com.example.data.FirebaseManager(application) }
    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    // In-memory cache for resolved original URLs so repeated copies/shares are instantaneous
    private val resolvedUrlCache = ConcurrentHashMap<String, String>()
    private val networkSemaphore = Semaphore(12)

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun toggleFilterChipsVisibility() {
        _uiState.update { it.copy(areFilterChipsVisible = !it.areFilterChipsVisible) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun markArticleVisited(link: String) {
        if (link.isBlank()) return
        _uiState.update { state ->
            state.copy(visitedLinks = state.visitedLinks + link)
        }
        viewModelScope.launch(Dispatchers.IO) {
            keywordPrefs.markLinkVisited(link)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val loadedKeywords = keywordPrefs.getKeywords()
            val loadedRssFeeds = keywordPrefs.getRssFeeds()
            val loadedApiKey = keywordPrefs.getYoutubeApiKey()
            val loadedChannelId = keywordPrefs.getYoutubeChannelId()
            val loadedVisited = keywordPrefs.getVisitedLinks()
            _uiState.update {
                it.copy(
                    keywords = loadedKeywords,
                    rssFeeds = loadedRssFeeds,
                    youtubeApiKey = loadedApiKey,
                    youtubeChannelId = loadedChannelId,
                    visitedLinks = loadedVisited
                )
            }
            // 1. Load cached database news first on background thread
            loadNewsFromDatabase()
            // 2. Fetch fresh news and videos in parallel on background thread
            fetchAllNews(showFullScreenLoader = _uiState.value.bgbNews.isEmpty())
        }
        syncWithCloud()
        startAutoUpdate()
    }

    private fun syncWithCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cloudNews = firebaseManager.restoreSavedNews()
                if (cloudNews.isNotEmpty()) {
                    database.newsDao().insertAll(cloudNews)
                    loadNewsFromDatabase()
                }
            } catch (e: Exception) {
                // Ignore cloud sync errors silently
            }
        }
    }

    companion object {
        // Strict 36-hour freshness cutoff so old news never appears in any feed
        private const val MAX_NEWS_AGE_MILLIS = 36L * 60 * 60 * 1000L

        private val BD_MEDIA_MARKERS = listOf(
            // Bengali Names
            "প্রথম আলো", "যুগান্তর", "কালের কণ্ঠ", "ইত্তেফাক", "সমকাল", "মানবজমিন",
            "নয়া দিগন্ত", "নয়া দিগন্ত", "বাংলাদেশ প্রতিদিন", "জনকণ্ঠ", "ইনকিলাব",
            "বণিক বার্তা", "আজকের পত্রিকা", "দেশ রূপান্তর", "যায়যায়দিন", "যায়যায়দিন",
            "বিডিনিউজ", "বাংলানিউজ", "ঢাকা পোস্ট", "জাগো নিউজ", "রাইজিংবিডি",
            "বার্তা২৪", "সারাবাংলা", "বাংলা ট্রিবিউন", "ঢাকা টাইমস", "সময় টিভি",
            "সময় টিভি", "সময় নিউজ", "সময় নিউজ", "যমুনা টিভি", "যমুনা নিউজ",
            "চ্যানেল আই", "চ্যানেল ২৪", "একাত্তর", "এনটিভি", "আরটিভি", "ইন্ডিপেন্ডেন্ট",
            "কালবেলা", "আমাদের সময়", "আমাদের সময়", "ভোরের কাগজ", "ডেইলি স্টার",
            "ঢাকা ট্রিবিউন", "বাসস", "ইউএনবি", "দৈনিক বাংলা", "প্রতিদিনের বাংলাদেশ",
            "প্রতিদিনের সংবাদ", "খবরের কাগজ", "মানবকণ্ঠ", "আলোকিত বাংলাদেশ", "ডিবিসি",
            "নিউজ ২৪", "এটিএন", "দীপ্ত টিভি", "একুশে টিভি", "বৈশাখী টিভি", "মাছরাঙা",
            "নাগরিক টিভি", "দেশ টিভি", "ঢাকা মেইল", "ঢাকা প্রকাশ", "দৈনিক আজাদী",
            "দৈনিক পূর্বকোণ", "চট্টগ্রাম প্রতিদিন", "সিলেট ভিউ", "কক্সবাজার নিউজ",
            "পার্বত্য নিউজ", "প্রিয়.কম", "জুমবাংলা", "বার্তা বাজার", "বাংলা ইনসাইডার",
            "বিডি জার্নাল", "অধিকার", "আমার সংবাদ", "নয়া শতাব্দী", "নয়া শতাব্দী",
            "দৈনিক সংগ্রাম", "দৈনিক করতোয়া", "পূর্বাঞ্চল", "সোনার দেশ", "বিডি২৪লাইভ",
            "দিনকাল", "রেডিও টুডে", "জাগো এফএম", "সকালের সময়", "সকালের সময়",
            "খোলা কাগজ", "সময়ের আলো", "সময়ের আলো", "আলোকিত সকাল", "ঢাকা টুডে",
            // English Names & Domains
            "prothom alo", "prothomalo", "jugantor", "kaler kantho", "kalerkantho",
            "ittefaq", "samakal", "manab zamin", "manabzamin", "mzamin",
            "naya diganta", "nayadiganta", "bangladesh pratidin", "bd-pratidin",
            "janakantha", "inqilab", "bonik barta", "bonikbarta", "ajker patrika",
            "ajkerpatrika", "desh rupantor", "deshrupantor", "jaijaidin",
            "bdnews24", "banglanews24", "dhaka post", "dhakapost", "jago news",
            "jagonews", "risingbd", "barta24", "sarabangla", "bangla tribune",
            "banglatribune", "dhaka times", "dhakatimes", "somoy", "jamuna",
            "channel i", "channelionline", "channel 24", "channel24", "ekattor",
            "ntvbd", "ntv online", "rtvonline", "independent24", "independent tv",
            "kalbela", "amader shomoy", "amadershomoy", "bhorer kagoj", "bhorerkagoj",
            "vorerkagoj", "the daily star", "thedailystar", "dailystar.net",
            "dhaka tribune", "dhakatribune", "business standard", "tbsnews", "tbs news",
            "new age", "newagebd", "financial express", "thefinancialexpress",
            "daily sun", "daily-sun", "daily observer", "observerbd", "bssnews",
            "unb.com", "united news of bangladesh", "dainik bangla", "dainikbangla",
            "protidiner bangladesh", "protidinerbangladesh", "protidiner sangbad",
            "protidinersangbad", "khaborer kagoj", "khaborerkagoj", "manobkantha",
            "alokito bangladesh", "alokitobangladesh", "dbc news", "dbcnews",
            "news24bd", "atn bangla", "atn news", "deepto", "ekushey", "boishakhi",
            "maasranga", "nagorik", "desh tv", "desh.tv", "dhaka mail", "dhakamail",
            "dhaka prokash", "dhakaprokash", "dainik azadi", "dainikazadi",
            "purbokone", "ctgpratidin", "sylhetview", "coxsbazarnews", "ukhiyanews",
            "parbattanews", "zoombangla", "dmpnews", "ispr", "bahannonews",
            "shampratikdeshkal", "sharebiz", "arthosuchak", "orthomail",
            "banglainsider", "bdjournal", "opinionbd", "amarsangbad", "amar-sangbad",
            "bbarta24", "campuslive", "odhikar.news", "viewsbangladesh", "karatoa",
            "sonalisangbad", "purbanchal", "bd24live", "bdmorning", "bdtoday",
            "bangladarpan", "banglarbarta", "bangladesh post", "bangladeshpost",
            "daily asian age", "dailyasianage", "bangladesh monitor", "dhaka courier",
            "energy bangla", "textile today", "cvoice24", "shomoyer alo", "shomoyeralo",
            "khola kagoj", "kholakagoj", "deshkal", "bartabazar", "dhakatoday"
        )

        private val SPORTS_KEYWORDS = listOf(
            "খেলা", "ক্রিকেট", "ফুটবল", "স্পোর্টস", "টি-টোয়েন্টি", "আইপিএল", "বিপিএল",
            "sports", "cricket", "football", "খেলোয়াড়", "টুর্নামেন্ট", "বিশ্বকাপ",
            "মেসি", "রোনালদো", "সাকিব", "তামিম", "বিসিবি", "বাফুফে", "হকি", "টেনিস", "অলিম্পিক"
        )
    }

    private suspend fun loadNewsFromDatabase() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            database.newsDao().deleteOldNews(System.currentTimeMillis() - MAX_NEWS_AGE_MILLIS)

            val allBgb = database.newsDao().getNewsByCategory("bgb")
                .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) && !isFacebookOrSocialNews(it) }
                .sortedByDescending { it.timestamp }
            val allNational = database.newsDao().getNewsByCategory("national")
                .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) && !isFacebookOrSocialNews(it) }
                .sortedByDescending { it.timestamp }
            val allIndian = database.newsDao().getNewsByCategory("indian")
                .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) && !isBangladeshiMedia(it) && !isFacebookOrSocialNews(it) }
                .sortedByDescending { it.timestamp }
            val allMyanmar = database.newsDao().getNewsByCategory("myanmar")
                .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) && !isBangladeshiMedia(it) && !isFacebookOrSocialNews(it) }
                .sortedByDescending { it.timestamp }
            val allInternational = database.newsDao().getNewsByCategory("international")
                .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) && !isBangladeshiMedia(it) && !isFacebookOrSocialNews(it) }
                .sortedByDescending { it.timestamp }
            val allRss = database.newsDao().getNewsByCategory("rss")
                .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) }
                .sortedByDescending { it.timestamp }
            val saved = database.newsDao().getSavedNews()
                .filter { !isBlockedNews(it) }
                .sortedByDescending { it.timestamp }

            val dbFacebook = database.newsDao().getNewsByCategory("facebook")
                .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) }
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }

            val allLatest = (allBgb + allNational)
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }

            _uiState.update { state ->
                state.copy(
                    bgbNews = allBgb,
                    facebookNews = dbFacebook,
                    nationalNews = allNational,
                    indianNews = allIndian,
                    myanmarNews = allMyanmar,
                    internationalNews = allInternational,
                    rssNews = allRss.ifEmpty { state.rssNews },
                    latestNews = allLatest,
                    savedNews = saved,
                    isLoading = if (allBgb.isNotEmpty() || allNational.isNotEmpty() || allIndian.isNotEmpty()) false else state.isLoading
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchAllNews(showFullScreenLoader: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (showFullScreenLoader && _uiState.value.bgbNews.isEmpty()) {
                _uiState.update { it.copy(isLoading = true) }
            } else {
                _uiState.update { it.copy(isRefreshing = true) }
            }

            supervisorScope {
                val bgbJob = async(Dispatchers.IO) { fetchBgbNews() }
                val nationalJob = async(Dispatchers.IO) { fetchNationalNews() }
                val indianJob = async(Dispatchers.IO) { fetchIndianNews() }
                val myanmarJob = async(Dispatchers.IO) { fetchMyanmarNews() }
                val internationalJob = async(Dispatchers.IO) { fetchInternationalNews() }
                val rssJob = async(Dispatchers.IO) { fetchCustomRssNews() }
                val videoJob = async(Dispatchers.IO) { fetchVideos() }
                awaitAll(bgbJob, nationalJob, indianJob, myanmarJob, internationalJob, rssJob, videoJob)
            }

            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            preDecodeTopLinks(_uiState.value.latestNews.take(10))
        }
    }

    private fun preDecodeTopLinks(topArticles: List<NewsArticle>) {
        viewModelScope.launch(Dispatchers.IO) {
            for (article in topArticles) {
                val rawLink = article.originalLink ?: article.link
                if (rawLink.isNotBlank() && !resolvedUrlCache.containsKey(rawLink)) {
                    try {
                        val decoded = com.example.data.GoogleNewsDecoder.decodeUrl(rawLink)
                        if (decoded.isNotBlank()) {
                            resolvedUrlCache[rawLink] = decoded
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun resolveAndCopyLink(article: NewsArticle, onLinkResolved: (String) -> Unit) {
        val rawLink = article.originalLink ?: article.link
        val cached = resolvedUrlCache[rawLink]
        if (!cached.isNullOrBlank()) {
            onLinkResolved(cached)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val finalUrl = com.example.data.GoogleNewsDecoder.decodeUrl(rawLink)
            if (finalUrl.isNotBlank()) {
                resolvedUrlCache[rawLink] = finalUrl
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onLinkResolved(finalUrl)
            }
        }
    }

    fun refreshNews() {
        fetchAllNews(showFullScreenLoader = false)
    }

    /**
     * Fetches Bangladesh News combining all 5 major news search mediums together:
     * 1. Google News (when:1d)
     * 2. Bing News (24h sorted by date)
     * 3. Yahoo News
     * 4. DuckDuckGo News (24h verified)
     * 5. Reddit News (24h sorted by new)
     * Plus Facebook / Social Feed stored separately under category = "facebook".
     */
    private suspend fun fetchBgbNews() = supervisorScope {
        val keywords = _uiState.value.keywords
        val newsTasks = mutableListOf<kotlinx.coroutines.Deferred<List<NewsArticle>>>()
        val fbTasks = mutableListOf<kotlinx.coroutines.Deferred<List<NewsArticle>>>()
        val socialExclusions = "-site:facebook.com -site:fb.com"

        for (keyword in keywords) {
            if (isBengali(keyword)) {
                // 1. Google News (Bangladesh Bengali edition - strictly last 24h)
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$keyword $socialExclusions", hl = "bn", gl = "BD", ceid = "BD:bn", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                // 2. Bing News (Bengali keyword)
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchBingNews(keyword, market = "en-US", matchedTag = keyword)
                    }
                })
                // 3. Yahoo News (Bengali keyword)
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchYahooNews(keyword, matchedTag = keyword)
                    }
                })
                // 4. DuckDuckGo News (Bengali keyword)
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchDuckDuckGoNews(keyword, matchedTag = keyword)
                    }
                })
                // 5. Reddit News (Bengali keyword)
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchRedditNews(keyword, matchedTag = keyword)
                    }
                })

                // Also query English equivalents for Bangladesh across Bing, Yahoo, DuckDuckGo, Reddit & Google BD English
                val enVariants = repository.getEnglishSearchVariants(keyword)
                for (enQuery in enVariants) {
                    newsTasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchNews("$enQuery $socialExclusions", hl = "en-BD", gl = "BD", ceid = "BD:en", whenWindow = "1d")
                                .map { it.copy(matchedKeyword = keyword) }
                        }
                    })
                    newsTasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchBingNews(enQuery, market = "en-US", matchedTag = keyword)
                        }
                    })
                    newsTasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchYahooNews(enQuery, matchedTag = keyword)
                        }
                    })
                    newsTasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchDuckDuckGoNews(enQuery, matchedTag = keyword)
                        }
                    })
                    newsTasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchRedditNews(enQuery, matchedTag = keyword)
                        }
                    })
                }
            } else {
                // 1. Google News (Bangladesh English edition - strictly last 24h)
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$keyword $socialExclusions", hl = "en-BD", gl = "BD", ceid = "BD:en", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                // 2. Bing News
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchBingNews("$keyword Bangladesh", market = "en-US", matchedTag = keyword)
                    }
                })
                // 3. Yahoo News
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchYahooNews("$keyword Bangladesh", matchedTag = keyword)
                    }
                })
                // 4. DuckDuckGo News
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchDuckDuckGoNews("$keyword Bangladesh", matchedTag = keyword)
                    }
                })
                // 5. Reddit News
                newsTasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchRedditNews("$keyword Bangladesh", matchedTag = keyword)
                    }
                })
            }
            // 6. Facebook & Social posts fetched into dedicated social collection
            fbTasks.add(async(Dispatchers.IO) {
                networkSemaphore.withPermit {
                    repository.fetchFacebookNews(keyword)
                }
            })
        }

        val bgbArticles = try {
            newsTasks.awaitAll().flatten()
        } catch (e: Exception) {
            emptyList()
        }

        val fetchedFbArticles = try {
            fbTasks.awaitAll().flatten()
        } catch (e: Exception) {
            emptyList()
        }

        val savedLinks = database.newsDao().getSavedNews().map { it.link }.toSet()
        val allBgb = bgbArticles
            .filter {
                isStrictlyFreshArticle(it) &&
                    !isSportsNews(it) &&
                    !isBlockedNews(it) &&
                    !isFacebookOrSocialNews(it) &&
                    isValidBorderNews(it)
            }
            .distinctBy { it.link }
            .distinctBy { "${getSearchMedium(it)}|${it.title.trim().lowercase()}" }
            .map { it.copy(category = "bgb", isSaved = savedLinks.contains(it.link)) }
            .sortedByDescending { it.timestamp }

        val cleanedFb = fetchedFbArticles
            .filter { isStrictlyFreshArticle(it) && !isSportsNews(it) && !isBlockedNews(it) }
            .distinctBy { it.link }
            .distinctBy { it.title.trim().lowercase() }
            .map { it.copy(category = "facebook", isSaved = savedLinks.contains(it.link)) }
            .sortedByDescending { it.timestamp }

        if (allBgb.isNotEmpty()) {
            database.newsDao().insertAll(allBgb)
        }
        if (cleanedFb.isNotEmpty()) {
            database.newsDao().insertAll(cleanedFb)
        }

        val updatedBgb = database.newsDao().getNewsByCategory("bgb")
            .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) && !isFacebookOrSocialNews(it) && isValidBorderNews(it) }
            .sortedByDescending { it.timestamp }

        val updatedFb = database.newsDao().getNewsByCategory("facebook")
            .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) }
            .distinctBy { it.link }
            .sortedByDescending { it.timestamp }

        _uiState.update { state ->
            val allLatest = (updatedBgb + state.nationalNews)
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }
            state.copy(
                bgbNews = updatedBgb,
                facebookNews = updatedFb,
                latestNews = allLatest,
                isLoading = false
            )
        }
    }

    /**
     * Fetches Indian News across ALL Indian newspapers and portals (Bengali & English)
     * strictly from the last 24h based on the user's saved keywords, while 100% blocking
     * Bangladeshi newspapers and Facebook/Social media posts.
     */
    private suspend fun fetchIndianNews() = supervisorScope {
        val keywords = _uiState.value.keywords
        if (keywords.isEmpty()) return@supervisorScope
        val tasks = mutableListOf<kotlinx.coroutines.Deferred<List<NewsArticle>>>()
        val bdAndSocialExclusions = "-site:.bd -site:facebook.com -site:fb.com -site:prothomalo.com -site:jugantor.com -site:kalerkantho.com -site:dhakapost.com -site:somoynews.tv -site:jagonews24.com -site:banglatribune.com -site:bd-pratidin.com"

        for (keyword in keywords) {
            if (isBengali(keyword)) {
                // 1. Google News India (Bengali edition - strictly last 24h)
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$keyword $bdAndSocialExclusions", hl = "bn", gl = "IN", ceid = "IN:bn", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                // 2. Google News India (Bengali edition with Indian state/border context - strictly last 24h)
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$keyword (ভারত OR বিএসএফ OR পশ্চিমবঙ্গ OR ত্রিপুরা OR আসাম OR কলকাতা) $bdAndSocialExclusions", hl = "bn", gl = "IN", ceid = "IN:bn", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                // 3. Bing News India for Bengali keyword
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchBingNews("$keyword ভারত", market = "en-IN", matchedTag = keyword)
                    }
                })
                // 4. English equivalents in Indian English edition (The Hindu, TOI, Indian Express, NDTV, HT, Telegraph India, etc.)
                val enVariants = repository.getEnglishSearchVariants(keyword)
                for (enQuery in enVariants) {
                    tasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchNews("$enQuery $bdAndSocialExclusions", hl = "en-IN", gl = "IN", ceid = "IN:en", whenWindow = "1d")
                                .map { it.copy(matchedKeyword = keyword) }
                        }
                    })
                    tasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchBingNews("$enQuery India", market = "en-IN", matchedTag = keyword)
                        }
                    })
                    tasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchYahooNews("$enQuery India", matchedTag = keyword)
                        }
                    })
                    tasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchDuckDuckGoNews("$enQuery India", matchedTag = keyword)
                        }
                    })
                    tasks.add(async(Dispatchers.IO) {
                        networkSemaphore.withPermit {
                            repository.fetchRedditNews("$enQuery India", matchedTag = keyword)
                        }
                    })
                }
            } else {
                // English / Latin keyword: query Google News India (English & Bengali), Bing News India, Yahoo News, DuckDuckGo, Reddit
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$keyword $bdAndSocialExclusions", hl = "en-IN", gl = "IN", ceid = "IN:en", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$keyword $bdAndSocialExclusions", hl = "bn", gl = "IN", ceid = "IN:bn", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchBingNews("$keyword India", market = "en-IN", matchedTag = keyword)
                    }
                })
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchYahooNews("$keyword India", matchedTag = keyword)
                    }
                })
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchDuckDuckGoNews("$keyword India", matchedTag = keyword)
                    }
                })
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchRedditNews("$keyword India", matchedTag = keyword)
                    }
                })
            }
        }

        val fetchedIndian = try {
            tasks.awaitAll().flatten()
        } catch (e: Exception) {
            emptyList()
        }

        val savedLinks = database.newsDao().getSavedNews().map { it.link }.toSet()
        val cleanedIndian = fetchedIndian
            .filter {
                isStrictlyFreshArticle(it) &&
                    !isSportsNews(it) &&
                    !isBlockedNews(it) &&
                    !isBangladeshiMedia(it) &&
                    !isFacebookOrSocialNews(it)
            }
            .distinctBy { it.link }
            .distinctBy { "${getSearchMedium(it)}|${it.title.trim().lowercase()}" }
            .map { it.copy(category = "indian", isSaved = savedLinks.contains(it.link)) }
            .sortedByDescending { it.timestamp }

        if (cleanedIndian.isNotEmpty()) {
            database.newsDao().insertAll(cleanedIndian)
        }

        val dbIndian = database.newsDao().getNewsByCategory("indian")
            .filter {
                isStrictlyFreshArticle(it) &&
                    !isBlockedNews(it) &&
                    !isSportsNews(it) &&
                    !isBangladeshiMedia(it) &&
                    !isFacebookOrSocialNews(it)
            }
            .sortedByDescending { it.timestamp }

        val mergedIndian = (cleanedIndian + dbIndian)
            .distinctBy { it.link }
            .distinctBy { "${getSearchMedium(it)}|${it.title.trim().lowercase()}" }
            .sortedByDescending { it.timestamp }

        _uiState.update { state ->
            state.copy(
                indianNews = mergedIndian,
                isLoading = false
            )
        }
    }

    /**
     * Fetches Myanmar & Rakhine/Arakan border news across Myanmar portals (The Irrawaddy, Myanmar Now,
     * Mizzima, Narinjara, DVB, BNI, Frontier Myanmar, Eleven Media) and global engines strictly from
     * the last 24h based on the user's saved keywords, while blocking Bangladeshi media and Facebook posts.
     */
    private suspend fun fetchMyanmarNews() = supervisorScope {
        val keywords = _uiState.value.keywords
        if (keywords.isEmpty()) return@supervisorScope
        val tasks = mutableListOf<kotlinx.coroutines.Deferred<List<NewsArticle>>>()
        val bdAndSocialExclusions = "-site:.bd -site:facebook.com -site:fb.com -site:prothomalo.com -site:jugantor.com -site:kalerkantho.com -site:dhakapost.com -site:thedailystar.net -site:tbsnews.net -site:dhakatribune.com"

        // Also query Myanmar media directly for border/Rakhine/Arakan updates (last 24h)
        tasks.add(async(Dispatchers.IO) {
            networkSemaphore.withPermit {
                repository.fetchNews(
                    "(site:irrawaddy.com OR site:myanmar-now.org OR site:mizzima.com OR site:narinjara.com OR site:bnionline.net OR site:frontiermyanmar.net OR site:dvb.no OR site:elevenmyanmar.com) (Bangladesh OR Rakhine OR Arakan OR border OR Maungdaw OR Rohingya)",
                    hl = "en-US",
                    gl = "US",
                    ceid = "US:en",
                    whenWindow = "1d"
                ).map { it.copy(matchedKeyword = "মিয়ানমার") }
            }
        })

        for (keyword in keywords) {
            val myanmarQueries = repository.getMyanmarSearchVariants(keyword)
            for (query in myanmarQueries) {
                // 1. Google News Global (last 24h, excluding BD media & Facebook)
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$query $bdAndSocialExclusions", hl = "en-US", gl = "US", ceid = "US:en", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                // 2. Bing News
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchBingNews(query, market = "en-US", matchedTag = keyword)
                    }
                })
                // 3. Yahoo News
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchYahooNews(query, matchedTag = keyword)
                    }
                })
                // 4. DuckDuckGo News
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchDuckDuckGoNews(query, matchedTag = keyword)
                    }
                })
                // 5. Reddit News
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchRedditNews(query, matchedTag = keyword)
                    }
                })
            }
        }

        val fetchedMyanmar = try {
            tasks.awaitAll().flatten()
        } catch (e: Exception) {
            emptyList()
        }

        val savedLinks = database.newsDao().getSavedNews().map { it.link }.toSet()
        val cleanedMyanmar = fetchedMyanmar
            .filter {
                isStrictlyFreshArticle(it) &&
                    !isSportsNews(it) &&
                    !isBlockedNews(it) &&
                    !isBangladeshiMedia(it) &&
                    !isFacebookOrSocialNews(it)
            }
            .distinctBy { it.link }
            .distinctBy { "${getSearchMedium(it)}|${it.title.trim().lowercase()}" }
            .map { it.copy(category = "myanmar", isSaved = savedLinks.contains(it.link)) }
            .sortedByDescending { it.timestamp }

        if (cleanedMyanmar.isNotEmpty()) {
            database.newsDao().insertAll(cleanedMyanmar)
        }

        val dbMyanmar = database.newsDao().getNewsByCategory("myanmar")
            .filter {
                isStrictlyFreshArticle(it) &&
                    !isBlockedNews(it) &&
                    !isSportsNews(it) &&
                    !isBangladeshiMedia(it) &&
                    !isFacebookOrSocialNews(it)
            }
            .sortedByDescending { it.timestamp }

        val mergedMyanmar = (cleanedMyanmar + dbMyanmar)
            .distinctBy { it.link }
            .distinctBy { "${getSearchMedium(it)}|${it.title.trim().lowercase()}" }
            .sortedByDescending { it.timestamp }

        _uiState.update { state ->
            state.copy(
                myanmarNews = mergedMyanmar,
                isLoading = false
            )
        }
    }

    /**
     * Fetches International News across ALL global agencies & newspapers (Google News US/UK,
     * Bing News Global, Yahoo News Global, DuckDuckGo News, Reddit World News) strictly from the last 24h
     * based on the user's saved keywords, while blocking Bangladeshi newspapers and Facebook posts.
     */
    private suspend fun fetchInternationalNews() = supervisorScope {
        val keywords = _uiState.value.keywords
        if (keywords.isEmpty()) return@supervisorScope
        val tasks = mutableListOf<kotlinx.coroutines.Deferred<List<NewsArticle>>>()
        val bdAndSocialExclusions = "-site:.bd -site:facebook.com -site:fb.com -site:prothomalo.com -site:jugantor.com -site:kalerkantho.com -site:dhakapost.com -site:thedailystar.net -site:tbsnews.net -site:dhakatribune.com -site:somoynews.tv -site:jagonews24.com"

        for (keyword in keywords) {
            val enVariants = repository.getEnglishSearchVariants(keyword)
            if (isBengali(keyword)) {
                // Global Bengali coverage (BBC Bangla, DW Bangla, VOA Bangla, BenarNews, NHK, etc., last 24h)
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$keyword $bdAndSocialExclusions", hl = "bn", gl = "US", ceid = "US:bn", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchBingNews(keyword, market = "en-US", matchedTag = keyword)
                    }
                })
            }

            for (query in enVariants) {
                // 1. Google News International (US Global edition, last 24h)
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$query $bdAndSocialExclusions", hl = "en-US", gl = "US", ceid = "US:en", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                // 2. Google News International (UK World edition, last 24h)
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchNews("$query $bdAndSocialExclusions", hl = "en-GB", gl = "GB", ceid = "GB:en", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                })
                // 3. Bing News Global Search
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchBingNews(query, market = "en-US", matchedTag = keyword)
                    }
                })
                // 4. Yahoo News Global Search (Reuters, AP, AFP, Al Jazeera, etc.)
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchYahooNews(query, matchedTag = keyword)
                    }
                })
                // 5. DuckDuckGo Global News Search
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchDuckDuckGoNews(query, matchedTag = keyword)
                    }
                })
                // 6. Reddit World & Geopolitics RSS Search
                tasks.add(async(Dispatchers.IO) {
                    networkSemaphore.withPermit {
                        repository.fetchRedditNews(query, matchedTag = keyword)
                    }
                })
            }
        }

        val fetchedIntl = try {
            tasks.awaitAll().flatten()
        } catch (e: Exception) {
            emptyList()
        }

        val savedLinks = database.newsDao().getSavedNews().map { it.link }.toSet()
        val cleanedIntl = fetchedIntl
            .filter {
                isStrictlyFreshArticle(it) &&
                    !isSportsNews(it) &&
                    !isBlockedNews(it) &&
                    !isBangladeshiMedia(it) &&
                    !isFacebookOrSocialNews(it)
            }
            .distinctBy { it.link }
            .distinctBy { "${getSearchMedium(it)}|${it.title.trim().lowercase()}" }
            .map { it.copy(category = "international", isSaved = savedLinks.contains(it.link)) }
            .sortedByDescending { it.timestamp }

        if (cleanedIntl.isNotEmpty()) {
            database.newsDao().insertAll(cleanedIntl)
        }

        val dbIntl = database.newsDao().getNewsByCategory("international")
            .filter {
                isStrictlyFreshArticle(it) &&
                    !isBlockedNews(it) &&
                    !isSportsNews(it) &&
                    !isBangladeshiMedia(it) &&
                    !isFacebookOrSocialNews(it)
            }
            .sortedByDescending { it.timestamp }

        val mergedIntl = (cleanedIntl + dbIntl)
            .distinctBy { it.link }
            .distinctBy { "${getSearchMedium(it)}|${it.title.trim().lowercase()}" }
            .sortedByDescending { it.timestamp }

        _uiState.update { state ->
            state.copy(
                internationalNews = mergedIntl,
                isLoading = false
            )
        }
    }

    /**
     * Fetches news from all Custom RSS Feeds added by the user in Settings.
     */
    private suspend fun fetchCustomRssNews() = supervisorScope {
        val feeds = keywordPrefs.getRssFeeds()
        _uiState.update { it.copy(rssFeeds = feeds) }
        if (feeds.isEmpty()) {
            _uiState.update { it.copy(rssNews = emptyList()) }
            return@supervisorScope
        }

        val tasks = feeds.map { feed ->
            async(Dispatchers.IO) {
                networkSemaphore.withPermit {
                    repository.fetchCustomRssFeed(feed)
                }
            }
        }

        val fetchedRss = try {
            tasks.awaitAll().flatten()
        } catch (e: Exception) {
            emptyList()
        }

        val savedLinks = database.newsDao().getSavedNews().map { it.link }.toSet()
        val cleanedRss = fetchedRss
            .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) }
            .distinctBy { it.link }
            .map { it.copy(category = "rss", isSaved = savedLinks.contains(it.link)) }
            .sortedByDescending { it.timestamp }

        if (cleanedRss.isNotEmpty()) {
            database.newsDao().insertAll(cleanedRss)
        }

        _uiState.update { state ->
            state.copy(rssNews = cleanedRss)
        }
    }

    private suspend fun fetchNationalNews() = supervisorScope {
        val baseNationalKeywords = listOf("সীমান্ত", "বাংলাদেশ ভারত সীমান্ত", "সীমান্ত সংবাদ", "বিজিবি", "নিরাপত্তা")
        val userKeywords = _uiState.value.keywords
        val nationalKeywords = (baseNationalKeywords + userKeywords).distinct()
        val socialExclusions = "-site:facebook.com -site:fb.com"

        val tasks = nationalKeywords.map { keyword ->
            async(Dispatchers.IO) {
                networkSemaphore.withPermit {
                    if (isBengali(keyword)) {
                        repository.fetchNews("$keyword $socialExclusions", hl = "bn", gl = "BD", ceid = "BD:bn", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    } else {
                        repository.fetchNews("$keyword $socialExclusions", hl = "en-BD", gl = "BD", ceid = "BD:en", whenWindow = "1d")
                            .map { it.copy(matchedKeyword = keyword) }
                    }
                }
            }
        }

        val nationalArticles = try {
            tasks.awaitAll().flatten()
        } catch (e: Exception) {
            emptyList()
        }

        val savedLinks = database.newsDao().getSavedNews().map { it.link }.toSet()
        val allNational = nationalArticles
            .filter {
                isStrictlyFreshArticle(it) &&
                    !isSportsNews(it) &&
                    !isBlockedNews(it) &&
                    !isFacebookOrSocialNews(it) &&
                    isValidBorderNews(it)
            }
            .distinctBy { it.link }
            .distinctBy { "${getSearchMedium(it)}|${it.title.trim().lowercase()}" }
            .map { it.copy(category = "national", isSaved = savedLinks.contains(it.link)) }
            .sortedByDescending { it.timestamp }

        if (allNational.isNotEmpty()) {
            database.newsDao().insertAll(allNational)
        }

        val updatedNational = database.newsDao().getNewsByCategory("national")
            .filter { isStrictlyFreshArticle(it) && !isBlockedNews(it) && !isFacebookOrSocialNews(it) && isValidBorderNews(it) }
            .sortedByDescending { it.timestamp }

        _uiState.update { state ->
            val allLatest = (state.bgbNews + updatedNational)
                .distinctBy { it.link }
                .sortedByDescending { it.timestamp }
            state.copy(
                nationalNews = updatedNational,
                latestNews = allLatest
            )
        }
    }

    private fun isBengali(text: String): Boolean {
        return text.any { it in 'ঀ'..'৿' }
    }

    /**
     * Multi-layer freshness verification:
     * 1. Requires a real positive parsed timestamp (never 0).
     * 2. Strictly within the last 36 hours (MAX_NEWS_AGE_MILLIS).
     * 3. Rejects URLs or titles containing old archive years (e.g., /2021/, /2022/, /2023/, /2024/, /2025/).
     */
    private fun isStrictlyFreshArticle(article: NewsArticle): Boolean {
        if (article.timestamp <= 0L) return false
        val currentMillis = System.currentTimeMillis()
        val diff = currentMillis - article.timestamp
        if (diff < -600_000L || diff > MAX_NEWS_AGE_MILLIS) return false
        return !repository.hasOldArchiveYear(article.originalLink ?: article.link, article.title)
    }

    /**
     * Identifies Facebook / Social feed posts so they stay strictly in the Social Feed
     * and never leak into Indian, Myanmar, International, or newspaper-only feeds.
     */
    private fun isFacebookOrSocialNews(article: NewsArticle): Boolean {
        val sourceLower = article.source.lowercase()
        val linkLower = (article.originalLink ?: article.link).lowercase()
        return article.category == "facebook" ||
            sourceLower.contains("facebook") ||
            sourceLower.contains("fb/social") ||
            sourceLower.contains("সোশ্যাল ফিড") ||
            linkLower.contains("facebook.com") ||
            linkLower.contains("fb.com") ||
            linkLower.contains("fb.watch")
    }

    private fun isValidBorderNews(article: NewsArticle): Boolean {
        val textToCheck = article.title.lowercase()
        val userKeywords = _uiState.value.keywords

        // Always allow articles that explicitly match any user-configured keyword or matched tag
        if (userKeywords.any { kw ->
                kw.isNotBlank() && (
                    textToCheck.contains(kw.lowercase()) ||
                        article.matchedKeyword.equals(kw, ignoreCase = true)
                    )
            }
        ) {
            return true
        }

        if (textToCheck.contains("সীমান্ত") || textToCheck.contains("border")) {
            val hasBd = textToCheck.contains("বাংলাদেশ") ||
                textToCheck.contains("bangladesh") ||
                textToCheck.contains("বিজিবি") ||
                textToCheck.contains("bgb") ||
                textToCheck.contains("বর্ডার গার্ড") ||
                textToCheck.contains("বিএসএফ") ||
                textToCheck.contains("bsf")
            if (!hasBd) {
                return false
            }
        }

        val hasCrime = textToCheck.contains("মাদক") ||
            textToCheck.contains("চোরাচালান") ||
            textToCheck.contains("অস্ত্র") ||
            textToCheck.contains("গোলাবারুদ")
        if (hasCrime) {
            val hasBgb = textToCheck.contains("বিজিবি") ||
                textToCheck.contains("bgb") ||
                textToCheck.contains("বর্ডার গার্ড")
            if (!hasBgb) {
                return false
            }
        }

        return true
    }

    /**
     * Strictly identifies Bangladeshi newspapers, TV channels, online portals, and .bd domains
     * so they are completely blocked from Indian News, Myanmar News, and International News tabs.
     */
    private fun isBangladeshiMedia(article: NewsArticle): Boolean {
        val sourceLower = article.source.lowercase()
        val linkLower = (article.originalLink ?: article.link).lowercase()
        val titleTailLower = if (article.title.contains(" - ")) {
            article.title.substringAfterLast(" - ").lowercase()
        } else ""

        // 1. Check for .bd country-code top-level domain
        if (linkLower.contains(".bd/") || linkLower.endsWith(".bd") ||
            sourceLower.endsWith(".bd") || titleTailLower.endsWith(".bd")
        ) {
            return true
        }

        return BD_MEDIA_MARKERS.any { marker ->
            sourceLower.contains(marker) || linkLower.contains(marker) || titleTailLower.contains(marker)
        }
    }

    private fun isBlockedNews(article: NewsArticle): Boolean {
        val source = article.source.lowercase()
        val link = article.link.lowercase()
        val title = article.title.lowercase()
        return source.contains("vietnam.vn") || source.contains("vietnam") ||
            link.contains("vietnam.vn") || link.contains("vietnam") ||
            title.contains("vietnam.vn")
    }

    private fun isSportsNews(article: NewsArticle): Boolean {
        return SPORTS_KEYWORDS.any {
            article.title.contains(it, ignoreCase = true) ||
                article.source.contains(it, ignoreCase = true)
        }
    }

    fun toggleBookmark(article: NewsArticle) {
        viewModelScope.launch(Dispatchers.IO) {
            val newState = !article.isSaved
            database.newsDao().insertAll(listOf(article.copy(isSaved = newState)))
            database.newsDao().updateSavedStatus(article.link, newState)
            try {
                firebaseManager.syncSavedNews(article, newState)
            } catch (e: Exception) {
                // Ignore firebase error
            }

            val saved = database.newsDao().getSavedNews().sortedByDescending { it.timestamp }
            _uiState.update { state ->
                val updateList = { list: List<NewsArticle> ->
                    list.map { if (it.link == article.link) it.copy(isSaved = newState) else it }
                }
                state.copy(
                    bgbNews = updateList(state.bgbNews),
                    nationalNews = updateList(state.nationalNews),
                    indianNews = updateList(state.indianNews),
                    myanmarNews = updateList(state.myanmarNews),
                    internationalNews = updateList(state.internationalNews),
                    rssNews = updateList(state.rssNews),
                    facebookNews = updateList(state.facebookNews),
                    latestNews = updateList(state.latestNews),
                    savedNews = saved
                )
            }
        }
    }

    private fun startAutoUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(5 * 60 * 1000) // Auto-refresh every 5 minutes silently
                if (_uiState.value.isAutoUpdating) {
                    fetchBgbNews()
                    fetchIndianNews()
                    fetchMyanmarNews()
                    fetchInternationalNews()
                    fetchCustomRssNews()
                    fetchVideos()
                }
            }
        }
    }

    fun toggleAutoUpdate() {
        _uiState.update { it.copy(isAutoUpdating = !it.isAutoUpdating) }
    }

    fun selectBottomTab(index: Int) {
        _uiState.update { it.copy(selectedBottomTab = index) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index, selectedFilter = "সকল") }
    }

    fun addRssFeed(title: String, url: String) {
        if (url.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                keywordPrefs.addRssFeed(title, url)
                _uiState.update { it.copy(rssFeeds = keywordPrefs.getRssFeeds(), isRefreshing = true) }
                fetchCustomRssNews()
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun removeRssFeed(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            keywordPrefs.removeRssFeed(url)
            val updatedFeeds = keywordPrefs.getRssFeeds()
            _uiState.update { it.copy(rssFeeds = updatedFeeds) }
            fetchCustomRssNews()
        }
    }

    fun removeKeyword(keyword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            keywordPrefs.removeKeyword(keyword)
            _uiState.update { it.copy(keywords = keywordPrefs.getKeywords()) }
            fetchAllNews()
        }
    }

    fun addKeyword(keyword: String) {
        if (keyword.isNotBlank() && !_uiState.value.keywords.contains(keyword)) {
            viewModelScope.launch(Dispatchers.IO) {
                keywordPrefs.addKeyword(keyword)
                _uiState.update {
                    it.copy(keywords = keywordPrefs.getKeywords())
                }
                fetchAllNews()
            }
        }
    }

    fun updateYoutubeApiKey(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            keywordPrefs.saveYoutubeApiKey(key)
            _uiState.update { it.copy(youtubeApiKey = key) }
            fetchVideos()
        }
    }

    fun updateYoutubeChannelId(channelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            keywordPrefs.saveYoutubeChannelId(channelId)
            _uiState.update { it.copy(youtubeChannelId = channelId) }
            fetchVideos()
        }
    }

    fun clearAppCache(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                resolvedUrlCache.clear()
                database.newsDao().deleteOldNews(System.currentTimeMillis() + 1000L)
                getApplication<Application>().cacheDir.deleteRecursively()
            } catch (_: Exception) {}
            loadNewsFromDatabase()
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete()
            }
            fetchAllNews(showFullScreenLoader = false)
        }
    }

    private suspend fun fetchVideos() {
        try {
            val key = keywordPrefs.getYoutubeApiKey()
            val channelIds = keywordPrefs.getYoutubeChannelId()
            val keywords = _uiState.value.keywords
            when (val result = youtubeRepository.fetchBgbVideos(keywords, key, channelIds)) {
                is com.example.data.YouTubeFetchResult.Success -> {
                    _uiState.update { it.copy(bgbVideos = result.videos, videoErrorMessage = null) }
                }
                is com.example.data.YouTubeFetchResult.Error -> {
                    _uiState.update { state ->
                        if (state.bgbVideos.isEmpty()) {
                            state.copy(videoErrorMessage = result.message)
                        } else {
                            state
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { state ->
                if (state.bgbVideos.isEmpty()) {
                    state.copy(videoErrorMessage = "ভিডিও লোড করতে সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}")
                } else state
            }
        }
    }
}

/**
 * Identifies the search medium (Google, Bing, Yahoo, DuckDuckGo, Reddit, ফেসবুক)
 * for any NewsArticle based on its source prefix, category, or feed link.
 */
fun getSearchMedium(article: NewsArticle): String {
    val src = article.source.trim().lowercase()
    val lnk = article.link.lowercase()
    return when {
        article.category == "facebook" ||
            src.startsWith("facebook") ||
            src.startsWith("fb/") ||
            src.contains("সোশ্যাল ফিড") ||
            lnk.contains("facebook.com") ||
            lnk.contains("fb.com") -> "ফেসবুক"
        src.startsWith("bing") || src.contains("bing news") || lnk.contains("bing.com") -> "Bing"
        src.startsWith("yahoo") || src.contains("yahoo news") || lnk.contains("yahoo.com") -> "Yahoo"
        src.startsWith("duckduckgo") || lnk.contains("duckduckgo.com") -> "DuckDuckGo"
        src.startsWith("reddit") || lnk.contains("reddit.com") -> "Reddit"
        else -> "Google"
    }
}