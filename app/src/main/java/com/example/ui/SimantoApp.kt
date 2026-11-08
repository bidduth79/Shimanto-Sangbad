package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.KeywordMatcher
import com.example.data.NewsArticle
import com.example.ui.components.BottomNavBar
import com.example.ui.components.Header
import com.example.ui.components.rememberNewsActions
import com.example.ui.components.toast
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeTab
import com.example.ui.home.NewsViewModel
import com.example.ui.rss.RssFeedScreen
import com.example.ui.saved.SavedNewsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.video.VideoError
import com.example.ui.video.VideoList
import com.example.ui.video.VideoUiState
import com.example.ui.video.VideoViewModel
import com.example.ui.live.LiveSearchScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.components.ArticleReaderDialog
import com.example.R
import com.example.ui.components.VideoSkeletonList
import androidx.compose.ui.res.stringResource

/** Bottom-navigation destinations, in bar order. */
private object Destination {
    const val HOME = 0
    const val VIDEO = 1
    const val RSS = 2
    const val LIVE = 3
    const val SAVED = 4
    const val SETTINGS = 5
}

/** App root: theme, bottom navigation and the screen for the selected destination. */
@Composable
fun SimantoApp(onDarkModeChanged: (Boolean) -> Unit) {
    val newsViewModel: NewsViewModel = hiltViewModel()
    val videoViewModel: VideoViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val home by newsViewModel.uiState.collectAsStateWithLifecycle()
    val video by videoViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()

    SideEffect { onDarkModeChanged(settings.isDarkMode) }

    MyApplicationTheme(darkTheme = settings.isDarkMode) {
        val context = LocalContext.current
        var destination by rememberSaveable { mutableIntStateOf(Destination.HOME) }
        var readerArticle by remember { mutableStateOf<NewsArticle?>(null) }
        var showYouTubeSettings by remember { mutableStateOf(false) }

        val actions = rememberNewsActions(
            resolveLink = newsViewModel::resolveLink,
            onOpen = { article ->
                newsViewModel.markVisited(article)
                readerArticle = article
            },
            onToggleBookmark = newsViewModel::toggleBookmark
        )
        val openExternal: (String) -> Unit = { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
        val addFeed: (String, String) -> Unit = { title, url ->
            settingsViewModel.addRssFeed(title, url)
            context.toast(R.string.toast_rss_added)
        }
        val removeFeed: (String) -> Unit = { url ->
            settingsViewModel.removeRssFeed(url)
            context.toast(R.string.toast_rss_removed)
        }
        val saveYoutube: (String, String) -> Unit = { apiKey, channelIds ->
            settingsViewModel.saveYoutubeApiKey(apiKey)
            settingsViewModel.saveYoutubeChannelIds(channelIds)
            context.toast(R.string.toast_youtube_settings_saved)
        }

        readerArticle?.let { article ->
            ArticleReaderDialog(
                url = article.originalLink ?: article.link,
                title = article.title,
                onDismiss = { readerArticle = null },
                onOpenExternal = openExternal
            )
        }

        if (showYouTubeSettings) {
            YouTubeSettingsDialog(
                currentApiKey = settings.youtubeApiKey,
                currentChannelId = settings.youtubeChannelIds,
                onDismiss = { showYouTubeSettings = false },
                onSave = saveYoutube
            )
        }

        // Back goes other tab -> Home, then country tab -> "all", then exits
        BackHandler(enabled = destination != Destination.HOME || home.selection.tab != HomeTab.ALL) {
            if (destination != Destination.HOME) destination = Destination.HOME
            else newsViewModel.selectTab(HomeTab.ALL)
        }

        // "New since you last looked" counts for the bottom bar. A tab counts as looked at while it is
        // open and when you leave it; a first-ever visit shows no count.
        LaunchedEffect(destination) {
            if (destination == Destination.HOME) settingsViewModel.markHomeSeen()
            if (destination == Destination.VIDEO) settingsViewModel.markVideoSeen()
        }
        var previousDestination by remember { mutableIntStateOf(destination) }
        LaunchedEffect(destination) {
            if (previousDestination == Destination.HOME && destination != Destination.HOME) settingsViewModel.markHomeSeen()
            if (previousDestination == Destination.VIDEO && destination != Destination.VIDEO) settingsViewModel.markVideoSeen()
            previousDestination = destination
        }
        val newNewsCount = remember(home.allTimestamps, settings.lastSeenHomeAt, settings.showNewBadges) {
            val since = settings.lastSeenHomeAt
            if (!settings.showNewBadges || since == 0L) 0
            else home.allTimestamps.count { it > since }
        }
        val newVideoCount = remember(video.videos, settings.lastSeenVideoAt, settings.showNewBadges) {
            val since = settings.lastSeenVideoAt
            if (!settings.showNewBadges || since == 0L) 0
            else video.videos.count { com.example.ui.video.parseYouTubeTimestampMillis(it.snippet?.publishedAt) > since }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomNavBar(
                    selectedTab = destination,
                    savedCount = home.savedNews.size,
                    rssCount = home.rssFeeds.size,
                    newNewsCount = newNewsCount,
                    newVideoCount = newVideoCount,
                    onTabSelected = { destination = it }
                )
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                val header = @Composable { showFilterToggle: Boolean ->
                    Header(
                        query = home.selection.searchQuery,
                        isRefreshing = home.isRefreshing,
                        isDarkMode = settings.isDarkMode,
                        areFilterChipsVisible = home.selection.areFilterChipsVisible,
                        onToggleFilterChips = if (showFilterToggle) newsViewModel::toggleFilterChips else null,
                        onToggleDarkMode = settingsViewModel::toggleDarkMode,
                        onQueryChange = newsViewModel::updateSearchQuery,
                        keywords = home.keywords,
                        selectedKeyword = home.selection.keyword,
                        keywordCounts = home.keywordCounts,
                        onSelectKeyword = newsViewModel::selectKeyword,
                        onRefreshClick = {
                            newsViewModel.refresh()
                            videoViewModel.refresh()
                        },
                        onOpenYouTubeSettings = { showYouTubeSettings = true }
                    )
                }

                when (destination) {
                    Destination.HOME -> {
                        header(true)
                        HomeScreen(
                            state = home,
                            actions = actions,
                            onSelectTab = newsViewModel::selectTab,
                            onSelectFilter = newsViewModel::selectFilter,
                            onSelectMaxAge = newsViewModel::selectMaxAge,
                            onToggleOnlyUnread = newsViewModel::toggleOnlyUnread,
                            onRefresh = newsViewModel::refresh
                        )
                    }

                    Destination.VIDEO -> {
                        header(true)
                        LaunchedEffect(Unit) { videoViewModel.onScreenVisible() }
                        VideoTab(
                            state = video,
                            searchQuery = home.selection.searchQuery,
                            keyword = home.selection.keyword,
                            maxAgeHours = home.selection.maxAgeHours,
                            onSelectMaxAge = newsViewModel::selectMaxAge,
                            mediaChannelIds = videoViewModel.mediaChannelIds.collectAsStateWithLifecycle().value,
                            watchedLinks = videoViewModel.watchedLinks.collectAsStateWithLifecycle().value,
                            onWatched = videoViewModel::markWatched,
                            showFilters = home.selection.areFilterChipsVisible,
                            onRetry = videoViewModel::refresh
                        )
                    }

                    Destination.RSS -> RssFeedScreen(
                        rssFeeds = home.rssFeeds,
                        rssArticles = home.customFeedNews,
                        keywords = home.keywords,
                        isRefreshing = home.isRefreshing,
                        visitedLinks = home.visitedLinks,
                        speakingLink = actions.speakingLink,
                        onAddRssFeed = addFeed,
                        onRemoveRssFeed = removeFeed,
                        onOpenSettings = { destination = Destination.SETTINGS },
                        onRefresh = newsViewModel::refresh,
                        onArticleClick = actions.open,
                        onSpeakClick = actions.speak,
                        onCopyClick = actions.copyLink,
                        onWhatsAppClick = actions.shareToWhatsApp,
                        onBookmarkClick = actions.toggleBookmark
                    )

                    Destination.LIVE -> LiveSearchScreen(
                        keywords = home.keywords,
                        facebookNews = home.facebookNews,
                        actions = actions,
                        onArticleVisited = newsViewModel::markVisited,
                        onOpenExternal = openExternal
                    )

                    Destination.SAVED -> SavedNewsScreen(
                        savedArticles = home.savedNews,
                        visitedLinks = home.visitedLinks,
                        speakingLink = actions.speakingLink,
                        keywords = home.keywords,
                        onArticleClick = actions.open,
                        onSpeakClick = actions.speak,
                        onCopyClick = actions.copyLink,
                        onWhatsAppClick = actions.shareToWhatsApp,
                        onBookmarkClick = actions.toggleBookmark
                    )

                    Destination.SETTINGS -> SettingsScreen(
                        state = settings,
                        onAddKeyword = settingsViewModel::addKeyword,
                        onRemoveKeyword = settingsViewModel::removeKeyword,
                        onAddRssFeed = addFeed,
                        onRemoveRssFeed = removeFeed,
                        onSaveYoutubeSettings = saveYoutube,
                        onToggleDarkMode = settingsViewModel::toggleDarkMode,
                        onToggleNewBadges = settingsViewModel::toggleNewBadges,
                        onClearCache = {
                            settingsViewModel.clearCache {
                                context.toast(R.string.toast_cache_cleared)
                                newsViewModel.refresh()
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Video tab body: a spinner on first load, then the list filtered by the header search text. */
@Composable
private fun VideoTab(
    state: VideoUiState,
    searchQuery: String,
    keyword: String?,
    maxAgeHours: Int?,
    onSelectMaxAge: (Int?) -> Unit,
    mediaChannelIds: Set<String>,
    watchedLinks: Set<String>,
    onWatched: (String) -> Unit,
    showFilters: Boolean,
    onRetry: () -> Unit
) {
    val videos = remember(state.videos, searchQuery, keyword) {
        state.videos
            .filter { keyword == null || KeywordMatcher.matches(it.snippet?.title.orEmpty(), listOf(keyword)) }
            .filter {
                searchQuery.isBlank() ||
                    it.snippet?.title?.contains(searchQuery, ignoreCase = true) == true ||
                    it.snippet?.channelTitle?.contains(searchQuery, ignoreCase = true) == true
            }
    }
    if (state.isLoading && videos.isEmpty()) {
        VideoSkeletonList(message = stringResource(R.string.video_loading_first))
        return
    }
    val errorText = when (val error = state.error) {
        null -> null
        VideoError.NoResults -> stringResource(R.string.video_error_no_results)
        is VideoError.LoadFailed -> stringResource(
            R.string.video_error_load_failed,
            error.detail ?: stringResource(R.string.unknown_error)
        )
    }
    VideoList(
        videos = videos,
        errorMessage = errorText,
        maxAgeHours = maxAgeHours,
        onSelectMaxAge = onSelectMaxAge,
        mediaChannelIds = mediaChannelIds,
        watchedLinks = watchedLinks,
        onWatched = onWatched,
        isRefreshing = state.isRefreshing,
        showFilters = showFilters,
        onRetry = onRetry
    )
}
