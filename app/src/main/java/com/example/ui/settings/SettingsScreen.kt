package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import com.example.ui.components.bengaliDigits
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.RssFeedItem
import com.example.ui.YouTubeSettingsDialog

/** One-tap feeds offered in the RSS sections. Names are feed titles (data), not UI text. */
val SuggestedRssFeeds = listOf(
    RssFeedItem("প্রথম আলো", "https://www.prothomalo.com/feed/"),
    RssFeedItem("বিবিসি বাংলা", "https://feeds.bbci.co.uk/bengali/rss.xml"),
    RssFeedItem("ডয়েচে ভেলে বাংলা", "https://rss.dw.com/xml/rss-ben-all"),
    RssFeedItem("দ্য ডেইলি স্টার", "https://www.thedailystar.net/frontpage/rss.xml"),
    RssFeedItem("The Hindu (ভারত)", "https://www.thehindu.com/news/national/feeder/default.rss"),
    RssFeedItem("Times of India (ভারত)", "https://timesofindia.indiatimes.com/rssfeedstopstories.cms"),
    RssFeedItem("আনন্দবাজার (ভারত)", "https://news.google.com/rss/search?q=site:anandabazar.com&hl=bn&gl=IN&ceid=IN:bn"),
    RssFeedItem("Al Jazeera (আন্তর্জাতিক)", "https://www.aljazeera.com/xml/rss/all.xml"),
    RssFeedItem("BBC World (আন্তর্জাতিক)", "https://feeds.bbci.co.uk/news/world/rss.xml"),
    RssFeedItem("The Irrawaddy (মিয়ানমার)", "https://www.irrawaddy.com/feed")
)

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAddKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onAddRssFeed: (title: String, url: String) -> Unit,
    onRemoveRssFeed: (String) -> Unit,
    onSaveYoutubeSettings: (apiKey: String, channelIds: String) -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleNewBadges: () -> Unit,
    onToggleAlerts: () -> Unit,
    onToggleTranslate: () -> Unit,
    onAddAlertKeyword: (String) -> Unit,
    onRemoveAlertKeyword: (String) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showYouTubeDialog by rememberSaveable { mutableStateOf(false) }
    if (showYouTubeDialog) {
        YouTubeSettingsDialog(
            currentApiKey = state.youtubeApiKey,
            currentChannelId = state.youtubeChannelIds,
            onDismiss = { showYouTubeDialog = false },
            onSave = onSaveYoutubeSettings
        )
    }

    val scrollState = rememberScrollState()
    com.example.ui.components.ScrollToTopOnReselect(scrollState)
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileCard(state)

        GroupLabel(stringResource(R.string.settings_group_sources))
        ExpandableSettingsCard(
            icon = Icons.Default.Search,
            title = stringResource(R.string.settings_keywords_title, state.keywords.size),
            subtitle = stringResource(R.string.settings_keywords_subtitle),
            testTag = "keywords_settings_card",
            initiallyExpanded = true
        ) {
            KeywordsSection(keywords = state.keywords, onAdd = onAddKeyword, onRemove = onRemoveKeyword)
        }
        ExpandableSettingsCard(
            icon = Icons.Default.RssFeed,
            title = stringResource(R.string.settings_rss_title, state.rssFeeds.size),
            subtitle = stringResource(R.string.settings_rss_subtitle),
            testTag = "rss_settings_card"
        ) {
            RssFeedsSection(feeds = state.rssFeeds, onAdd = onAddRssFeed, onRemove = onRemoveRssFeed)
        }

        GroupLabel(stringResource(R.string.settings_group_alerts))
        SettingsCard(modifier = Modifier.testTag("alerts_card")) {
            SettingRow(
                icon = Icons.Default.NotificationsActive,
                iconTint = Color(0xFFDC2626),
                title = stringResource(R.string.settings_alerts_title),
                subtitle = stringResource(if (state.alertsEnabled) R.string.settings_alerts_on else R.string.settings_alerts_off),
                onClick = onToggleAlerts
            ) {
                Switch(checked = state.alertsEnabled, onCheckedChange = { onToggleAlerts() })
            }
            AnimatedVisibility(visible = state.alertsEnabled) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        stringResource(R.string.settings_alert_words, state.alertKeywords.size),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KeywordsSection(keywords = state.alertKeywords, onAdd = onAddAlertKeyword, onRemove = onRemoveAlertKeyword)
                }
            }
        }

        GroupLabel(stringResource(R.string.settings_group_video))
        YouTubeCard(state = state, onOpen = { showYouTubeDialog = true })

        val healthNow = com.example.ui.components.LocalNow.current
        val problems = problemCount(state.sourceStatuses, healthNow)
        ExpandableSettingsCard(
            icon = Icons.Default.MonitorHeart,
            title = stringResource(R.string.health_title),
            subtitle = when {
                state.sourceStatuses.isEmpty() -> stringResource(R.string.health_waiting)
                problems == 0 -> stringResource(R.string.health_all_ok)
                else -> stringResource(R.string.health_problems, com.example.ui.components.bengaliDigits(problems))
            },
            testTag = "source_health_card"
        ) {
            SourceHealthBody(state.sourceStatuses, state.lastRefreshRoundAt)
        }

        GroupLabel(stringResource(R.string.settings_group_app))
        SettingsCard {
            SettingRow(
                icon = if (state.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                iconTint = if (state.isDarkMode) Color(0xFF818CF8) else Color(0xFFF59E0B),
                title = stringResource(R.string.settings_theme_title),
                subtitle = stringResource(if (state.isDarkMode) R.string.settings_theme_dark else R.string.settings_theme_light),
                onClick = onToggleDarkMode
            ) {
                Switch(checked = state.isDarkMode, onCheckedChange = { onToggleDarkMode() })
            }
            RowDivider()
            SettingRow(
                icon = Icons.Default.Translate,
                iconTint = Color(0xFF0EA5E9),
                title = stringResource(R.string.settings_translate_title),
                subtitle = stringResource(if (state.translateHeadlines) R.string.settings_translate_on else R.string.settings_translate_off),
                onClick = onToggleTranslate
            ) {
                Switch(checked = state.translateHeadlines, onCheckedChange = { onToggleTranslate() })
            }
            RowDivider()
            SettingRow(
                icon = Icons.Default.NotificationsActive,
                iconTint = Color(0xFFE11D48),
                title = stringResource(R.string.settings_badges_title),
                subtitle = stringResource(if (state.showNewBadges) R.string.settings_badges_on else R.string.settings_badges_off),
                onClick = onToggleNewBadges
            ) {
                Switch(checked = state.showNewBadges, onCheckedChange = { onToggleNewBadges() })
            }
            RowDivider()
            SettingRow(
                icon = Icons.Default.CleaningServices,
                iconTint = Color(0xFF14B8A6),
                title = stringResource(R.string.settings_cache_title),
                subtitle = stringResource(R.string.settings_cache_description),
                modifier = Modifier.testTag("memory_cleaner_card")
            ) {
                FilledTonalButton(
                    onClick = onClearCache,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(stringResource(R.string.settings_cache_action), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
            RowDivider()
            SettingRow(
                icon = Icons.Default.Info,
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.settings_about_title),
                subtitle = stringResource(R.string.settings_about_version, state.appVersion)
            )
        }
        Spacer(modifier = Modifier.height(com.example.ui.components.LocalBottomBarSpace.current))
    }
}

/** Top card: emblem, app name, tagline and how many keywords, feeds and channels are set up. */
@Composable
private fun ProfileCard(state: SettingsUiState) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF0F3D2E), Color(0xFF14532D), Color(0xFF0A0F1C))))
            .drawBehind {
                // the same soft circles the news cards use
                drawCircle(Color.White.copy(alpha = 0.06f), radius = size.height * 0.75f, center = Offset(size.width * 0.95f, 0f))
                drawCircle(Color.White.copy(alpha = 0.04f), radius = size.height * 0.45f, center = Offset(size.width * 0.78f, size.height))
            }
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.10f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.bgb_emblem_header),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        stringResource(R.string.settings_tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFBBF24)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatPill(bengaliDigits(state.keywords.size), stringResource(R.string.settings_stat_keywords), Modifier.weight(1f))
                StatPill(bengaliDigits(state.rssFeeds.size), stringResource(R.string.settings_stat_feeds), Modifier.weight(1f))
                StatPill(bengaliDigits(state.channelCount), stringResource(R.string.settings_stat_channels), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
    }
}

