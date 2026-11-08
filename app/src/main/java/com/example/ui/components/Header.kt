package com.example.ui.components

import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale

// Top app bar: title, date, search, theme toggle and overflow actions.

@Composable
fun Header(
    query: String,
    isRefreshing: Boolean = false,
    isDarkMode: Boolean = true,
    areFilterChipsVisible: Boolean = true,
    onToggleFilterChips: (() -> Unit)? = null,
    onToggleDarkMode: (() -> Unit)? = null,
    onQueryChange: (String) -> Unit,
    keywords: List<String>,
    selectedKeyword: String? = null,
    keywordCounts: Map<String, Int> = emptyMap(),
    onSelectKeyword: (String?) -> Unit = {},
    onRefreshClick: (() -> Unit)? = null,
    onOpenYouTubeSettings: (() -> Unit)? = null
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var expandedMoreMenu by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    val todayLabel = remember {
        java.text.SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("bn-BD")).format(java.util.Date())
    }

    Column {
        if (isSearchExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(scheme.surfaceVariant)
                    .border(1.dp, scheme.primary.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isSearchExpanded = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.header_back), tint = scheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.header_search_hint), color = scheme.outline, style = MaterialTheme.typography.bodyLarge)
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onBackground),
                        cursorBrush = SolidColor(scheme.primary),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.header_clear), tint = scheme.outline)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.bgb_emblem_header),
                    contentDescription = null, // decorative: the app name is right next to it
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = todayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.outline,
                        maxLines = 1
                    )
                }

                HeaderAction(Icons.Default.Search, stringResource(R.string.header_search)) { isSearchExpanded = true }

                if (onToggleDarkMode != null) {
                    HeaderAction(
                        icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        description = stringResource(if (isDarkMode) R.string.header_light_mode else R.string.header_dark_mode),
                        tint = if (isDarkMode) Color(0xFFFBBF24) else scheme.onSurfaceVariant,
                        onClick = onToggleDarkMode
                    )
                }

                KeywordFilterButton(
                    keywords = keywords,
                    selectedKeyword = selectedKeyword,
                    keywordCounts = keywordCounts,
                    onSelectKeyword = onSelectKeyword
                )

                Box {
                    HeaderAction(Icons.Default.MoreVert, stringResource(R.string.header_more)) { expandedMoreMenu = true }
                    DropdownMenu(
                        expanded = expandedMoreMenu,
                        onDismissRequest = { expandedMoreMenu = false }
                    ) {
                        if (onRefreshClick != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.header_refresh)) },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    onRefreshClick()
                                    expandedMoreMenu = false
                                }
                            )
                        }
                        if (onToggleFilterChips != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(if (areFilterChipsVisible) R.string.header_hide_filters else R.string.header_show_filters)) },
                                leadingIcon = {
                                    Icon(
                                        if (areFilterChipsVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    onToggleFilterChips()
                                    expandedMoreMenu = false
                                }
                            )
                        }
                        if (onOpenYouTubeSettings != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.header_youtube_settings)) },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                                onClick = {
                                    onOpenYouTubeSettings()
                                    expandedMoreMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(50)),
                color = scheme.primary,
                trackColor = scheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    description: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}
