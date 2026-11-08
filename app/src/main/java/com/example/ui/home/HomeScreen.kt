package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.components.FilterChipsBar
import com.example.ui.components.NewsActions
import com.example.ui.components.NewsCategoryTabBar
import com.example.ui.components.NewsListWithRefresh
import com.example.ui.components.SkeletonList

/** Country tabs, filter chips and the news list. All filtering already happened in [buildHomeState]. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: NewsActions,
    onSelectTab: (HomeTab) -> Unit,
    onSelectFilter: (String) -> Unit,
    onSelectMaxAge: (Int?) -> Unit,
    onToggleOnlyUnread: () -> Unit,
    onRefresh: () -> Unit
) {
    AnimatedVisibility(visible = state.selection.areFilterChipsVisible) {
        Column(modifier = Modifier.fillMaxWidth()) {
            NewsCategoryTabBar(
                selectedTab = state.selection.tab.ordinal,
                allCount = state.tabCounts.all,
                bdCount = state.tabCounts.bangladesh,
                indianCount = state.tabCounts.india,
                myanmarCount = state.tabCounts.myanmar,
                internationalCount = state.tabCounts.international,
                onSelectTab = { onSelectTab(HomeTab.entries[it]) }
            )
            val counts = state.filterCounts
            FilterChipsBar(
                isAllTab = state.selection.tab == HomeTab.ALL,
                selectedFilter = state.selection.filter,
                counts = counts,
                maxAgeHours = state.selection.maxAgeHours,
                onSelectMaxAge = onSelectMaxAge,
                onlyUnread = state.selection.onlyUnread,
                onToggleOnlyUnread = onToggleOnlyUnread,
                onSelectFilter = onSelectFilter
            )
        }
    }

    if (state.isLoading) {
        SkeletonList()
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        NewsListWithRefresh(
            articles = state.visibleArticles,
            heroArticles = state.heroArticles,
            showRegion = state.showRegionLabels,
            isRefreshing = state.isRefreshing,
            visitedLinks = state.visitedLinks,
            speakingLink = actions.speakingLink,
            keywords = state.keywords,
            onRefresh = onRefresh,
            onArticleClick = actions.open,
            onSpeakClick = actions.speak,
            onCopyClick = actions.copyLink,
            onWhatsAppClick = actions.shareToWhatsApp,
            onBookmarkClick = actions.toggleBookmark
        )
    }
}