/** API status (green/red), today's quota use and the channel count, with a button to edit them. */
@Composable
private fun YouTubeCard(state: SettingsUiState, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ok = state.hasYoutubeApiKey
    val statusColor = if (ok) Color(0xFF16A34A) else Color(0xFFDC2626)
    SettingsCard(modifier = Modifier.testTag("youtube_settings_card")) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Default.SmartDisplay, Color(0xFFEF4444))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_youtube_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(if (ok) R.string.settings_api_active else R.string.settings_api_missing),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }
                }
            }

            if (ok) {
                Spacer(modifier = Modifier.height(14.dp))
                val fraction = (state.quotaUsedToday.toFloat() / state.quotaDailyLimit).coerceIn(0f, 1f)
                val barColor = when {
                    fraction < 0.5f -> Color(0xFF16A34A)
                    fraction < 0.8f -> Color(0xFFF59E0B)
                    else -> Color(0xFFDC2626)
                }
                Text(
                    stringResource(
                        R.string.settings_quota_today,
                        bengaliNumber(state.quotaUsedToday),
                        bengaliNumber(state.quotaDailyLimit)
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = barColor,
                    trackColor = scheme.outlineVariant.copy(alpha = 0.5f),
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(stringResource(R.string.settings_quota_note), style = MaterialTheme.typography.labelSmall, color = scheme.outline)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.settings_channels_line, bengaliDigits(state.channelCount)),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(text = stringResource(R.string.settings_youtube_open), icon = Icons.Default.Key, onClick = onOpen)
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, top = 6.dp)
    )
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f)),
        content = content
    )
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier.size(40.dp).background(tint.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, iconTint)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelLarge, color = scheme.outline)
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 68.dp)
    )
}

