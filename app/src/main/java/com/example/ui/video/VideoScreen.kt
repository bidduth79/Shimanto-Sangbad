package com.example.ui.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.YouTubeSearchResult
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.components.FilterChipItem
import com.example.ui.components.LocalBottomBarSpace
import com.example.ui.components.ScrollToTopOnReselect
import com.example.ui.components.OnlyUnreadToggle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.example.ui.components.NewsCategoryTabBar
import com.example.ui.components.TimeFilterChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable

// Video tab: time-range chips and the list of news videos.

@Composable
fun VideoList(
    videos: List<YouTubeSearchResult>,
    errorMessage: String? = null,
    maxAgeHours: Int? = null,
    onSelectMaxAge: (Int?) -> Unit = {},
    mediaChannelIds: Set<String> = emptySet(),
    watchedLinks: Set<String> = emptySet(),
    onWatched: (String) -> Unit = {},
    isRefreshing: Boolean = false,
    showFilters: Boolean = true,
    onRetry: (() -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val scheme = MaterialTheme.colorScheme
    var activePlayingVideo by remember { mutableStateOf<YouTubeSearchResult?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf(VideoFilter.ALL) }
    var onlyUnwatched by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    if (activePlayingVideo != null) {
        InAppVideoPlayerDialog(
            video = activePlayingVideo!!,
            onDismiss = { activePlayingVideo = null },
            onOpenInExternalApp = { videoId ->
                try {
                    uriHandler.openUri("https://www.youtube.com/watch?v=$videoId")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        )
    }

    if (!errorMessage.isNullOrBlank() && videos.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = stringResource(R.string.video_error_title),
                        tint = scheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.video_error_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.outline,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (onRetry != null) {
                        Button(
                            onClick = onRetry,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    } else if (videos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = scheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = stringResource(R.string.video_empty), color = scheme.outline, style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        val clock = com.example.ui.components.LocalNow.current
        val timeFilteredVideos = remember(videos, maxAgeHours, if (maxAgeHours != null) clock else 0L) {
            val now = System.currentTimeMillis()
            val cutoff = maxAgeHours?.let { now - it * 3_600_000L }
            if (cutoff == null) videos else videos.filter { video ->
                val ts = parseYouTubeTimestampMillis(video.snippet?.publishedAt)
                ts <= 0L || ts in cutoff..(now + 600_000L)
            }
        }

        val classifier = remember(videos, mediaChannelIds, watchedLinks) {
            VideoClassifier(videos, mediaChannelIds, watchedLinks)
        }
        val region = VideoTabs[selectedTab]
        val regionCounts = remember(classifier, timeFilteredVideos) { classifier.regionCounts(timeFilteredVideos) }
        val regionVideos = remember(classifier, timeFilteredVideos, region) {
            timeFilteredVideos.filter { classifier.inRegion(it, region) }
        }
        val counts = remember(classifier, regionVideos) { classifier.counts(regionVideos) }
        val shownVideos = remember(classifier, regionVideos, selectedFilter, onlyUnwatched) {
            regionVideos.filter {
                classifier.matches(it, selectedFilter) && (!onlyUnwatched || region == null || !classifier.isWatched(it))
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            androidx.compose.animation.AnimatedVisibility(visible = showFilters) {
                Column {
                    NewsCategoryTabBar(
                        selectedTab = selectedTab,
                        allCount = regionCounts.all,
                        bdCount = regionCounts.bangladesh,
                        indianCount = regionCounts.india,
                        myanmarCount = regionCounts.myanmar,
                        internationalCount = regionCounts.international,
                        onSelectTab = {
                            // each tab has its own chips, so start from "all" again
                            selectedTab = it
                            selectedFilter = VideoFilter.ALL
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeFilterChip(
                            maxAgeHours = maxAgeHours,
                            onSelect = onSelectMaxAge,
                            allLabel = stringResource(R.string.video_time_all)
                        )
                        val chips = if (region == null) listOf(
                            VideoFilter.ALL to (R.string.filter_all to counts.all),
                            VideoFilter.UNREAD to (R.string.video_filter_unwatched to counts.unread),
                            VideoFilter.READ to (R.string.video_filter_watched to counts.read)
                        ) else listOf(
                            VideoFilter.ALL to (R.string.filter_all to counts.all),
                            VideoFilter.TV to (R.string.video_kind_tv to counts.tv),
                            VideoFilter.NATIONAL to (R.string.video_kind_national to counts.national),
                            VideoFilter.LOCAL to (R.string.video_kind_local to counts.local),
                            VideoFilter.OTHER to (R.string.video_kind_other to counts.other)
                        )
                        chips
                            // empty kinds are hidden (unless selected), "all" and watched chips always show
                            .filter { (filter, lc) -> filter == selectedFilter || lc.second > 0 || region == null || filter == VideoFilter.ALL }
                            .forEach { (filter, labelAndCount) ->
                                FilterChipItem(
                                    label = stringResource(
                                        R.string.filter_with_count,
                                        stringResource(labelAndCount.first),
                                        labelAndCount.second
                                    ),
                                    isSelected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter }
                                )
                            }
                        if (region != null) OnlyUnreadToggle(
                            label = stringResource(R.string.video_only_unwatched),
                            checked = onlyUnwatched,
                            onToggle = { onlyUnwatched = !onlyUnwatched }
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = isRefreshing) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50)),
                        color = scheme.primary,
                        trackColor = scheme.surfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.video_refreshing),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.outline,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
            }

            if (shownVideos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = scheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.video_empty_in_range),
                            color = scheme.outline,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                ScrollToTopOnReselect(listState)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = LocalBottomBarSpace.current + 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(shownVideos, key = { it.id?.videoId ?: it.hashCode().toString() }) { video ->
                        val videoId = video.id?.videoId
                        if (!videoId.isNullOrBlank()) {
                            VideoCard(
                                video = video,
                                isWatched = classifier.isWatched(video),
                                info = classifier.infoOf(video),
                                showRegion = region == null,
                                onInAppPlay = {
                                    onWatched(videoId)
                                    activePlayingVideo = video
                                },
                                onExternalPlay = {
                                    onWatched(videoId)
                                    val url = "https://www.youtube.com/watch?v=$videoId"
                                    try {
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
