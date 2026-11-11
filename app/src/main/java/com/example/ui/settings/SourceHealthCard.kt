package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SourceHealth
import com.example.data.SourceHealth.Kind
import com.example.data.SourceHealth.Level
import com.example.ui.components.LocalNow
import com.example.ui.components.bengaliDigits

/** How many sources have a problem right now (shown in the card's subtitle). */
fun problemCount(statuses: List<SourceHealth.Status>, now: Long): Int =
    statuses.count { SourceHealth.levelOf(it, now).let { level -> level == Level.DOWN || level == Level.STALE } }

/**
 * "উৎসের অবস্থা": what each news source last did, as seen from this phone. Search engines one per
 * row; newspaper feeds as a count, with the ones in trouble listed by name and reason.
 */
@Composable
fun SourceHealthBody(statuses: List<SourceHealth.Status>, lastRoundAt: Long) {
    val scheme = MaterialTheme.colorScheme
    val now = LocalNow.current
    if (statuses.isEmpty()) {
        Text(stringResource(R.string.health_waiting), style = MaterialTheme.typography.bodyMedium, color = scheme.outline)
        return
    }
    if (lastRoundAt > 0L) {
        Text(
            stringResource(R.string.health_last_refresh, ago(now - lastRoundAt)),
            style = MaterialTheme.typography.labelLarge,
            color = scheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    statuses.filter { it.kind == Kind.SEARCH }.sortedBy { it.label }.forEach { status ->
        val level = SourceHealth.levelOf(status, now)
        val detail = when (level) {
            Level.DOWN -> stringResource(R.string.health_down, status.lastError ?: "?")
            Level.STALE -> stringResource(R.string.health_stale, ago(now - status.newestItemAt))
            else -> stringResource(
                R.string.health_search_ok,
                ago(now - status.newestItemAt),
                bengaliDigits(status.requests - status.failedRequests),
                bengaliDigits(status.requests)
            )
        }
        HealthRow(status.label, level, detail)
    }

    val feeds = statuses.filter { it.kind == Kind.NEWSPAPER }
    if (feeds.isNotEmpty()) {
        val troubled = feeds.filter { SourceHealth.levelOf(it, now).let { l -> l == Level.DOWN || l == Level.STALE } }
        HealthRow(
            label = stringResource(R.string.health_feeds),
            level = if (troubled.size * 4 > feeds.size) Level.STALE else Level.OK,
            detail = stringResource(R.string.health_feeds_count, bengaliDigits(feeds.size - troubled.size), bengaliDigits(feeds.size))
        )
        troubled.sortedBy { it.label }.take(MAX_TROUBLED_SHOWN).forEach { status -> FeedProblem(status, now) }
        if (troubled.size > MAX_TROUBLED_SHOWN) {
            Text(
                stringResource(R.string.health_more, bengaliDigits(troubled.size - MAX_TROUBLED_SHOWN)),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.outline,
                modifier = Modifier.padding(start = 20.dp, top = 2.dp)
            )
        }
    }

    statuses.filter { it.kind == Kind.CUSTOM }.sortedBy { it.label }.forEach { status ->
        val level = SourceHealth.levelOf(status, now)
        val detail = when {
            level == Level.DOWN -> stringResource(R.string.health_down, status.lastError ?: "?")
            status.newestItemAt > 0L -> stringResource(R.string.health_feed_newest, ago(now - status.newestItemAt))
            else -> stringResource(R.string.health_ok)
        }
        HealthRow(stringResource(R.string.health_custom, status.label), level, detail)
    }
}

@Composable
private fun FeedProblem(status: SourceHealth.Status, now: Long) {
    val level = SourceHealth.levelOf(status, now)
    val reason = if (level == Level.DOWN) stringResource(R.string.health_down, status.lastError ?: "?")
    else stringResource(R.string.health_feed_newest, ago(now - status.newestItemAt))
    Text(
        text = "• ${status.label} — $reason",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 20.dp, top = 2.dp)
    )
}

@Composable
private fun HealthRow(label: String, level: Level, detail: String) {
    val scheme = MaterialTheme.colorScheme
    val dot = when (level) {
        Level.OK -> Color(0xFF16A34A)
        Level.STALE -> Color(0xFFF59E0B)
        Level.DOWN -> Color(0xFFDC2626)
        Level.UNKNOWN -> scheme.outlineVariant
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(dot, CircleShape))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = if (level == Level.DOWN) dot else scheme.outline)
        }
    }
}

/** "৫ মিনিট", "৩ ঘণ্টা", "২ দিন". */
@Composable
private fun ago(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    return when {
        minutes < 60 -> stringResource(R.string.health_minutes, bengaliDigits(minutes.toInt()))
        minutes < 48 * 60 -> stringResource(R.string.health_hours, bengaliDigits((minutes / 60).toInt()))
        else -> stringResource(R.string.health_days, bengaliDigits((minutes / 1440).toInt()))
    }
}

private const val MAX_TROUBLED_SHOWN = 10
