package com.example.ui.live

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.NewsArticle
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.isSystemInDarkModeOf
import com.example.ui.components.translucentCircles

private val FacebookBlue = Color(0xFF1877F2)
private val WhatsAppGreen = Color(0xFF25D366)

/** A Facebook / social post in the live feed. */
@Composable
fun SocialNewsCard(
    article: NewsArticle,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Surface(
        color = scheme.surface,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FacebookBlue.copy(alpha = 0.35f), shape)
            .clickable(onClick = onOpen)
    ) {
        // Circles drawn inside the Surface, so they are clipped to its rounded shape
        Column(
            modifier = Modifier
                .translucentCircles(FacebookBlue, article.title.hashCode(), isSystemInDarkModeOf(scheme))
                .padding(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = FacebookBlue.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = article.matchedKeyword.ifBlank { stringResource(R.string.live_social_tag) },
                        color = FacebookBlue,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = article.source,
                    color = scheme.outline,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBookmark, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (article.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.news_bookmark),
                        tint = if (article.isSaved) scheme.primary else scheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatRelativeTime(article.timestamp, article.pubDate),
                    color = scheme.outline,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.news_share_whatsapp),
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.news_copy_link),
                            tint = scheme.onBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
