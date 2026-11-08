package com.example.ui.live

import android.net.Uri
import android.util.Base64
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.NewsArticle
import com.example.ui.components.ArticleReaderDialog
import com.example.ui.components.NewsActions
import com.example.ui.components.SmartTwoStepJumpButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A web search the live screen can open in the in-app reader. */
private enum class SearchEngine(
    @param:StringRes val label: Int,
    val shortLabel: String,
    val brandColor: Color,
    val defaultQuery: String,
    val testTag: String
) {
    FACEBOOK(R.string.live_engine_facebook, "FB", Color(0xFF1877F2), "বিজিবি সীমান্ত", "fb_search_button"),
    YOUTUBE(R.string.live_engine_youtube, "YT", Color(0xFFFF0000), "বিজিবি সীমান্ত", "yt_search_button"),
    GOOGLE(R.string.live_engine_google, "Google", Color.Unspecified, "বিজিবি সীমান্ত", "google_search_button"),
    BING(R.string.live_engine_bing, "Bing", Color(0xFF008373), "BGB Bangladesh border", "bing_search_button"),
    YAHOO(R.string.live_engine_yahoo, "Yahoo", Color(0xFF6001D2), "BGB Bangladesh border", "yahoo_search_button"),
    DUCKDUCKGO(R.string.live_engine_duckduckgo, "DDG", Color(0xFFDE5833), "BGB Bangladesh border", "ddg_search_button");

    /** Search URL for [query], limited to recent results where the engine supports it. */
    fun urlFor(query: String): String {
        val q = Uri.encode(query)
        return when (this) {
            FACEBOOK -> "https://m.facebook.com/search/posts/?q=$q&filters=${Uri.encode(todayFacebookFilter())}"
            YOUTUBE -> "https://m.youtube.com/results?search_query=$q&sp=CAI%253D"
            GOOGLE -> "https://www.google.com/search?q=$q&tbm=nws&tbs=qdr:d"
            BING -> "https://www.bing.com/news/search?q=$q&qft=interval%3d%227%22"
            YAHOO -> "https://news.search.yahoo.com/search?p=$q"
            DUCKDUCKGO -> "https://duckduckgo.com/?q=$q&iar=news&ia=news"
        }
    }
}

/** Facebook's "posts from today" search filter (a base64 JSON blob, as its web UI builds it). */
private fun todayFacebookFilter(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR).toString()
    val month = String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)
    val day = String.format(Locale.US, "%02d", calendar.get(Calendar.DAY_OF_MONTH))
    val json = """{"rp_creation_time":"{\"name\":\"creation_time\",\"args\":\"{\\\"start_year\\\":\\\"$year\\\",\\\"start_month\\\":\\\"$month\\\",\\\"start_day\\\":\\\"$day\\\",\\\"end_year\\\":\\\"$year\\\",\\\"end_month\\\":\\\"$month\\\",\\\"end_day\\\":\\\"$day\\\"}\"}"}"""
    return Base64.encodeToString(json.toByteArray(), Base64.NO_WRAP)
}

/**
 * Live search: one-tap web searches (Facebook, YouTube, news engines) for any text or saved keyword,
 * plus the automatically collected social feed.
 */
