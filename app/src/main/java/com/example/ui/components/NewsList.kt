package com.example.ui.components

import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NewsArticle
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Scrolling, pull-to-refresh news list with incremental paging and a jump button.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListWithRefresh(
    articles: List<NewsArticle>,
    heroArticles: List<NewsArticle> = emptyList(),
    showRegion: Boolean = false,
    isRefreshing: Boolean,
    visitedLinks: Set<String>,
    speakingLink: String? = null,
    keywords: List<String> = emptyList(),
    onRefresh: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onSpeakClick: (NewsArticle) -> Unit = {},
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit = {},
    relatedReports: Map<String, List<NewsArticle>> = emptyMap()
) {
    var sourcesOf by remember { mutableStateOf<NewsArticle?>(null) }
    sourcesOf?.let { lead ->
        RelatedSourcesSheet(
            lead = lead,
            others = relatedReports[lead.link].orEmpty(),
            visitedLinks = visitedLinks,
            onOpen = { article ->
                sourcesOf = null
                onArticleClick(article)
            },
            onDismiss = { sourcesOf = null }
        )
    }
    var visibleLimit by remember { mutableIntStateOf(30) }
    val scheme = MaterialTheme.colorScheme
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    ScrollToTopOnReselect(listState)
    val reducedMotion = rememberReducedMotion()

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
                    Text(stringResource(R.string.news_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.news_empty_hint), style = MaterialTheme.typography.bodyMedium, color = scheme.outline)
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
                    contentPadding = PaddingValues(top = 6.dp, bottom = LocalBottomBarSpace.current + 12.dp)
                ) {
                    if (heroArticles.isNotEmpty()) {
                        item(key = "hero_carousel") {
                            BreakingNewsHeroCarousel(
                                articles = heroArticles,
                                keywords = keywords,
                                showRegion = showRegion,
                                imageParallax = if (reducedMotion) null else listState::firstItemScrollPx,
                                onArticleClick = onArticleClick
                            )
                        }
                    }

                    items(visibleArticles, key = { it.link + it.timestamp }) { article ->
                        val isVisited = article.link in visitedLinks
                        val itemKey = article.link + article.timestamp
                        val parallax = remember(itemKey, reducedMotion) {
                            if (reducedMotion) null else { { listState.viewportPositionOf(itemKey) } }
                        }
                        NewsCard(
                            article = article,
                            isVisited = isVisited,
                            isSpeaking = speakingLink == article.link,
                            keywords = keywords,
                            showRegion = showRegion,
                            parallax = parallax,
                            onArticleClick = onArticleClick,
                            onSpeakClick = onSpeakClick,
                            onCopyClick = onCopyClick,
                            onWhatsAppClick = onWhatsAppClick,
                            onBookmarkClick = onBookmarkClick,
                            relatedCount = relatedReports[article.link]?.size ?: 0,
                            onRelatedClick = { sourcesOf = article }
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
                            .padding(end = 16.dp, bottom = LocalBottomBarSpace.current + 14.dp)
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
                            contentDescription = stringResource(R.string.jump_middle),
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
                                contentDescription = stringResource(R.string.jump_top),
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
                                contentDescription = stringResource(R.string.jump_bottom),
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
                            contentDescription = stringResource(R.string.jump_top_direct),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
