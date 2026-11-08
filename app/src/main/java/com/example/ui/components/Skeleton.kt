package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

// Placeholder cards shown while the first news load is in progress.

@Composable
fun NewsSkeletonCard() {
    val scheme = MaterialTheme.colorScheme
    val shimmer = rememberInfiniteTransition(label = "skeleton_shimmer")
    val shimmerAlpha by shimmer.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val block = scheme.outlineVariant.copy(alpha = shimmerAlpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface, RoundedCornerShape(20.dp))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(26.dp).background(block, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(110.dp).height(12.dp).background(block, RoundedCornerShape(6.dp)))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(block, RoundedCornerShape(6.dp)))
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth(0.75f).height(16.dp).background(block, RoundedCornerShape(6.dp)))
            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.width(80.dp).height(10.dp).background(block, RoundedCornerShape(6.dp)))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(modifier = Modifier.width(96.dp).height(72.dp).background(block, RoundedCornerShape(14.dp)))
    }
}

@Composable
fun SkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            NewsSkeletonCard()
        }
    }
}

/** Placeholder video card (thumbnail, two title lines, channel line) while the first videos load. */
@Composable
fun VideoSkeletonCard() {
    val scheme = MaterialTheme.colorScheme
    val shimmer = rememberInfiniteTransition(label = "video_skeleton")
    val alpha by shimmer.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "video_skeleton_alpha"
    )
    val block = scheme.outlineVariant.copy(alpha = alpha)
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface, shape)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(block, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(48.dp).background(scheme.surface.copy(alpha = 0.5f), CircleShape))
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(16.dp).background(block, RoundedCornerShape(6.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).background(block, RoundedCornerShape(6.dp)))
            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.width(160.dp).height(11.dp).background(block, RoundedCornerShape(6.dp)))
        }
    }
}

/** A column of [VideoSkeletonCard]s with a short "loading" note on top. */
@Composable
fun VideoSkeletonList(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
        }
        repeat(3) { VideoSkeletonCard() }
    }
}
