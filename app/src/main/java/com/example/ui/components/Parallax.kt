package com.example.ui.components

import android.provider.Settings
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user turned animations off (Settings › Accessibility › Remove animations, or the
 * developer "Animator duration scale" = off). Decorative motion such as parallax is skipped then.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Where the list item with [key] sits in the viewport: 0 when centred, about -1 at the top edge and
 * +1 at the bottom edge. Reads [LazyListState.layoutInfo], so call it only from the draw / graphics
 * layer phase: scrolling then just redraws, it never recomposes the item.
 */
fun LazyListState.viewportPositionOf(key: Any): Float {
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return 0f
    val half = (info.viewportEndOffset - info.viewportStartOffset) / 2f
    if (half <= 0f) return 0f
    val viewportCentre = info.viewportStartOffset + half
    val itemCentre = item.offset + item.size / 2f
    return ((itemCentre - viewportCentre) / half).coerceIn(-1.5f, 1.5f)
}

/** How far the first item has scrolled off the top (0 once it is gone); for header parallax. */
fun LazyListState.firstItemScrollPx(): Float =
    if (firstVisibleItemIndex == 0) firstVisibleItemScrollOffset.toFloat() else 0f
