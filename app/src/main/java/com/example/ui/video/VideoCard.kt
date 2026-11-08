package com.example.ui.video

import com.example.ui.components.copyToClipboard
import com.example.ui.components.shareToWhatsApp
import com.example.ui.components.toast
import androidx.compose.runtime.remember
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.YouTubeSearchResult
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.ChannelInfo
import com.example.data.SourceKind
import com.example.data.VideoRegion
import com.example.ui.components.visitedColor
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.CheckCircle

// One news video with thumbnail, channel, time and share actions.

@Composable
fun VideoCard(
    video: YouTubeSearchResult,
    isWatched: Boolean = false,
    info: ChannelInfo = ChannelInfo(VideoRegion.BANGLADESH, SourceKind.NATIONAL),
    showRegion: Boolean = true,
    onInAppPlay: () -> Unit,
    onExternalPlay: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val snippet = video.snippet
    val title = snippet?.title ?: stringResource(R.string.video_untitled)
    val channelTitle = snippet?.channelTitle ?: ""
    val publishedAt = snippet?.publishedAt
    val resources = LocalContext.current.resources
    val formattedTime = remember(publishedAt) { formatYouTubeDate(publishedAt, resources) }
    val videoId = video.id?.videoId ?: ""
    val thumbnailUrl = snippet?.thumbnails?.high?.url
        ?: snippet?.thumbnails?.medium?.url
        ?: snippet?.thumbnails?.default?.url
        ?: if (videoId.isNotBlank()) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInAppPlay),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface)
    ) {
        Column {
            if (thumbnailUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.video_play),
                                tint = scheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 6.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isWatched) visitedColor(scheme) else scheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                com.example.ui.components.TranslatedLine(title, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (channelTitle.isNotBlank()) {
                        Text(
                            text = channelTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (channelTitle.isNotBlank() && formattedTime.isNotBlank()) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.outline
                        )
                    }
                    if (formattedTime.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = stringResource(R.string.video_time),
                                tint = scheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (showRegion) {
                        VideoTag(
                            text = stringResource(
                                when (info.region) {
                                    VideoRegion.BANGLADESH -> R.string.tab_bangladesh
                                    VideoRegion.INDIA -> R.string.tab_india
                                    VideoRegion.MYANMAR -> R.string.tab_myanmar
                                    VideoRegion.INTERNATIONAL -> R.string.tab_international
                                }
                            ),
                            color = when (info.region) {
                                VideoRegion.BANGLADESH -> Color(0xFF16A34A)
                                VideoRegion.INDIA -> Color(0xFFF97316)
                                VideoRegion.MYANMAR -> Color(0xFFEAB308)
                                VideoRegion.INTERNATIONAL -> Color(0xFF3B82F6)
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    VideoTag(
                        text = stringResource(
                            when (info.kind) {
                                SourceKind.TV -> R.string.video_kind_tv
                                SourceKind.NATIONAL -> R.string.video_kind_national
                                SourceKind.LOCAL -> R.string.video_kind_local
                                SourceKind.OTHER -> R.string.video_kind_other
                            }
                        ),
                        color = if (info.kind == SourceKind.OTHER) Color(0xFF94A3B8) else scheme.primary
                    )
                    if (isWatched) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.video_filter_watched),
                            tint = visitedColor(scheme),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    val context = LocalContext.current
                    CardAction(Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.video_youtube), scheme.onSurfaceVariant, onExternalPlay)
                    CardAction(Icons.AutoMirrored.Filled.Send, stringResource(R.string.news_share_whatsapp), Color(0xFF25D366)) {
                        if (videoId.isNotBlank()) context.shareToWhatsApp("$title\nhttps://www.youtube.com/watch?v=$videoId")
                    }
                    CardAction(Icons.Default.ContentCopy, stringResource(R.string.action_copy_link), scheme.primary) {
                        if (videoId.isNotBlank()) {
                            context.copyToClipboard("YouTube", "https://www.youtube.com/watch?v=$videoId")
                            context.toast(context.getString(R.string.toast_video_link_copied))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoTag(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun CardAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}
