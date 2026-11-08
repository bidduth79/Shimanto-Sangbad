package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.GlowColors

/** Red used to show that a keyword filter is narrowing the news. */
private val ActiveFilterRed = Color(0xFFE11D48)

/**
 * Header button that opens the keyword menu. It turns red, with a dot, while a keyword other than
 * "all news" is selected, so a narrowed list is never mistaken for everything.
 */
@Composable
fun KeywordFilterButton(
    keywords: List<String>,
    selectedKeyword: String?,
    keywordCounts: Map<String, Int>,
    onSelectKeyword: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = selectedKeyword != null
    val scheme = MaterialTheme.colorScheme

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isActive) ActiveFilterRed.copy(alpha = 0.14f) else Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = stringResource(R.string.header_keywords),
                tint = if (isActive) ActiveFilterRed else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .size(9.dp)
                    .border(1.5.dp, scheme.background, CircleShape)
                    .background(ActiveFilterRed, CircleShape)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 6.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = scheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant),
            modifier = Modifier.widthIn(min = 250.dp, max = 300.dp)
        ) {
            MenuHeader()
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))
            KeywordRow(
                label = stringResource(R.string.header_all_news),
                icon = Icons.Default.Newspaper,
                accent = scheme.primary,
                count = null,
                selected = !isActive,
                onClick = {
                    onSelectKeyword(null)
                    expanded = false
                }
            )
            keywords.forEachIndexed { index, keyword ->
                KeywordRow(
                    label = keyword,
                    icon = Icons.AutoMirrored.Filled.Label,
                    accent = GlowColors[index % GlowColors.size],
                    count = keywordCounts[keyword] ?: 0,
                    selected = keyword == selectedKeyword,
                    onClick = {
                        onSelectKeyword(keyword)
                        expanded = false
                    }
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
        }
    }
}

@Composable
private fun MenuHeader() {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(scheme.primary.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FilterList, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(stringResource(R.string.keyword_menu_title), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = scheme.onSurface)
            Text(stringResource(R.string.keyword_menu_subtitle), style = MaterialTheme.typography.labelSmall, color = scheme.outline)
        }
    }
}

/** One menu entry: tinted icon tile, label, story count and a check when selected. */
@Composable
private fun KeywordRow(
    label: String,
    icon: ImageVector,
    accent: Color,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(accent.copy(alpha = 0.18f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.primary else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) scheme.onSurfaceVariant else scheme.outline,
                modifier = Modifier
                    .background(scheme.surfaceVariant, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
