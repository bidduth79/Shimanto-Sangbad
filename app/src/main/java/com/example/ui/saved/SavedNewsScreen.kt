package com.example.ui.saved

import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NewsArticle
import com.example.ui.theme.*
import com.example.ui.components.*

// Bookmarked articles.

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
                    text = stringResource(R.string.saved_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.saved_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.outline
                )
            }
            Surface(
                color = scheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.saved_count, savedArticles.size),
                    color = scheme.primary,
                    style = MaterialTheme.typography.labelLarge,
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
                        text = stringResource(R.string.saved_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.saved_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.outline
                    )
                }
            }
        } else {
            val savedListState = androidx.compose.foundation.lazy.rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                com.example.ui.components.ScrollToTopOnReselect(savedListState)
                LazyColumn(
                    state = savedListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = com.example.ui.components.LocalBottomBarSpace.current + 12.dp)
                ) {
                    items(savedArticles, key = { it.link }) { article ->
                        val isVisited = article.link in visitedLinks
                        NewsCard(
                            article = article,
                            isVisited = isVisited,
                            isSpeaking = speakingLink == article.link,
                            keywords = keywords,
                            showRegion = true,
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
