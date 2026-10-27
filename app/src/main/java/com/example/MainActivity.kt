package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.NewsArticle
import com.example.ui.InAppSocialWebViewDialog
import com.example.ui.NewsViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: NewsViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            var speakingArticleLink by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    scheduleBackgroundNewsSync(applicationContext)
                }
                if (tts == null) {
                    tts = TextToSpeech(applicationContext) { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            val bnLocale = Locale("bn", "BD")
                            val res = tts?.setLanguage(bnLocale)
                            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                                tts?.setLanguage(Locale.getDefault())
                            }
                        }
                    }
                }
            }

            MyApplicationTheme(darkTheme = uiState.isDarkMode) {
                var showYouTubeSettingsDialog by remember { mutableStateOf(false) }
                var activeReaderArticle by remember { mutableStateOf<NewsArticle?>(null) }

                val speakArticle: (NewsArticle) -> Unit = { article ->
                    if (speakingArticleLink == article.link) {
                        tts?.stop()
                        speakingArticleLink = null
                    } else {
                        speakingArticleLink = article.link
                        val speechText = "${article.source} জানাচ্ছে: ${article.title}"
                        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, article.link)
                        Toast.makeText(this@MainActivity, "🔊 সংবাদটি পড়ে শোনানো হচ্ছে...", Toast.LENGTH_SHORT).show()
                    }
                }

                if (showYouTubeSettingsDialog) {
                    com.example.ui.YouTubeSettingsDialog(
                        currentApiKey = uiState.youtubeApiKey,
                        currentChannelId = uiState.youtubeChannelId,
                        onDismiss = { showYouTubeSettingsDialog = false },
                        onSave = { apiKey, channelId ->
                            viewModel.updateYoutubeApiKey(apiKey)
                            viewModel.updateYoutubeChannelId(channelId)
                            Toast.makeText(this@MainActivity, "ইউটিউব সেটিংস সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (activeReaderArticle != null) {
                    val article = activeReaderArticle!!
                    val link = article.originalLink ?: article.link
                    InAppSocialWebViewDialog(
                        url = link,
                        title = article.title,
                        onDismiss = { activeReaderArticle = null },
                        onOpenExternal = { extUrl ->
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(extUrl)))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }

                val handleCopyLink: (NewsArticle) -> Unit = { article ->
                    viewModel.resolveAndCopyLink(article) { finalUrl ->
                        val clipboard = this@MainActivity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("News Link", finalUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this@MainActivity, "✅ খবরের মূল লিংক কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }

                val handleWhatsAppShare: (NewsArticle) -> Unit = { article ->
                    viewModel.resolveAndCopyLink(article) { finalUrl ->
                        val shareText = "${article.title}\n$finalUrl"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        try {
                            this@MainActivity.startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            this@MainActivity.startActivity(Intent.createChooser(fallbackIntent, "শেয়ার করুন"))
                        }
                    }
                }

                val handleArticleClick: (NewsArticle) -> Unit = { article ->
                    viewModel.markArticleVisited(article.link)
                    activeReaderArticle = article
                }

                androidx.activity.compose.BackHandler(enabled = uiState.selectedBottomTab != 0 || uiState.selectedTab != 0) {
                    if (uiState.selectedBottomTab != 0) {
                        viewModel.selectBottomTab(0)
                    } else {
                        viewModel.selectTab(0)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        BottomNavBar(
                            selectedTab = uiState.selectedBottomTab,
                            savedCount = uiState.savedNews.size,
                            rssCount = uiState.rssFeeds.size,
                            onTabSelected = { viewModel.selectBottomTab(it) }
                        )
                    },
                    contentWindowInsets = ScaffoldDefaults.contentWindowInsets
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        if (uiState.selectedBottomTab == 0) {
                            Header(
                                query = uiState.searchQuery,
                                isRefreshing = uiState.isRefreshing,
                                isDarkMode = uiState.isDarkMode,
                                areFilterChipsVisible = uiState.areFilterChipsVisible,
                                onToggleFilterChips = { viewModel.toggleFilterChipsVisibility() },
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                keywords = uiState.keywords,
                                onRefreshClick = { viewModel.refreshNews() },
                                onOpenYouTubeSettings = { showYouTubeSettingsDialog = true }
                            )

                            val bdCombined = remember(uiState.latestNews, uiState.bgbNews) {
                                uiState.latestNews.ifEmpty { uiState.bgbNews }
                            }
                            val rawCategoryArticles = when (uiState.selectedTab) {
                                1 -> uiState.indianNews
                                2 -> uiState.myanmarNews
                                3 -> uiState.internationalNews
                                else -> bdCombined
                            }

                            val bdCount = remember(bdCombined) {
                                bdCombined.distinctBy { it.title.trim().lowercase() }.size
                            }
                            val indianCount = remember(uiState.indianNews) {
                                uiState.indianNews.distinctBy { it.title.trim().lowercase() }.size
                            }
                            val myanmarCount = remember(uiState.myanmarNews) {
                                uiState.myanmarNews.distinctBy { it.title.trim().lowercase() }.size
                            }
                            val internationalCount = remember(uiState.internationalNews) {
                                uiState.internationalNews.distinctBy { it.title.trim().lowercase() }.size
                            }

                            val dedupedCategoryArticles = remember(rawCategoryArticles) {
                                rawCategoryArticles.distinctBy { it.title.trim().lowercase() }
                            }
                            val unreadCount = remember(dedupedCategoryArticles, uiState.visitedLinks) {
                                dedupedCategoryArticles.count { it.link !in uiState.visitedLinks }
                            }
                            val readCount = remember(dedupedCategoryArticles, uiState.visitedLinks) {
                                dedupedCategoryArticles.count { it.link in uiState.visitedLinks }
                            }
                            val googleArticles = remember(rawCategoryArticles) {
                                rawCategoryArticles.filter { com.example.ui.getSearchMedium(it) == "Google" }
                            }
                            val bingArticles = remember(rawCategoryArticles) {
                                rawCategoryArticles.filter { com.example.ui.getSearchMedium(it) == "Bing" }
                            }
                            val yahooArticles = remember(rawCategoryArticles) {
                                rawCategoryArticles.filter { com.example.ui.getSearchMedium(it) == "Yahoo" }
                            }
                            val ddgArticles = remember(rawCategoryArticles) {
                                rawCategoryArticles.filter { com.example.ui.getSearchMedium(it) == "DuckDuckGo" }
                            }
                            val redditArticles = remember(rawCategoryArticles) {
                                rawCategoryArticles.filter { com.example.ui.getSearchMedium(it) == "Reddit" }
                            }

                            androidx.compose.animation.AnimatedVisibility(visible = uiState.areFilterChipsVisible) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    NewsCategoryTabBar(
                                        selectedTab = uiState.selectedTab,
                                        bdCount = bdCount,
                                        indianCount = indianCount,
                                        myanmarCount = myanmarCount,
                                        internationalCount = internationalCount,
                                        onSelectTab = { viewModel.selectTab(it) }
                                    )

                                    FilterChipsBar(
                                        selectedFilter = uiState.selectedFilter,
                                        totalCount = dedupedCategoryArticles.size,
                                        unreadCount = unreadCount,
                                        readCount = readCount,
                                        googleCount = googleArticles.size,
                                        bingCount = bingArticles.size,
                                        yahooCount = yahooArticles.size,
                                        ddgCount = ddgArticles.size,
                                        redditCount = redditArticles.size,
                                        fbCount = uiState.facebookNews.size,
                                        onSelectFilter = { filter -> viewModel.selectFilter(filter) }
                                    )
                                }
                            }

                            if (uiState.isLoading && rawCategoryArticles.isEmpty()) {
                                SkeletonList()
                            } else {
                                val filteredList = remember(
                                    uiState.selectedFilter,
                                    uiState.searchQuery,
                                    dedupedCategoryArticles,
                                    uiState.visitedLinks,
                                    googleArticles,
                                    bingArticles,
                                    yahooArticles,
                                    ddgArticles,
                                    redditArticles,
                                    uiState.facebookNews,
                                    uiState.savedNews
                                ) {
                                    val baseList = when (uiState.selectedFilter) {
                                        "সকল" -> dedupedCategoryArticles
                                        "অপঠিত" -> dedupedCategoryArticles.filter { it.link !in uiState.visitedLinks }
                                        "পঠিত" -> dedupedCategoryArticles.filter { it.link in uiState.visitedLinks }
                                        "Google" -> googleArticles
                                        "Bing" -> bingArticles
                                        "Yahoo" -> yahooArticles
                                        "DuckDuckGo" -> ddgArticles
                                        "Reddit" -> redditArticles
                                        "ফেসবুক", "সোশ্যাল ফিড", "ফেসবুক/সোশ্যাল" -> uiState.facebookNews
                                        "সংরক্ষিত" -> uiState.savedNews
                                        else -> dedupedCategoryArticles.filter {
                                            it.title.contains(uiState.selectedFilter, ignoreCase = true) ||
                                                it.matchedKeyword.equals(uiState.selectedFilter, ignoreCase = true)
                                        }
                                    }

                                    if (uiState.searchQuery.isNotBlank()) {
                                        baseList.filter {
                                            it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                                                it.source.contains(uiState.searchQuery, ignoreCase = true)
                                        }
                                    } else {
                                        baseList
                                    }
                                }

                                Column(modifier = Modifier.fillMaxSize()) {
                                    NewsListWithRefresh(
                                        articles = filteredList,
                                        heroArticles = if (uiState.selectedFilter == "সকল" && uiState.searchQuery.isBlank()) {
                                            filteredList.take(4)
                                        } else emptyList(),
                                        isBgbTab = true,
                                        isRefreshing = uiState.isRefreshing,
                                        visitedLinks = uiState.visitedLinks,
                                        speakingLink = speakingArticleLink,
                                        keywords = uiState.keywords,
                                        onRefresh = { viewModel.refreshNews() },
                                        onArticleClick = handleArticleClick,
                                        onSpeakClick = speakArticle,
                                        onCopyClick = handleCopyLink,
                                        onWhatsAppClick = handleWhatsAppShare,
                                        onBookmarkClick = { article ->
                                            val willSave = !article.isSaved
                                            viewModel.toggleBookmark(article)
                                            Toast.makeText(
                                                this@MainActivity,
                                                if (willSave) "সংবাদটি সংরক্ষিত হয়েছে" else "সংরক্ষিত তালিকা থেকে সরানো হয়েছে",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            }
                        } else if (uiState.selectedBottomTab == 1) {
                            Header(
                                query = uiState.searchQuery,
                                isRefreshing = uiState.isRefreshing,
                                isDarkMode = uiState.isDarkMode,
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onQueryChange = { viewModel.updateSearchQuery(it) },
                                keywords = uiState.keywords,
                                onRefreshClick = { viewModel.refreshNews() },
                                onOpenYouTubeSettings = { showYouTubeSettingsDialog = true }
                            )
                            val filteredVideos = if (uiState.searchQuery.isNotBlank()) {
                                uiState.bgbVideos.filter {
                                    (it.snippet?.title?.contains(uiState.searchQuery, ignoreCase = true) == true) ||
                                        (it.snippet?.channelTitle?.contains(uiState.searchQuery, ignoreCase = true) == true)
                                }
                            } else {
                                uiState.bgbVideos
                            }
                            if (uiState.isLoading && filteredVideos.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                com.example.ui.VideoList(
                                    videos = filteredVideos,
                                    errorMessage = uiState.videoErrorMessage,
                                    onRetry = { viewModel.refreshNews() }
                                )
                            }
                        } else if (uiState.selectedBottomTab == 2) {
                            RssFeedScreen(
                                rssFeeds = uiState.rssFeeds,
                                rssArticles = uiState.rssNews,
                                keywords = uiState.keywords,
                                isRefreshing = uiState.isRefreshing,
                                visitedLinks = uiState.visitedLinks,
                                speakingLink = speakingArticleLink,
                                onAddRssFeed = { title, url ->
                                    viewModel.addRssFeed(title, url)
                                    Toast.makeText(this@MainActivity, "✅ আরএসএস ফিড যুক্ত করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                onRemoveRssFeed = { url ->
                                    viewModel.removeRssFeed(url)
                                    Toast.makeText(this@MainActivity, "আরএসএস ফিড সরানো হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                onOpenSettings = { viewModel.selectBottomTab(5) },
                                onRefresh = { viewModel.refreshNews() },
                                onArticleClick = handleArticleClick,
                                onSpeakClick = speakArticle,
                                onCopyClick = handleCopyLink,
                                onWhatsAppClick = handleWhatsAppShare,
                                onBookmarkClick = { article ->
                                    val willSave = !article.isSaved
                                    viewModel.toggleBookmark(article)
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (willSave) "সংবাদটি সংরক্ষিত হয়েছে" else "সংরক্ষিত তালিকা থেকে সরানো হয়েছে",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        } else if (uiState.selectedBottomTab == 3) {
                            com.example.ui.ProfileContent(
                                keywords = uiState.keywords,
                                facebookNews = uiState.facebookNews,
                                onArticleClick = { article -> viewModel.markArticleVisited(article.link) },
                                onCopyClick = handleCopyLink,
                                onWhatsAppClick = handleWhatsAppShare,
                                onBookmarkClick = { article -> viewModel.toggleBookmark(article) }
                            )
                        } else if (uiState.selectedBottomTab == 4) {
                            SavedNewsScreen(
                                savedArticles = uiState.savedNews,
                                visitedLinks = uiState.visitedLinks,
                                speakingLink = speakingArticleLink,
                                keywords = uiState.keywords,
                                onArticleClick = handleArticleClick,
                                onSpeakClick = speakArticle,
                                onCopyClick = handleCopyLink,
                                onWhatsAppClick = handleWhatsAppShare,
                                onBookmarkClick = { article ->
                                    viewModel.toggleBookmark(article)
                                    Toast.makeText(this@MainActivity, "সংরক্ষিত তালিকা থেকে সরানো হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else if (uiState.selectedBottomTab == 5) {
                            com.example.ui.SettingsContent(
                                keywords = uiState.keywords,
                                rssFeeds = uiState.rssFeeds,
                                youtubeApiKey = uiState.youtubeApiKey,
                                youtubeChannelId = uiState.youtubeChannelId,
                                onAddKeyword = { viewModel.addKeyword(it) },
                                onRemoveKeyword = { viewModel.removeKeyword(it) },
                                onAddRssFeed = { title, url ->
                                    viewModel.addRssFeed(title, url)
                                    Toast.makeText(this@MainActivity, "✅ আরএসএস ফিড যুক্ত করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                onRemoveRssFeed = { url ->
                                    viewModel.removeRssFeed(url)
                                    Toast.makeText(this@MainActivity, "আরএসএস ফিড সরানো হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                onSaveApiKey = { viewModel.updateYoutubeApiKey(it) },
                                onSaveChannelId = { viewModel.updateYoutubeChannelId(it) },
                                onClearCache = {
                                    viewModel.clearAppCache {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "✨ ক্যাশ ও পুরনো ডাটা সফলভাবে পরিষ্কার করা হয়েছে!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun BreakingNewsHeroCarousel(
    articles: List<NewsArticle>,
    keywords: List<String>,
    onArticleClick: (NewsArticle) -> Unit
) {
    if (articles.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { articles.size })
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val article = articles[page]
            val badgeTag = getDisplayKeyword(article, keywords)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF063420),
                                Color(0xFF0E2238),
                                Color(0xFF141B29)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.6f),
                                Color(0xFF38BDF8).copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onArticleClick(article) }
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Live Pulsing Badge
                            Surface(
                                color = Color(0xFFE11D48).copy(alpha = 0.25f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(Color(0xFFFF4D6D).copy(alpha = pulseAlpha), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "শীর্ষ সংবাদ",
                                        color = Color(0xFFFF8FA3),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = badgeTag,
                                    color = Primary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "${page + 1}/${articles.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = article.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = article.source,
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatRelativeTime(article.timestamp, article.pubDate),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewsCategoryTabBar(
    selectedTab: Int,
    bdCount: Int,
    indianCount: Int,
    myanmarCount: Int,
    internationalCount: Int,
    onSelectTab: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val tabs = listOf(
        Triple(0, "বাংলাদেশ নিউজ", bdCount),
        Triple(1, "ভারতীয় নিউজ", indianCount),
        Triple(2, "মায়ানমার নিউজ", myanmarCount),
        Triple(3, "ইন্টারন্যাশনাল নিউজ", internationalCount)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(scheme.surfaceVariant, RoundedCornerShape(16.dp))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
            .horizontalScroll(scrollState)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (index, label, count) ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) scheme.primary else Color.Transparent)
                    .clickable { onSelectTab(index) }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) scheme.onPrimary else scheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = "$count টি সংবাদ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) scheme.onPrimary.copy(alpha = 0.85f) else scheme.outline,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChipsBar(
    selectedFilter: String,
    totalCount: Int,
    unreadCount: Int,
    readCount: Int,
    googleCount: Int,
    bingCount: Int,
    yahooCount: Int,
    ddgCount: Int,
    redditCount: Int,
    fbCount: Int,
    onSelectFilter: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val mainFilters = listOf(
        "সকল" to "সকল ($totalCount)",
        "অপঠিত" to "অপঠিত ($unreadCount)",
        "পঠিত" to "পঠিত ($readCount)",
        "Google" to "Google ($googleCount)",
        "Bing" to "Bing ($bingCount)",
        "Yahoo" to "Yahoo ($yahooCount)",
        "DuckDuckGo" to "DuckDuckGo ($ddgCount)",
        "Reddit" to "Reddit ($redditCount)",
        "ফেসবুক" to "ফেসবুক ($fbCount)"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        mainFilters.forEach { (key, label) ->
            val isSelected = selectedFilter == key
            FilterChipItem(
                label = label,
                isSelected = isSelected,
                onClick = { onSelectFilter(key) }
            )
        }
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = if (isSelected) scheme.primary else scheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) scheme.primary else scheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) scheme.onPrimary else scheme.onBackground,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SavedNewsScreen(
    savedArticles: List<NewsArticle>,
    visitedLinks: Set<String>,
    speakingLink: String? = null,
    keywords: List<String>,
    onArticleClick: (NewsArticle) -> Unit,
    onSpeakClick: (NewsArticle) -> Unit = {},
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "সংরক্ষিত সংবাদ (Bookmarks)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "আপনার সেভ করা গুরুত্বপূর্ণ সংবাদসমূহ",
                    fontSize = 13.sp,
                    color = scheme.outline
                )
            }
            Surface(
                color = scheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${savedArticles.size}টি সেভ করা",
                    color = scheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (savedArticles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = scheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "কোনো সংরক্ষিত সংবাদ নেই",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "যেকোনো সংবাদের বুকমার্ক আইকনে ট্যাপ করে এখানে সেভ করে রাখুন",
                        fontSize = 13.sp,
                        color = scheme.outline
                    )
                }
            }
        } else {
            val savedListState = androidx.compose.foundation.lazy.rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = savedListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(savedArticles, key = { it.link }) { article ->
                        val isVisited = article.link in visitedLinks
                        BgbSpecialNewsCard(
                            article = article,
                            isVisited = isVisited,
                            isSpeaking = speakingLink == article.link,
                            keywords = keywords,
                            onArticleClick = onArticleClick,
                            onSpeakClick = onSpeakClick,
                            onCopyClick = onCopyClick,
                            onWhatsAppClick = onWhatsAppClick,
                            onBookmarkClick = onBookmarkClick
                        )
                    }
                }
                if (savedArticles.size > 1) {
                    SmartTwoStepJumpButton(
                        listState = savedListState,
                        totalItemsCount = savedArticles.size,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NewsSkeletonCard() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceVariant, RoundedCornerShape(20.dp))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(60.dp).height(20.dp).background(scheme.outlineVariant, RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(100.dp).height(12.dp).background(scheme.outlineVariant, RoundedCornerShape(4.dp)))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(scheme.outlineVariant, RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).background(scheme.outlineVariant, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
fun SkeletonList() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(5) {
            NewsSkeletonCard()
        }
    }
}

@Composable
fun Header(
    query: String,
    isRefreshing: Boolean = false,
    isDarkMode: Boolean = true,
    areFilterChipsVisible: Boolean = true,
    onToggleFilterChips: (() -> Unit)? = null,
    onToggleDarkMode: (() -> Unit)? = null,
    onQueryChange: (String) -> Unit,
    keywords: List<String>,
    onRefreshClick: (() -> Unit)? = null,
    onOpenYouTubeSettings: (() -> Unit)? = null
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var expandedDropdown by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp)
                .background(scheme.surfaceVariant, RoundedCornerShape(28.dp))
                .border(1.dp, scheme.outlineVariant, RoundedCornerShape(28.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearchExpanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = { isSearchExpanded = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = scheme.onBackground)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("খবর খুঁজুন...", color = scheme.outline)
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            textStyle = androidx.compose.ui.text.TextStyle(color = scheme.onBackground, fontSize = 16.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = scheme.outline)
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(scheme.primary, Color(0xFF00B4D8))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "সীমান্ত সংবাদ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onBackground,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isSearchExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = scheme.primary)
                    }

                    if (onToggleFilterChips != null) {
                        IconButton(
                            onClick = onToggleFilterChips,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (areFilterChipsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (areFilterChipsVisible) "ফিল্টার চিপস লুকান" else "ফিল্টার চিপস দেখুন",
                                tint = if (areFilterChipsVisible) scheme.primary else scheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (onToggleDarkMode != null) {
                        IconButton(
                            onClick = onToggleDarkMode,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDarkMode) Color(0xFFFBBF24) else scheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (onRefreshClick != null) {
                        IconButton(
                            onClick = onRefreshClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = scheme.primary)
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Keywords", tint = scheme.primary)
                        }
                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("সকল নিউজ") },
                                onClick = {
                                    onQueryChange("")
                                    expandedDropdown = false
                                }
                            )
                            keywords.forEach { keyword ->
                                DropdownMenuItem(
                                    text = { Text(keyword) },
                                    onClick = {
                                        onQueryChange(keyword)
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (onOpenYouTubeSettings != null) {
                        IconButton(
                            onClick = onOpenYouTubeSettings,
                            modifier = Modifier
                                .size(36.dp)
                                .background(scheme.primary.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "ইউটিউব সেটিংস",
                                tint = scheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(2.dp),
                color = scheme.primary
            )
        }
    }
}

fun getDisplayKeyword(article: NewsArticle, keywords: List<String> = emptyList()): String {
    if (article.matchedKeyword.isNotBlank()) {
        return article.matchedKeyword
    }
    val searchList = keywords + listOf(
        "বিজিবি", "বর্ডার গার্ড বাংলাদেশ", "BGB", "বিএসএফ", "BSF",
        "সীমান্ত", "বাংলাদেশ ভারত সীমান্ত", "সীমান্ত সংবাদ", "বাংলাদেশ", "নিরাপত্তা"
    )
    for (kw in searchList) {
        if (kw.isNotBlank() && article.title.contains(kw, ignoreCase = true)) {
            return kw
        }
    }
    return if (article.category == "bgb") "বিজিবি" else "জাতীয়"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListWithRefresh(
    articles: List<NewsArticle>,
    heroArticles: List<NewsArticle> = emptyList(),
    isBgbTab: Boolean,
    isRefreshing: Boolean,
    visitedLinks: Set<String>,
    speakingLink: String? = null,
    keywords: List<String> = emptyList(),
    onRefresh: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onSpeakClick: (NewsArticle) -> Unit = {},
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit = {}
) {
    var visibleLimit by remember { mutableIntStateOf(30) }
    val scheme = MaterialTheme.colorScheme
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(articles) {
        if (articles.size <= 30) {
            visibleLimit = 30
        }
    }

    val visibleArticles = articles.take(visibleLimit)
    val hasMore = articles.size > visibleLimit
    val heroCount = if (heroArticles.isNotEmpty()) 1 else 0
    val totalTargetItems = heroCount + articles.size

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            visibleLimit = 30
            onRefresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (articles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = scheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("কোনো সংবাদ পাওয়া যায়নি", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("অন্য ফিল্টার নির্বাচন করুন বা রিফ্রেশ করুন", fontSize = 14.sp, color = scheme.outline)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 84.dp)
                ) {
                    if (heroArticles.isNotEmpty()) {
                        item(key = "hero_carousel") {
                            BreakingNewsHeroCarousel(
                                articles = heroArticles,
                                keywords = keywords,
                                onArticleClick = onArticleClick
                            )
                        }
                    }

                    items(visibleArticles, key = { it.link + it.timestamp }) { article ->
                        val isVisited = article.link in visitedLinks
                        BgbSpecialNewsCard(
                            article = article,
                            isVisited = isVisited,
                            isSpeaking = speakingLink == article.link,
                            keywords = keywords,
                            onArticleClick = onArticleClick,
                            onSpeakClick = onSpeakClick,
                            onCopyClick = onCopyClick,
                            onWhatsAppClick = onWhatsAppClick,
                            onBookmarkClick = onBookmarkClick
                        )
                    }

                    if (hasMore) {
                        item {
                            LaunchedEffect(visibleLimit) {
                                delay(150)
                                visibleLimit += 30
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    color = scheme.primary,
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }
                }

                if (articles.size > 1) {
                    SmartTwoStepJumpButton(
                        listState = listState,
                        totalItemsCount = totalTargetItems,
                        onPrepareFullList = {
                            if (visibleLimit < articles.size) {
                                visibleLimit = articles.size
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 14.dp)
                    )
                }
            }
        }
    }
}

enum class JumpStage {
    TOP,
    MIDDLE,
    BOTTOM
}

@Composable
fun SmartTwoStepJumpButton(
    listState: androidx.compose.foundation.lazy.LazyListState,
    totalItemsCount: Int,
    onPrepareFullList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()
    var jumpStage by remember { mutableStateOf(JumpStage.TOP) }
    var isProgrammaticJump by remember { mutableStateOf(false) }

    // Keep stage in sync if the user manually scrolls all the way to the top or bottom
    LaunchedEffect(
        listState.isScrollInProgress,
        listState.canScrollBackward,
        listState.canScrollForward
    ) {
        if (!isProgrammaticJump && listState.isScrollInProgress) {
            if (!listState.canScrollBackward) {
                jumpStage = JumpStage.TOP
            } else if (!listState.canScrollForward && listState.canScrollBackward) {
                jumpStage = JumpStage.BOTTOM
            }
        }
    }

    fun executeJump(nextStage: JumpStage, targetIndexProvider: () -> Int) {
        jumpStage = nextStage
        onPrepareFullList()
        coroutineScope.launch {
            isProgrammaticJump = true
            try {
                delay(35)
                val targetIndex = targetIndexProvider().coerceAtLeast(0)
                val currentIndex = listState.firstVisibleItemIndex
                if (kotlin.math.abs(targetIndex - currentIndex) > 12) {
                    val snapIndex = if (targetIndex > currentIndex) {
                        (targetIndex - 6).coerceAtLeast(0)
                    } else {
                        targetIndex + 6
                    }
                    listState.scrollToItem(snapIndex)
                }
                listState.animateScrollToItem(targetIndex)
            } finally {
                delay(120)
                isProgrammaticJump = false
            }
        }
    }

    Box(modifier = modifier) {
        when (jumpStage) {
            JumpStage.TOP -> {
                // Step 1 at Top: Single Down Arrow -> Jumps to Middle and splits into Up & Down arrows
                Surface(
                    onClick = {
                        executeJump(JumpStage.MIDDLE) {
                            (totalItemsCount / 2).coerceAtLeast(1)
                        }
                    },
                    shape = CircleShape,
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.5.dp, scheme.onPrimary.copy(alpha = 0.25f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "মাঝখানে যান",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            JumpStage.MIDDLE -> {
                // Step 2 in Middle: Two Arrows (Up -> Top, Down -> Bottom)
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = scheme.surfaceVariant.copy(alpha = 0.96f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .border(1.5.dp, scheme.primary.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        // Up Arrow -> Jump to Top
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(scheme.primary.copy(alpha = 0.18f))
                                .clickable {
                                    executeJump(JumpStage.TOP) { 0 }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "উপরে যান",
                                tint = scheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            modifier = Modifier.width(24.dp),
                            thickness = 1.dp,
                            color = scheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Down Arrow -> Jump to Bottom
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(scheme.primary)
                                .clickable {
                                    executeJump(JumpStage.BOTTOM) {
                                        (totalItemsCount - 1).coerceAtLeast(0)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "নিচে যান",
                                tint = scheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            JumpStage.BOTTOM -> {
                // At Bottom: Single Up Arrow -> Jumps DIRECTLY to Top (skipping middle)
                Surface(
                    onClick = {
                        executeJump(JumpStage.TOP) { 0 }
                    },
                    shape = CircleShape,
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.5.dp, scheme.onPrimary.copy(alpha = 0.25f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "সরাসরি উপরে যান",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BgbSpecialNewsCard(
    article: NewsArticle,
    isVisited: Boolean,
    isSpeaking: Boolean = false,
    keywords: List<String> = emptyList(),
    onArticleClick: (NewsArticle) -> Unit,
    onSpeakClick: (NewsArticle) -> Unit = {},
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val accentColor = GlowColors[Math.abs(article.title.hashCode()) % GlowColors.size]
    val titleColor = if (isVisited) scheme.outline else scheme.onBackground
    val badgeTag = getDisplayKeyword(article, keywords)
    val isFacebookSource = article.source.contains("Facebook", ignoreCase = true) ||
        article.source.contains("FB/", ignoreCase = true)

    // Lightweight, 120Hz smooth card without GPU shadow overdraw
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(scheme.surface)
            .border(
                width = 1.dp,
                color = if (isSpeaking) scheme.primary else accentColor.copy(alpha = 0.28f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onArticleClick(article) }
            .height(IntrinsicSize.Min)
    ) {
        // Left Color Accent Strip
        Box(
            modifier = Modifier
                .width(5.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Source Monogram Avatar
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(accentColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = article.source.trim().take(1).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = article.source,
                        color = if (isFacebookSource) Color(0xFF38BDF8) else scheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badgeTag,
                        color = scheme.onErrorContainer,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(scheme.errorContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                    if (isVisited) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "পঠিত",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onBookmarkClick(article) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (article.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Bookmark",
                        tint = if (article.isSaved) scheme.primary else scheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = article.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    lineHeight = 23.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (!article.imageUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(10.dp))
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = scheme.outline,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatRelativeTime(article.timestamp, article.pubDate),
                        color = scheme.outline,
                        fontSize = 11.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Audio Listen (TTS) Button
                    IconButton(
                        onClick = { onSpeakClick(article) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                            contentDescription = "শুনুন",
                            tint = if (isSpeaking) Color(0xFFFF5252) else scheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = { onWhatsAppClick(article) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Share to WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { onCopyClick(article) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = scheme.onBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegularNewsCard(
    article: NewsArticle,
    isVisited: Boolean,
    keywords: List<String> = emptyList(),
    onArticleClick: (NewsArticle) -> Unit,
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit = {}
) {
    BgbSpecialNewsCard(
        article = article,
        isVisited = isVisited,
        keywords = keywords,
        onArticleClick = onArticleClick,
        onCopyClick = onCopyClick,
        onWhatsAppClick = onWhatsAppClick,
        onBookmarkClick = onBookmarkClick
    )
}

@Composable
fun RssFeedScreen(
    rssFeeds: List<com.example.data.RssFeedItem>,
    rssArticles: List<NewsArticle>,
    keywords: List<String>,
    isRefreshing: Boolean,
    visitedLinks: Set<String>,
    speakingLink: String? = null,
    onAddRssFeed: (String, String) -> Unit,
    onRemoveRssFeed: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onSpeakClick: (NewsArticle) -> Unit = {},
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var selectedFeedFilter by remember { mutableStateOf("সকল ফিড") }
    var showInlineAddBox by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

    val filteredArticles = remember(rssArticles, selectedFeedFilter, keywords) {
        when (selectedFeedFilter) {
            "সকল ফিড" -> rssArticles
            "কিওয়ার্ড ম্যাচিং" -> rssArticles.filter { article ->
                keywords.any { kw ->
                    kw.isNotBlank() && (
                        article.title.contains(kw, ignoreCase = true) ||
                            article.source.contains(kw, ignoreCase = true)
                        )
                }
            }
            else -> rssArticles.filter {
                it.matchedKeyword.equals(selectedFeedFilter, ignoreCase = true) ||
                    it.source.contains(selectedFeedFilter, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = scheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.RssFeed,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "আরএসএস ফিড",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Text(
                        text = "${rssFeeds.size}টি ফিড • ${rssArticles.size}টি সংবাদ",
                        fontSize = 12.sp,
                        color = scheme.outline
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = scheme.primary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalButton(
                    onClick = { showInlineAddBox = !showInlineAddBox },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (showInlineAddBox) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ফিড যোগ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isRefreshing) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(2.dp),
                color = scheme.primary
            )
        }

        // Inline Add Box or when no feeds are added yet
        if (showInlineAddBox || rssFeeds.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "নতুন আরএসএস (RSS) ফিড যোগ করুন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ফিডের নাম (ঐচ্ছিক, যেমন: BBC Bangla)", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newUrl,
                            onValueChange = { newUrl = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("RSS লিংক (https://.../rss.xml)", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newUrl.isNotBlank()) {
                                    onAddRssFeed(newTitle, newUrl)
                                    newTitle = ""
                                    newUrl = ""
                                    showInlineAddBox = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("যোগ করুন", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "জনপ্রিয় ফিড এক ক্লিকে যোগ করুন:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.outline
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.SuggestedRssFeeds.forEach { preset ->
                            val isAdded = rssFeeds.any { it.url.equals(preset.url, ignoreCase = true) }
                            Surface(
                                color = if (isAdded) scheme.primary.copy(alpha = 0.2f) else scheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        if (isAdded) scheme.primary else scheme.outlineVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (!isAdded) {
                                            onAddRssFeed(preset.title, preset.url)
                                        }
                                    }
                            ) {
                                Text(
                                    text = if (isAdded) "✓ ${preset.title}" else "+ ${preset.title}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAdded) scheme.primary else scheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips for RSS Feeds
        if (rssFeeds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChipItem(
                    label = "সকল ফিড (${rssArticles.size})",
                    isSelected = selectedFeedFilter == "সকল ফিড",
                    onClick = { selectedFeedFilter = "সকল ফিড" }
                )
                FilterChipItem(
                    label = "কিওয়ার্ড ম্যাচিং",
                    isSelected = selectedFeedFilter == "কিওয়ার্ড ম্যাচিং",
                    onClick = { selectedFeedFilter = "কিওয়ার্ড ম্যাচিং" }
                )
                rssFeeds.forEach { feed ->
                    FilterChipItem(
                        label = feed.title,
                        isSelected = selectedFeedFilter == feed.title,
                        onClick = { selectedFeedFilter = feed.title }
                    )
                }
            }
        }

        if (rssFeeds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RssFeed,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = scheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "এখনো কোনো আরএসএস ফিড যোগ করা হয়নি",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "উপরের বক্স থেকে অথবা সেটিংস থেকে যেকোনো আরএসএস ফিড যোগ করলে সেই নিউজগুলো এখানে চলে আসবে",
                        fontSize = 13.sp,
                        color = scheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onOpenSettings, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সেটিংস থেকে আরএসএস যোগ করুন")
                    }
                }
            }
        } else {
            NewsListWithRefresh(
                articles = filteredArticles,
                heroArticles = emptyList(),
                isBgbTab = true,
                isRefreshing = isRefreshing,
                visitedLinks = visitedLinks,
                speakingLink = speakingLink,
                keywords = keywords,
                onRefresh = onRefresh,
                onArticleClick = onArticleClick,
                onSpeakClick = onSpeakClick,
                onCopyClick = onCopyClick,
                onWhatsAppClick = onWhatsAppClick,
                onBookmarkClick = onBookmarkClick
            )
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: Int,
    savedCount: Int = 0,
    rssCount: Int = 0,
    onTabSelected: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .background(scheme.surfaceVariant, RoundedCornerShape(24.dp))
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            icon = Icons.Default.Home,
            label = "হোম",
            isSelected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        BottomNavItem(
            icon = Icons.Default.PlayArrow,
            label = "ভিডিও",
            isSelected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        BottomNavItem(
            icon = Icons.Default.RssFeed,
            label = if (rssCount > 0) "আরএসএস ($rssCount)" else "আরএসএস ফিড",
            isSelected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
        BottomNavItem(
            icon = Icons.Default.Public,
            label = "লাইভ সার্চ",
            isSelected = selectedTab == 3,
            onClick = { onTabSelected(3) }
        )
        BottomNavItem(
            icon = Icons.Default.Bookmark,
            label = if (savedCount > 0) "বুকমার্ক ($savedCount)" else "বুকমার্ক",
            isSelected = selectedTab == 4,
            onClick = { onTabSelected(4) }
        )
        BottomNavItem(
            icon = Icons.Default.Settings,
            label = "সেটিংস",
            isSelected = selectedTab == 5,
            onClick = { onTabSelected(5) }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isSelected) scheme.tertiaryContainer else Color.Transparent,
                    shape = CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) scheme.onTertiaryContainer else scheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) scheme.primary else scheme.outline
        )
    }
}

fun formatRelativeTime(timestamp: Long, fallback: String): String {
    if (timestamp == 0L) return fallback
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}
