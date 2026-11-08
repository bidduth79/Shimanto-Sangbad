package com.example.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.rememberReducedMotion
import kotlinx.coroutines.delay

/** The system splash draws the emblem 180dp wide, centred; this screen continues it pixel for pixel. */
private val EmblemSize = 180.dp

private val TaglineColor = Color(0xFFF1F5F9)
private val CaretColor = Color(0xFFE11D48)
private val TaglineStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)

/**
 * Second half of the launch: the emblem stays exactly where the system splash left it while the
 * tagline types out underneath, then the whole screen fades to reveal the app (which has been
 * loading news behind it all along).
 */
@Composable
fun LaunchIntro(onFinished: () -> Unit) {
    val tagline = stringResource(R.string.splash_tagline)
    val units = remember(tagline) { typingUnits(tagline) }
    val reducedMotion = rememberReducedMotion()

    var shownUnits by remember { mutableIntStateOf(if (reducedMotion) units.size else 0) }
    var caretVisible by remember { mutableStateOf(true) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay(START_DELAY_MILLIS)
            while (shownUnits < units.size) {
                // Spaces go by quickly; letters take a typist's beat
                delay(if (units[shownUnits].isBlank()) SPACE_MILLIS else UNIT_MILLIS)
                shownUnits++
            }
            repeat(CARET_BLINKS * 2) {
                delay(CARET_BLINK_MILLIS)
                caretVisible = !caretVisible
            }
        } else {
            delay(REDUCED_MOTION_HOLD_MILLIS)
        }
        screenAlpha.animateTo(0f, tween(FADE_OUT_MILLIS, easing = LinearEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(colorResource(R.color.splash_background))
            // Swallow touches so nothing in the app underneath can be tapped while the intro plays
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.bgb_emblem_large),
            contentDescription = null,
            modifier = Modifier.size(EmblemSize)
        )
        // The full line (plus caret) is laid out invisibly so the visible text grows from a fixed
        // left edge instead of re-centring, and jumping, with every new letter
        Box(modifier = Modifier.offset(y = EmblemSize / 2 + 44.dp)) {
            Text(text = "$tagline|", style = TaglineStyle, color = Color.Transparent)
            Text(
                text = buildAnnotatedString {
                    append(units.take(shownUnits).joinToString(""))
                    withStyle(SpanStyle(color = if (caretVisible && !reducedMotion) CaretColor else Color.Transparent, fontWeight = FontWeight.Light)) {
                        append("|")
                    }
                },
                style = TaglineStyle,
                color = TaglineColor
            )
        }
    }
}

private const val START_DELAY_MILLIS = 150L
private const val UNIT_MILLIS = 95L
private const val SPACE_MILLIS = 40L
private const val CARET_BLINKS = 2
private const val CARET_BLINK_MILLIS = 200L
private const val FADE_OUT_MILLIS = 350
private const val REDUCED_MOTION_HOLD_MILLIS = 700L
