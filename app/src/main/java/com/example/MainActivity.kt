package com.example

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.ui.SimantoApp
import com.example.ui.intro.LaunchIntro
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var appliedDarkMode: Boolean? = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Stage 1, system splash: let the emblem animation finish (700ms, well under the 1000ms
        // SurfaceSyncGroup timeout), then hand over to the intro screen
        val shownAt = SystemClock.uptimeMillis()
        splash.setKeepOnScreenCondition { SystemClock.uptimeMillis() - shownAt < EMBLEM_ANIMATION_MILLIS }
        splash.setOnExitAnimationListener { it.remove() }

        // Stage 2 (fresh launches only): the tagline types out under the emblem while news loads behind
        val showIntro = savedInstanceState == null

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            var introVisible by rememberSaveable { mutableStateOf(showIntro) }
            Box(modifier = Modifier.fillMaxSize()) {
                SimantoApp(onDarkModeChanged = ::applySystemBarStyle)
                if (introVisible) LaunchIntro(onFinished = { introVisible = false })
            }
        }
    }

    /** Match status/navigation bar icon colours to the in-app theme toggle without re-creating window sync groups. */
    private fun applySystemBarStyle(isDarkMode: Boolean) {
        if (appliedDarkMode == isDarkMode) return
        appliedDarkMode = isDarkMode
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDarkMode
        controller.isAppearanceLightNavigationBars = !isDarkMode
    }

    private companion object {
        const val EMBLEM_ANIMATION_MILLIS = 700L // splash_emblem_animated.xml runs 0–700 ms (<1000ms SurfaceSyncGroup limit)
    }
}