/** "১,২০০" for 1200: grouped, in Bengali digits. */
private fun bengaliNumber(n: Int): String = bengaliDigits(n).let { digits ->
    digits.reversed().chunked(3).joinToString(",").reversed()
}

/** A settings card whose body expands when the header row is tapped. */
@Composable
private fun ExpandableSettingsCard(
    icon: ImageVector,
    title: String,
    testTag: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    SettingsCard(modifier = Modifier.testTag(testTag)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            IconBadge(icon, scheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.labelLarge, color = scheme.outline)
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                tint = scheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun RssFeedsSection(
    feeds: List<RssFeedItem>,
    onAdd: (title: String, url: String) -> Unit,
    onRemove: (String) -> Unit
) {
    var showInput by rememberSaveable { mutableStateOf(true) }
    var title by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }

    SectionHeader(
        text = stringResource(R.string.settings_rss_add_new),
        addDescription = stringResource(R.string.rss_add_feed),
        toggleTestTag = "toggle_add_rss_button",
        onToggle = { showInput = !showInput }
    )

    if (showInput) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth().testTag("rss_title_input"),
            placeholder = { HintText(stringResource(R.string.settings_rss_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.weight(1f).testTag("rss_url_input"),
                placeholder = { HintText(stringResource(R.string.rss_url_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            PrimaryButton(
                text = stringResource(R.string.action_add),
                modifier = Modifier.testTag("add_rss_submit_button")
            ) {
                if (url.isNotBlank()) {
                    onAdd(title, url)
                    title = ""
                    url = ""
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SuggestedFeedChips(added = feeds, onAdd = onAdd)
    }

    if (feeds.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            feeds.forEach { feed ->
                RemovableRow(
                    title = feed.title,
                    subtitle = feed.url,
                    removeDescription = stringResource(R.string.action_remove),
                    onRemove = { onRemove(feed.url) }
                )
            }
        }
    }
}

/** Horizontal row of popular feeds; already-added ones are marked and inert. */
@Composable
fun SuggestedFeedChips(added: List<RssFeedItem>, onAdd: (title: String, url: String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = stringResource(R.string.rss_popular_title),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = scheme.outline
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SuggestedRssFeeds.forEach { preset ->
            val isAdded = added.any { it.url.equals(preset.url, ignoreCase = true) }
            val shape = RoundedCornerShape(14.dp)
            Surface(
                color = if (isAdded) scheme.primary.copy(alpha = 0.2f) else scheme.surface,
                shape = shape,
                modifier = Modifier
                    .clip(shape)
                    .border(1.dp, if (isAdded) scheme.primary else scheme.outlineVariant, shape)
                    .clickable(enabled = !isAdded) { onAdd(preset.title, preset.url) }
            ) {
                Text(
                    text = (if (isAdded) "✓ " else "+ ") + preset.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAdded) scheme.primary else scheme.onBackground,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordsSection(
    keywords: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var newKeyword by rememberSaveable { mutableStateOf("") }
    val submit = {
        if (newKeyword.isNotBlank()) {
            onAdd(newKeyword.trim())
            newKeyword = ""
        }
    }

    if (keywords.isEmpty()) {
        Text(stringResource(R.string.settings_keywords_empty), style = MaterialTheme.typography.bodyMedium, color = scheme.outline)
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keywords.forEach { keyword -> KeywordChip(keyword, onRemove = { onRemove(keyword) }) }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newKeyword,
            onValueChange = { newKeyword = it },
            modifier = Modifier.weight(1f),
            placeholder = { HintText(stringResource(R.string.settings_keyword_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() })
        )
        Spacer(modifier = Modifier.width(8.dp))
        PrimaryButton(text = stringResource(R.string.action_add), icon = Icons.Default.Add, onClick = submit)
    }
}

/** One keyword as a pill with a ✕ to remove it. */
@Composable
private fun KeywordChip(keyword: String, onRemove: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(scheme.primary.copy(alpha = 0.12f))
            .border(1.dp, scheme.primary.copy(alpha = 0.35f), shape)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(keyword, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = scheme.primary)
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.action_remove),
                tint = scheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, addDescription: String, toggleTestTag: String? = null, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(32.dp)
                .background(scheme.primary, RoundedCornerShape(50))
                .then(if (toggleTestTag != null) Modifier.testTag(toggleTestTag) else Modifier)
        ) {
            Icon(Icons.Default.Add, contentDescription = addDescription, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

/** A bordered row with a title (and optional subtitle) and a red remove button. */
@Composable
private fun RemovableRow(title: String, removeDescription: String, onRemove: () -> Unit, subtitle: String? = null) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface, shape)
            .border(1.dp, scheme.outlineVariant, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (subtitle != null) FontWeight.Bold else FontWeight.Normal,
                color = scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = scheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(30.dp).background(scheme.errorContainer, RoundedCornerShape(50))
        ) {
            Icon(Icons.Default.Delete, contentDescription = removeDescription, tint = scheme.onErrorContainer, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HintText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
}
