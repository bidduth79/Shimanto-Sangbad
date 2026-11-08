package com.example.ui.components

import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

// Bottom navigation bar.

@Composable
fun BottomNavBar(
    selectedTab: Int,
    savedCount: Int = 0,
    rssCount: Int = 0,
    newNewsCount: Int = 0,
    newVideoCount: Int = 0,
    onTabSelected: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceVariant,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Default.Home, stringResource(R.string.nav_home), selectedTab == 0, newNewsCount) { onTabSelected(0) }
            BottomNavItem(Icons.Default.PlayCircle, stringResource(R.string.nav_video), selectedTab == 1, newVideoCount) { onTabSelected(1) }
            BottomNavItem(Icons.Default.RssFeed, stringResource(R.string.nav_rss), selectedTab == 2, rssCount) { onTabSelected(2) }
            BottomNavItem(Icons.Default.Public, stringResource(R.string.nav_live), selectedTab == 3) { onTabSelected(3) }
            BottomNavItem(Icons.Default.Bookmark, stringResource(R.string.nav_saved), selectedTab == 4, savedCount) { onTabSelected(4) }
            BottomNavItem(Icons.Default.Settings, stringResource(R.string.nav_settings), selectedTab == 5) { onTabSelected(5) }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor by animateColorAsState(
        if (isSelected) scheme.primary else Color.Transparent,
        label = "nav_bg"
    )
    val contentColor by animateColorAsState(
        if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
        label = "nav_fg"
    )
    // Selected tab expands into a labelled pill; the rest stay icon-only so all six fit without scrolling
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .clickable { onClick() }
            .animateContentSize()
            .padding(horizontal = if (isSelected) 14.dp else 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0 && !isSelected) {
                    Badge(containerColor = scheme.primary, contentColor = scheme.onPrimary) {
                        Text(if (badgeCount > 99) "99+" else badgeCount.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(21.dp)
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
