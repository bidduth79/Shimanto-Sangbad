package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The bottom bar floats over the content (so the glass effect has something to blur). Lists add
 * this much empty space at their end so the last item can still scroll clear of the bar.
 */
val LocalBottomBarSpace = compositionLocalOf<Dp> { 0.dp }

/** Increases each time the open tab's bar button is tapped again: lists scroll back to the top. */
val LocalScrollToTopTick = compositionLocalOf { 0 }

/** Scrolls [state] to the top whenever the bar button of the open tab is tapped again. */
@Composable
fun ScrollToTopOnReselect(state: LazyListState) {
    val tick = LocalScrollToTopTick.current
    var handled by remember { mutableIntStateOf(tick) }
    LaunchedEffect(tick) {
        if (tick != handled) {
            handled = tick
            state.animateScrollToItem(0)
        }
    }
}

@Composable
fun ScrollToTopOnReselect(state: ScrollState) {
    val tick = LocalScrollToTopTick.current
    var handled by remember { mutableIntStateOf(tick) }
    LaunchedEffect(tick) {
        if (tick != handled) {
            handled = tick
            state.animateScrollTo(0)
        }
    }
}

/**
 * Hides the bottom bar while the user scrolls down to read and shows it again on any upward
 * scroll, like YouTube and Chrome. Small jitters are ignored.
 */
class HideOnScrollConnection(private val onVisibilityChange: (Boolean) -> Unit) : NestedScrollConnection {
    private var travelled = 0f

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        // a change of direction starts counting again
        travelled = if ((dy < 0) == (travelled < 0)) travelled + dy else dy
        when {
            travelled < -THRESHOLD_PX -> onVisibilityChange(false)
            travelled > THRESHOLD_PX -> onVisibilityChange(true)
        }
        return Offset.Zero
    }

    private companion object {
        const val THRESHOLD_PX = 24f
    }
}
