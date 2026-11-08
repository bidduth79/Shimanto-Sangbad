package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay

/**
 * The current time, updated once a minute. Cards read it for "5 minutes ago" labels, so the labels
 * keep counting while the app stays open instead of freezing at the time the card was first drawn.
 */
val LocalNow = compositionLocalOf { System.currentTimeMillis() }

/** A clock that ticks on each full minute. */
@Composable
fun rememberMinuteClock(): Long {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            val current = System.currentTimeMillis()
            delay(60_000 - current % 60_000 + 50)
            value = System.currentTimeMillis()
        }
    }
    return now
}