@Composable
fun LiveSearchScreen(
    keywords: List<String>,
    facebookNews: List<NewsArticle>,
    actions: NewsActions,
    onArticleVisited: (NewsArticle) -> Unit,
    onOpenExternal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var reader by remember { mutableStateOf<Pair<String, String>?>(null) } // url to title

    reader?.let { (url, title) ->
        ArticleReaderDialog(url = url, title = title, onDismiss = { reader = null }, onOpenExternal = onOpenExternal)
    }
    val openSearch = { engine: SearchEngine, text: String ->
        val q = text.ifBlank { engine.defaultQuery }
        reader = engine.urlFor(q) to "${engine.shortLabel}: $q"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .imePadding()
    ) {
        LiveHeader(postCount = facebookNews.size)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().testTag("social_search_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            placeholder = { Text(stringResource(R.string.live_search_hint)) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryEngineButton(SearchEngine.FACEBOOK, Icons.Default.Public) { openSearch(SearchEngine.FACEBOOK, query) }
            PrimaryEngineButton(SearchEngine.YOUTUBE, Icons.Default.PlayArrow) { openSearch(SearchEngine.YOUTUBE, query) }
            PrimaryEngineButton(SearchEngine.GOOGLE, Icons.Default.Search) { openSearch(SearchEngine.GOOGLE, query) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(SearchEngine.BING, SearchEngine.YAHOO, SearchEngine.DUCKDUCKGO).forEach { engine ->
                SecondaryEngineButton(engine) { openSearch(engine, query) }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        SegmentedTabs(
            titles = listOf(
                stringResource(R.string.live_tab_social, facebookNews.size),
                stringResource(R.string.live_tab_keywords, keywords.size)
            ),
            selected = selectedTab,
            onSelect = { selectedTab = it }
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            SocialFeed(
                posts = facebookNews,
                filter = query,
                actions = actions,
                onOpen = { article ->
                    onArticleVisited(article)
                    reader = (article.originalLink ?: article.link) to article.source
                }
            )
        } else {
            KeywordQuickSearch(keywords = keywords, onSearch = { engine, keyword -> openSearch(engine, keyword) })
        }
    }
}

@Composable
private fun LiveHeader(postCount: Int) {
    val scheme = MaterialTheme.colorScheme
    val today = remember { SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("bn-BD")).format(Date()) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.live_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.onBackground)
            Spacer(modifier = Modifier.height(2.dp))
            Text(stringResource(R.string.live_today, today), style = MaterialTheme.typography.bodyMedium, color = scheme.outline)
        }
        Surface(color = scheme.primary.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
            Text(
                text = stringResource(R.string.live_post_count, postCount),
                color = scheme.primary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PrimaryEngineButton(
    engine: SearchEngine,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val container = engine.brandColor.takeIf { it != Color.Unspecified } ?: scheme.primary
    val content = if (engine.brandColor == Color.Unspecified) scheme.onPrimary else Color.White
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).testTag(engine.testTag),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(engine.label), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SecondaryEngineButton(engine: SearchEngine, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).testTag(engine.testTag),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = engine.brandColor.copy(alpha = 0.2f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        Text(stringResource(engine.label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SegmentedTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        titles.forEachIndexed { index, title ->
            val isSelected = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) scheme.primary else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) scheme.onPrimary else scheme.onBackground
                )
            }
        }
    }
}

/** Social posts collected in the background, optionally narrowed by the search box text. */
@Composable
private fun ColumnScope.SocialFeed(
    posts: List<NewsArticle>,
    filter: String,
    actions: NewsActions,
    onOpen: (NewsArticle) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val visible = remember(posts, filter) {
        if (filter.isBlank()) posts else posts.filter {
            it.title.contains(filter, ignoreCase = true) || it.matchedKeyword.contains(filter, ignoreCase = true)
        }
    }

    if (visible.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Public, contentDescription = null, tint = scheme.outline, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(stringResource(R.string.live_social_empty_title), color = scheme.onBackground, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(stringResource(R.string.live_social_empty_hint), color = scheme.outline, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val listState = rememberLazyListState()
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        com.example.ui.components.ScrollToTopOnReselect(listState)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = com.example.ui.components.LocalBottomBarSpace.current + 12.dp)
        ) {
            items(visible, key = { it.link }) { article ->
                SocialNewsCard(
                    article = article,
                    onOpen = { onOpen(article) },
                    onCopy = { actions.copyLink(article) },
                    onShare = { actions.shareToWhatsApp(article) },
                    onBookmark = { actions.toggleBookmark(article) }
                )
            }
        }
        if (visible.size > 1) {
            SmartTwoStepJumpButton(
                listState = listState,
                totalItemsCount = visible.size,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 12.dp)
            )
        }
    }
}

/** Each saved keyword with one-tap Facebook / YouTube / Google News searches. */
@Composable
private fun ColumnScope.KeywordQuickSearch(keywords: List<String>, onSearch: (SearchEngine, String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    if (keywords.isEmpty()) {
        Text(stringResource(R.string.live_no_keywords), style = MaterialTheme.typography.bodyMedium, color = scheme.outline, modifier = Modifier.padding(top = 16.dp))
        return
    }
    LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = com.example.ui.components.LocalBottomBarSpace.current + 12.dp)
    ) {
        items(keywords, key = { it }) { keyword ->
            val shape = RoundedCornerShape(14.dp)
            Surface(
                color = scheme.surfaceVariant,
                shape = shape,
                modifier = Modifier.fillMaxWidth().border(1.dp, scheme.outlineVariant, shape)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(keyword, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = scheme.onBackground, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(SearchEngine.FACEBOOK, SearchEngine.YOUTUBE, SearchEngine.GOOGLE).forEach { engine ->
                            val tint = engine.brandColor.takeIf { it != Color.Unspecified } ?: scheme.primary
                            FilledTonalButton(
                                onClick = { onSearch(engine, keyword) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = tint.copy(alpha = 0.2f),
                                    contentColor = scheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(engine.shortLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
