package com.example.ui.components

import com.example.ui.home.HomeFilter
import com.example.ui.home.FilterCounts
import com.example.data.SearchMedium
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// Country tabs and source/read-state filter chips above the news list.

@Composable
fun NewsCategoryTabBar(
    selectedTab: Int,
    allCount: Int,
    bdCount: Int,
    indianCount: Int,
    myanmarCount: Int,
    internationalCount: Int,
    onSelectTab: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()
    val tabs = listOf(
        Triple(0, stringResource(R.string.tab_all), allCount),
        Triple(1, stringResource(R.string.tab_bangladesh), bdCount),
        Triple(2, stringResource(R.string.tab_india), indianCount),
        Triple(3, stringResource(R.string.tab_myanmar), myanmarCount),
        Triple(4, stringResource(R.string.tab_international), internationalCount)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (index, label, count) ->
            val isSelected = selectedTab == index
            val containerColor by animateColorAsState(
                if (isSelected) scheme.primary else scheme.surfaceVariant,
                label = "tab_bg"
            )
            val contentColor by animateColorAsState(
                if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                label = "tab_fg"
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(containerColor)
                    .clickable { onSelectTab(index) }
                    .padding(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .background(
                            if (isSelected) scheme.onPrimary else scheme.outlineVariant,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 7.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun FilterChipsBar(
    isAllTab: Boolean,
    selectedFilter: String,
    counts: FilterCounts,
    maxAgeHours: Int? = null,
    onSelectMaxAge: (Int?) -> Unit = {},
    onlyUnread: Boolean = false,
    onToggleOnlyUnread: () -> Unit = {},
    onSelectFilter: (String) -> Unit
) {
    // "All" tab: read state. Country tab: kind of source (empty kinds hidden) + "only unread" toggle.
    val chips = if (isAllTab) listOf(
        Triple(HomeFilter.ALL, R.string.filter_all, counts.total),
        Triple(HomeFilter.UNREAD, R.string.filter_unread, counts.unread),
        Triple(HomeFilter.READ, R.string.filter_read, counts.read)
    ) else listOf(
        Triple(HomeFilter.ALL, R.string.filter_all, counts.total),
        Triple(HomeFilter.TV, R.string.video_kind_tv, counts.tv),
        Triple(HomeFilter.NATIONAL, R.string.video_kind_national, counts.national),
        Triple(HomeFilter.LOCAL, R.string.video_kind_local, counts.local),
        Triple(HomeFilter.OTHER, R.string.video_kind_other, counts.other)
    ).filter { (key, _, count) -> key == HomeFilter.ALL || key == selectedFilter || count > 0 }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeFilterChip(maxAgeHours = maxAgeHours, onSelect = onSelectMaxAge)
        chips.forEach { (key, label, count) ->
            FilterChipItem(
                label = stringResource(R.string.filter_with_count, stringResource(label), count),
                isSelected = selectedFilter == key,
                onClick = { onSelectFilter(key) }
            )
        }
        if (!isAllTab) OnlyUnreadToggle(label = stringResource(R.string.filter_only_unread), checked = onlyUnread, onToggle = onToggleOnlyUnread)
    }
}

/** "Only unread/unwatched" switch at the end of a country tab's chips: an eye, red while active. */
@Composable
fun OnlyUnreadToggle(label: String, checked: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val active = Color(0xFFE11D48)
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (checked) active.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (checked) active else scheme.outlineVariant, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (checked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = null,
            tint = if (checked) active else scheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
            color = if (checked) active else scheme.onSurfaceVariant
        )
    }
}

@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        if (isSelected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        label = "chip_bg"
    )
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (isSelected) scheme.primary.copy(alpha = 0.6f) else scheme.outlineVariant,
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}
