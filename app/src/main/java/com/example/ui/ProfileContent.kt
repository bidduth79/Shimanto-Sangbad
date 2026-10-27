package com.example.ui

import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.NewsArticle
import com.example. formatRelativeTime
import com.example.ui.theme.ErrorContainer
import com.example.ui.theme.OnBackground
import com.example.ui.theme.OnErrorContainer
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.Outline
import com.example.ui.theme.OutlineVariant
import com.example.ui.theme.Primary
import com.example.ui.theme.Surface
import com.example.ui.theme.SurfaceVariant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun getTodayFbFilter(): String {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR).toString()
    val month = String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)
    val day = String.format(Locale.US, "%02d", calendar.get(Calendar.DAY_OF_MONTH))
    val outerJson = """{"rp_creation_time":"{\"name\":\"creation_time\",\"args\":\"{\\\"start_year\\\":\\\"$year\\\",\\\"start_month\\\":\\\"$month\\\",\\\"start_day\\\":\\\"$day\\\",\\\"end_year\\\":\\\"$year\\\",\\\"end_month\\\":\\\"$month\\\",\\\"end_day\\\":\\\"$day\\\"}\"}"}"""
    return Base64.encodeToString(outerJson.toByteArray(), Base64.NO_WRAP)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    keywords: List<String>,
    facebookNews: List<NewsArticle> = emptyList(),
    onArticleClick: (NewsArticle) -> Unit = {},
    onCopyClick: (NewsArticle) -> Unit = {},
    onWhatsAppClick: (NewsArticle) -> Unit = {},
    onBookmarkClick: (NewsArticle) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var customQuery by remember { mutableStateOf("") }
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = সোশ্যাল ফিড, 1 = কীওয়ার্ড লাইভ সার্চ
    var inAppWebUrl by remember { mutableStateOf<String?>(null) }
    var inAppWebTitle by remember { mutableStateOf("") }

    val todayDate = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD")).format(Date())

    if (inAppWebUrl != null) {
        InAppSocialWebViewDialog(
            url = inAppWebUrl!!,
            title = inAppWebTitle,
            onDismiss = { inAppWebUrl = null },
            onOpenExternal = { url ->
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "লাইভ সার্চ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "আজকের তারিখ: $todayDate",
                    fontSize = 13.sp,
                    color = Outline
                )
            }
            Surface(
                color = Primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${facebookNews.size}টি পোস্ট",
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Search Input Box
        OutlinedTextField(
            value = customQuery,
            onValueChange = { customQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("social_search_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Primary)
            },
            placeholder = { Text("যেকোনো বিষয় লিখে ফেসবুক/ইউটিউবে খুঁজুন...") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Action Buttons for Custom Search (Facebook, YouTube, Google News)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val q = customQuery.ifBlank { "বিজিবি সীমান্ত" }
                    val filter = getTodayFbFilter()
                    val url = "https://m.facebook.com/search/posts/?q=${Uri.encode(q)}&filters=${Uri.encode(filter)}"
                    inAppWebTitle = "Facebook: $q"
                    inAppWebUrl = url
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("fb_search_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ফেসবুক", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val q = customQuery.ifBlank { "বিজিবি সীমান্ত" }
                    val url = "https://m.youtube.com/results?search_query=${Uri.encode(q)}&sp=CAI%253D"
                    inAppWebTitle = "YouTube: $q"
                    inAppWebUrl = url
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("yt_search_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ইউটিউব", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val q = customQuery.ifBlank { "বিজিবি সীমান্ত" }
                    val url = "https://www.google.com/search?q=${Uri.encode(q)}&tbm=nws&tbs=qdr:d"
                    inAppWebTitle = "Google News: $q"
                    inAppWebUrl = url
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("google_search_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = OnPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("গুগল নিউজ", color = OnPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Secondary Action Buttons for Other News Engines (Bing News, Yahoo News, DuckDuckGo News)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    val q = customQuery.ifBlank { "BGB Bangladesh border" }
                    val url = "https://www.bing.com/news/search?q=${Uri.encode(q)}&qft=interval%3d%227%22"
                    inAppWebTitle = "Bing News: $q"
                    inAppWebUrl = url
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("bing_search_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF008373).copy(alpha = 0.2f),
                    contentColor = Color(0xFF2DD4BF)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("বিং নিউজ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = {
                    val q = customQuery.ifBlank { "BGB Bangladesh border" }
                    val url = "https://news.search.yahoo.com/search?p=${Uri.encode(q)}"
                    inAppWebTitle = "Yahoo News: $q"
                    inAppWebUrl = url
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("yahoo_search_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF6001D2).copy(alpha = 0.2f),
                    contentColor = Color(0xFFC084FC)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("ইয়াহু নিউজ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            FilledTonalButton(
                onClick = {
                    val q = customQuery.ifBlank { "BGB Bangladesh border" }
                    val url = "https://duckduckgo.com/?q=${Uri.encode(q)}&iar=news&ia=news"
                    inAppWebTitle = "DuckDuckGo News: $q"
                    inAppWebUrl = url
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ddg_search_button"),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFDE5833).copy(alpha = 0.2f),
                    contentColor = Color(0xFFFB923C)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("ডাকডাকগো নিউজ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Sub-Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariant, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                "সোশ্যাল ফিড (${facebookNews.size})",
                "কীওয়ার্ড কুইক সার্চ (${keywords.size})"
            )
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Primary else Color.Transparent)
                        .clickable { selectedSubTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) OnPrimary else OnBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedSubTab == 0) {
            // Live Facebook & Social News Feed collected automatically
            val filteredFb = if (customQuery.isNotBlank()) {
                facebookNews.filter {
                    it.title.contains(customQuery, ignoreCase = true) ||
                        it.matchedKeyword.contains(customQuery, ignoreCase = true)
                }
            } else {
                facebookNews
            }

            if (filteredFb.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "সোশ্যাল ফিড লোড হচ্ছে বা কোনো পোস্ট পাওয়া যায়নি",
                            color = OnBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "উপরের 'ফেসবুক' বাটনে ট্যাপ করে সরাসরি লাইভ ফেসবুক পোস্ট দেখুন",
                            color = Outline,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                val socialListState = androidx.compose.foundation.lazy.rememberLazyListState()
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        state = socialListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredFb, key = { it.link }) { article ->
                            SocialNewsCard(
                                article = article,
                                onOpenInApp = {
                                    onArticleClick(article)
                                    inAppWebTitle = article.source
                                    inAppWebUrl = article.originalLink ?: article.link
                                },
                                onCopyClick = { onCopyClick(article) },
                                onWhatsAppClick = { onWhatsAppClick(article) },
                                onBookmarkClick = { onBookmarkClick(article) }
                            )
                        }
                    }
                    if (filteredFb.size > 1) {
                        com.example.SmartTwoStepJumpButton(
                            listState = socialListState,
                            totalItemsCount = filteredFb.size,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 4.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        } else {
            // Saved Keywords One-Tap Search List
            if (keywords.isEmpty()) {
                Text(
                    text = "কোনো কীওয়ার্ড সেভ করা নেই। সেটিংস থেকে যোগ করুন।",
                    fontSize = 14.sp,
                    color = Outline,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(keywords) { keyword ->
                        Surface(
                            color = SurfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, OutlineVariant, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = keyword,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            val filter = getTodayFbFilter()
                                            val url = "https://m.facebook.com/search/posts/?q=${Uri.encode(keyword)}&filters=${Uri.encode(filter)}"
                                            inAppWebTitle = "FB: $keyword"
                                            inAppWebUrl = url
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF1877F2).copy(alpha = 0.2f),
                                            contentColor = Color(0xFF64B5F6)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("FB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            val url = "https://m.youtube.com/results?search_query=${Uri.encode(keyword)}&sp=CAI%253D"
                                            inAppWebTitle = "YT: $keyword"
                                            inAppWebUrl = url
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFFFF0000).copy(alpha = 0.2f),
                                            contentColor = Color(0xFFFF8A80)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("YT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            val url = "https://www.google.com/search?q=${Uri.encode(keyword)}&tbm=nws&tbs=qdr:d"
                                            inAppWebTitle = "News: $keyword"
                                            inAppWebUrl = url
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Primary.copy(alpha = 0.2f),
                                            contentColor = Primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SocialNewsCard(
    article: NewsArticle,
    onOpenInApp: () -> Unit,
    onCopyClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1877F2).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .clickable { onOpenInApp() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF1877F2).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = article.matchedKeyword.ifBlank { "সোশ্যাল ফিড" },
                            color = Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = article.source,
                        color = Outline,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (article.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save",
                        tint = if (article.isSaved) Primary else Outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = article.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
                lineHeight = 21.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatRelativeTime(article.timestamp, article.pubDate),
                    color = Outline,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconButton(onClick = onWhatsAppClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Share WhatsApp",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onCopyClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = OnBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InAppSocialWebViewDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit,
    onOpenExternal: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(url) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
            webViewRef?.clearHistory()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Surface(
                    color = SurfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = OnBackground)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = title.ifBlank { "সংবাদ ভিউয়ার" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentUrl,
                                fontSize = 11.sp,
                                color = Outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row {
                            IconButton(onClick = { webViewRef?.reload() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Primary)
                            }
                            IconButton(onClick = { onOpenExternal(currentUrl) }) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = Primary)
                            }
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                                    isLoading = false
                                    if (!loadedUrl.isNullOrBlank()) {
                                        currentUrl = loadedUrl
                                    }
                                }
                            }
                            webChromeClient = WebChromeClient()
                            loadUrl(url)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}
