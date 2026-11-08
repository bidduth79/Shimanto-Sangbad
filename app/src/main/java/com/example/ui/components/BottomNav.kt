package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// Bottom navigation bar: a floating glass pill over the content.

private data class NavTab(val filled: ImageVector, val outlined: ImageVector, val label: Int)

private val NavTabs = listOf(
    NavTab(Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home),
    NavTab(Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle, R.string.nav_video),
    NavTab(Icons.Filled.RssFeed, Icons.Outlined.RssFeed, R.string.nav_rss),
    NavTab(Icons.Filled.Public, Icons.Outlined.Public, R.string.nav_live),
    NavTab(Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, R.string.nav_saved),
    NavTab(Icons.Filled.Settings, Icons.Outlined.Settings, R.string.nav_settings)
)

/**
 * @param hazeState the content's blur source; the bar shows it frosted behind itself (Android 12+,
 *   a translucent tint on older phones).
 * @param onTabSelected called for every tap, also on the open tab (the caller scrolls it to the top).
 */
@Composable
fun BottomNavBar(
    selectedTab: Int,
    savedCount: Int = 0,
    rssCount: Int = 0,
    newNewsCount: Int = 0,
    newVideoCount: Int = 0,
    hazeState: HazeState? = null,
    onTabSelected: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(28.dp)
    val glass = HazeStyle(
        backgroundColor = scheme.background,
        tint = HazeTint(scheme.surfaceVariant.copy(alpha = if (isDark) 0.62f else 0.70f)),
        blurRadius = 24.dp,
        noiseFactor = 0.05f,
        fallbackTint = HazeTint(scheme.surfaceVariant.copy(alpha = 0.94f))
    )
    val view = LocalView.current
    val badges = mapOf(0 to newNewsCount, 1 to newVideoCount, 2 to rssCount, 4 to savedCount)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(shape)
            .then(if (hazeState != null) Modifier.hazeEffect(state = hazeState, style = glass) else Modifier),
        shape = shape,
        color = if (hazeState != null) Color.Transparent else scheme.surfaceVariant,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabs.forEachIndexed { index, tab ->
                BottomNavItem(
                    icon = if (selectedTab == index) tab.filled else tab.outlined,
                    label = stringResource(tab.label),
                    isSelected = selectedTab == index,
                    badgeCount = badges[index] ?: 0
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onTabSelected(index)
                }
            }
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
    // A small springy pop when a tab becomes selected
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "nav_icon_scale"
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
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
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
