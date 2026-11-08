package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min

/**
 * Soft, translucent decorative circles behind a card's content: a large one off the top-right
 * corner (with a thin ring), a medium one off the bottom edge and a small accent dot.
 *
 * [parallax] (the card position in the list, see [viewportPositionOf]) makes the circles drift
 * against the scroll at different depths, so they seem to sit on a layer behind the text. It is
 * read here, in the draw phase, so scrolling only redraws the card.
 *
 * [seed] (e.g. the headline's hash) nudges the positions so neighbouring cards don't look stamped
 * out of one mould. Apply after the card's background and clip, before its padding; the circles are
 * drawn behind the text and clipped to the card's shape.
 */
fun Modifier.translucentCircles(
    color: Color,
    seed: Int,
    isDark: Boolean,
    parallax: (() -> Float)? = null
): Modifier = drawBehind {
    val w = size.width
    val h = size.height
    val base = min(w, h)
    val s = abs(seed)
    // Small, stable per-card variation in [-1, 1]
    val jx = ((s % 7) - 3) / 3f
    val jy = (((s / 7) % 7) - 3) / 3f
    // Translucency tuned so the circles read as texture, never compete with the text
    val strong = if (isDark) 0.13f else 0.10f
    val soft = if (isDark) 0.07f else 0.06f

    // Parallax: as the card travels up the screen the circles lag behind it (big = deepest layer)
    val shift = -(parallax?.invoke() ?: 0f) * base * 0.22f

    val bigCenter = Offset(w * (0.93f + 0.03f * jx), h * (0.02f + 0.06f * jy) + shift)
    val bigRadius = base * 0.62f
    drawCircle(color = color.copy(alpha = strong), radius = bigRadius, center = bigCenter)
    drawCircle(
        color = color.copy(alpha = soft),
        radius = bigRadius + 14.dp.toPx(),
        center = bigCenter,
        style = Stroke(width = 1.5.dp.toPx())
    )

    drawCircle(
        color = color.copy(alpha = soft),
        radius = base * 0.34f,
        center = Offset(w * (0.70f + 0.06f * jy), h * 1.02f + shift * 0.6f)
    )

    drawCircle(
        color = color.copy(alpha = strong),
        radius = base * 0.06f,
        center = Offset(w * (0.56f + 0.05f * jx), h * 0.18f - shift * 0.4f)
    )
}
