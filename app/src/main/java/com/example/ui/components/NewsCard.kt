package com.example.ui.components

import com.example.data.KeywordMatcher
import com.example.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.Send
import android.text.format.DateUtils
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.NewsArticle
import com.example.ui.theme.*
import com.example.ui.regionLabel

// A single news item card and its small helpers.

/** The user keyword this article is about (shown as its tag), or "" when none applies. */
fun getDisplayKeyword(article: NewsArticle, keywords: List<String> = emptyList()): String =
    article.matchedKeyword.ifBlank { KeywordMatcher.matchingKeyword(article.title, keywords).orEmpty() }

@Composable
fun NewsCard(
    article: NewsArticle,
    isVisited: Boolean,
    isSpeaking: Boolean = false,
    keywords: List<String> = emptyList(),
    showRegion: Boolean = false,
    parallax: (() -> Float)? = null,
    onArticleClick: (NewsArticle) -> Unit,
    onSpeakClick: (NewsArticle) -> Unit = {},
    onCopyClick: (NewsArticle) -> Unit,
    onWhatsAppClick: (NewsArticle) -> Unit,
    onBookmarkClick: (NewsArticle) -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val accentColor = GlowColors[Math.abs(article.title.hashCode()) % GlowColors.size]
    val titleColor = if (isVisited) visitedColor(scheme) else scheme.onSurface
    val badgeTag = getDisplayKeyword(article, keywords)
    val isFacebookSource = article.source.contains("Facebook", ignoreCase = true) ||
        article.source.contains("FB/", ignoreCase = true)
    val cardShape = RoundedCornerShape(20.dp)

    val isDark = isSystemInDarkModeOf(scheme)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // Light mode: white cards on a grey page need a soft shadow to stand out. Dark mode
            // separates by colour alone, so it skips the GPU shadow.
            .then(if (isDark) Modifier else Modifier.shadow(3.dp, cardShape, ambientColor = CardShadow, spotColor = CardShadow))
            .clip(cardShape)
            .background(scheme.surface)
            .translucentCircles(accentColor, seed = article.title.hashCode(), isDark = isDark, parallax = parallax)
            .border(
                width = if (isSpeaking) 1.5.dp else 1.dp,
                color = when {
                    isSpeaking -> scheme.primary
                    isDark -> scheme.outlineVariant.copy(alpha = 0.7f)
                    else -> scheme.outlineVariant
                },
                shape = cardShape
            )
            .clickable { onArticleClick(article) }
            .padding(start = 14.dp, end = 6.dp, top = 14.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source Monogram Avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accentColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = article.source.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.source,
                    color = if (isFacebookSource) Color(0xFF38BDF8) else scheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatRelativeTime(article.timestamp, article.pubDate),
                    color = scheme.outline,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (badgeTag.isNotBlank()) {
                    Text(
                        text = badgeTag,
                        color = scheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .background(scheme.primary.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
                if (showRegion) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = article.regionLabel(),
                        color = scheme.secondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .background(scheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (!article.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(96.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.surfaceVariant)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isVisited) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = visitedColor(scheme),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.news_read), color = visitedColor(scheme), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.weight(1f))
            // Audio Listen (TTS) Button
            CardAction(
                icon = if (isSpeaking) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.VolumeUp,
                description = stringResource(R.string.news_listen),
                tint = if (isSpeaking) Color(0xFFEF4444) else scheme.onSurfaceVariant
            ) { onSpeakClick(article) }
            CardAction(Icons.AutoMirrored.Filled.Send, stringResource(R.string.news_share_whatsapp), Color(0xFF25D366)) { onWhatsAppClick(article) }
            CardAction(Icons.Default.ContentCopy, stringResource(R.string.news_copy_link), scheme.onSurfaceVariant) { onCopyClick(article) }
            CardAction(
                icon = if (article.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                description = stringResource(R.string.news_bookmark),
                tint = if (article.isSaved) scheme.primary else scheme.onSurfaceVariant
            ) { onBookmarkClick(article) }
        }
    }
}

@Composable
private fun CardAction(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(19.dp))
    }
}

fun formatRelativeTime(timestamp: Long, fallback: String): String {
    if (timestamp == 0L) return fallback
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

/** Soft, slightly cool shadow for light-mode cards. */
internal val CardShadow = Color(0x330F172A)

/** True when [scheme] is the app's dark palette (the in-app toggle can differ from the system setting). */
internal fun isSystemInDarkModeOf(scheme: ColorScheme): Boolean = scheme.background.luminance() < 0.5f

/** Purple for headlines already opened, like visited links on the web; tuned per theme for contrast. */
internal fun visitedColor(scheme: ColorScheme): Color =
    if (isSystemInDarkModeOf(scheme)) Color(0xFFC4B5FD) else Color(0xFF7C3AED)
