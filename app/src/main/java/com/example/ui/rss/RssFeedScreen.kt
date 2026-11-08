package com.example.ui.rss

import com.example.ui.settings.SuggestedFeedChips
import com.example.data.KeywordMatcher
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NewsArticle
import com.example.ui.theme.*
import com.example.ui.components.*

// Articles from the user's own RSS feeds, with feed management.

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
    var selectedFeedFilter by remember { mutableStateOf(FILTER_ALL_FEEDS) }
    var showInlineAddBox by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newUrl by remember { mutableStateOf("") }

    val filteredArticles = remember(rssArticles, selectedFeedFilter, keywords) {
        when (selectedFeedFilter) {
            FILTER_ALL_FEEDS -> rssArticles
            FILTER_KEYWORDS -> rssArticles.filter { KeywordMatcher.matches(it.title, keywords) }
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
                        text = stringResource(R.string.rss_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.rss_summary, rssFeeds.size, rssArticles.size),
                        style = MaterialTheme.typography.labelLarge,
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
                    Text(stringResource(R.string.rss_add_feed), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
                        text = stringResource(R.string.rss_add_new_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.rss_name_hint), style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newUrl,
                            onValueChange = { newUrl = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.rss_url_hint), style = MaterialTheme.typography.bodyMedium) },
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
                            Text(stringResource(R.string.action_add), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SuggestedFeedChips(added = rssFeeds, onAdd = onAddRssFeed)
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
                    label = stringResource(R.string.filter_with_count, stringResource(R.string.rss_filter_all), rssArticles.size),
                    isSelected = selectedFeedFilter == FILTER_ALL_FEEDS,
                    onClick = { selectedFeedFilter = FILTER_ALL_FEEDS }
                )
                FilterChipItem(
                    label = stringResource(R.string.rss_filter_keywords),
                    isSelected = selectedFeedFilter == FILTER_KEYWORDS,
                    onClick = { selectedFeedFilter = FILTER_KEYWORDS }
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
                        text = stringResource(R.string.rss_empty_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.rss_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onOpenSettings, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.rss_open_settings))
                    }
                }
            }
        } else {
            NewsListWithRefresh(
                articles = filteredArticles,
                heroArticles = emptyList(),
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

// Internal keys for the two built-in chips (feed titles are used as keys for the rest)
private const val FILTER_ALL_FEEDS = "__all_feeds__"
private const val FILTER_KEYWORDS = "__keywords__"
