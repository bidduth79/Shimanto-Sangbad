package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RssFeedItem

val SuggestedRssFeeds = listOf(
    RssFeedItem("প্রথম আলো", "https://www.prothomalo.com/feed/"),
    RssFeedItem("বিবিসি বাংলা", "https://feeds.bbci.co.uk/bengali/rss.xml"),
    RssFeedItem("ডয়েচে ভেলে বাংলা", "https://rss.dw.com/xml/rss-ben-all"),
    RssFeedItem("দ্য ডেইলি স্টার", "https://www.thedailystar.net/frontpage/rss.xml"),
    RssFeedItem("The Hindu (ভারত)", "https://www.thehindu.com/news/national/feeder/default.rss"),
    RssFeedItem("Times of India (ভারত)", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms"),
    RssFeedItem("আনন্দবাজার (ভারত)", "https://news.google.com/rss/search?q=site:anandabazar.com&hl=bn&gl=IN&ceid=IN:bn"),
    RssFeedItem("Al Jazeera (আন্তর্জাতিক)", "https://www.aljazeera.com/xml/rss/all.xml"),
    RssFeedItem("BBC World (আন্তর্জাতিক)", "https://feeds.bbci.co.uk/news/world/rss.xml"),
    RssFeedItem("The Irrawaddy (মিয়ানমার)", "https://www.irrawaddy.com/feed")
)

@Composable
fun SettingsContent(
    keywords: List<String>,
    rssFeeds: List<RssFeedItem> = emptyList(),
    youtubeApiKey: String,
    youtubeChannelId: String = "",
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onAddRssFeed: (String, String) -> Unit = { _, _ -> },
    onRemoveRssFeed: (String) -> Unit = {},
    onSaveApiKey: (String) -> Unit,
    onSaveChannelId: (String) -> Unit = {},
    onClearCache: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Expandable states for each card (collapsed by default so the menu stays compact)
    var isYoutubeExpanded by remember { mutableStateOf(false) }
    var isMemoryExpanded by remember { mutableStateOf(false) }
    var isRssExpanded by remember { mutableStateOf(false) }
    var isKeywordsExpanded by remember { mutableStateOf(false) }

    var showAddInput by remember { mutableStateOf(true) }
    var newKeyword by remember { mutableStateOf("") }

    var showAddRssInput by remember { mutableStateOf(true) }
    var newRssTitle by remember { mutableStateOf("") }
    var newRssUrl by remember { mutableStateOf("") }

    var showYouTubeDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scheme = MaterialTheme.colorScheme

    if (showYouTubeDialog) {
        YouTubeSettingsDialog(
            currentApiKey = youtubeApiKey,
            currentChannelId = youtubeChannelId,
            onDismiss = { showYouTubeDialog = false },
            onSave = { key, channel ->
                onSaveApiKey(key)
                onSaveChannelId(channel)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "সেটিংস",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // --- 1. YouTube API Key & Channel ID Expandable Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("youtube_settings_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isYoutubeExpanded = !isYoutubeExpanded }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ইউটিউব এপিআই ও চ্যানেল আইডি",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = if (isYoutubeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isYoutubeExpanded) "সংকুচিত করুন" else "প্রসারিত করুন",
                        tint = scheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isYoutubeExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showYouTubeDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("সেটিংস", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // --- 2. Memory Cleaner Expandable Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("memory_cleaner_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isMemoryExpanded = !isMemoryExpanded }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "মেমোরি ক্লিনার",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = if (isMemoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isMemoryExpanded) "সংকুচিত করুন" else "প্রসারিত করুন",
                        tint = scheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isMemoryExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "অ্যাপের ক্যাশ ও পুরনো ডাটা পরিষ্কার করুন",
                                fontSize = 13.sp,
                                color = scheme.outline,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = onClearCache,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("ক্লিন করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- 3. RSS Feed Management Expandable Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rss_settings_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRssExpanded = !isRssExpanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "আরএসএস ফিড (${rssFeeds.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onBackground
                            )
                            Text(
                                text = "যেকোনো নিউজ সাইটের RSS লিংক যোগ করুন",
                                fontSize = 12.sp,
                                color = scheme.outline
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isRssExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isRssExpanded) "সংকুচিত করুন" else "প্রসারিত করুন",
                        tint = scheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isRssExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "নতুন ফিড যুক্ত করুন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onBackground
                            )
                            IconButton(
                                onClick = { showAddRssInput = !showAddRssInput },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(scheme.primary, RoundedCornerShape(50))
                                    .testTag("toggle_add_rss_button")
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add RSS Feed",
                                    tint = scheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (showAddRssInput) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = newRssTitle,
                                onValueChange = { newRssTitle = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rss_title_input"),
                                placeholder = { Text("ফিডের নাম (ঐচ্ছিক, যেমন: প্রথম আলো)", color = scheme.outline, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newRssUrl,
                                    onValueChange = { newRssUrl = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("rss_url_input"),
                                    placeholder = { Text("RSS লিংক (https://.../rss.xml)", color = scheme.outline, fontSize = 13.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newRssUrl.isNotBlank()) {
                                            onAddRssFeed(newRssTitle, newRssUrl)
                                            newRssTitle = ""
                                            newRssUrl = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = scheme.primary,
                                        contentColor = scheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("add_rss_submit_button")
                                ) {
                                    Text("যোগ করুন", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick-Add Popular Feeds Row
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "জনপ্রিয় ফিড এক ক্লিকে যোগ করুন:",
                                fontSize = 12.sp,
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
                                SuggestedRssFeeds.forEach { preset ->
                                    val isAdded = rssFeeds.any { it.url.equals(preset.url, ignoreCase = true) }
                                    Surface(
                                        color = if (isAdded) scheme.primary.copy(alpha = 0.2f) else scheme.surface,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(
                                                1.dp,
                                                if (isAdded) scheme.primary else scheme.outlineVariant,
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                if (!isAdded) {
                                                    onAddRssFeed(preset.title, preset.url)
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = if (isAdded) "✓ ${preset.title}" else "+ ${preset.title}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isAdded) scheme.primary else scheme.onBackground,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (rssFeeds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                rssFeeds.forEach { feed ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(scheme.surface, RoundedCornerShape(12.dp))
                                            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = feed.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = scheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = feed.url,
                                                fontSize = 11.sp,
                                                color = scheme.outline,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { onRemoveRssFeed(feed.url) },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .background(scheme.errorContainer, RoundedCornerShape(50))
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove RSS Feed",
                                                tint = scheme.onErrorContainer,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Keywords Management Expandable Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("keywords_settings_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isKeywordsExpanded = !isKeywordsExpanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "আমার কিওয়ার্ড সমূহ (${keywords.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onBackground
                            )
                            Text(
                                text = "নিউজ ও ভিডিও সার্চের কিওয়ার্ড পরিচালনা করুন",
                                fontSize = 12.sp,
                                color = scheme.outline
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isKeywordsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isKeywordsExpanded) "সংকুচিত করুন" else "প্রসারিত করুন",
                        tint = scheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isKeywordsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "নতুন কিওয়ার্ড যুক্ত করুন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onBackground
                            )
                            IconButton(
                                onClick = { showAddInput = !showAddInput },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(scheme.primary, RoundedCornerShape(50))
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Keyword",
                                    tint = scheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (showAddInput) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newKeyword,
                                    onValueChange = { newKeyword = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("নতুন কিওয়ার্ড...", color = scheme.outline) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newKeyword.isNotBlank()) {
                                            onAddKeyword(newKeyword)
                                            newKeyword = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = scheme.primary,
                                        contentColor = scheme.onPrimary
                                    )
                                ) {
                                    Text("যোগ করুন")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            keywords.forEach { keyword ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(scheme.surface, RoundedCornerShape(12.dp))
                                        .border(1.dp, scheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = keyword, fontSize = 15.sp, color = scheme.onBackground)
                                    IconButton(
                                        onClick = { onRemoveKeyword(keyword) },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(scheme.errorContainer, RoundedCornerShape(50))
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove",
                                            tint = scheme.onErrorContainer,
                                            modifier = Modifier.size(15.dp)
                                        )
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

