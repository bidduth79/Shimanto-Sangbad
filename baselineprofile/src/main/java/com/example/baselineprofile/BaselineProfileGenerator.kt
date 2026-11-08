package com.example.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The journey a reader takes: open the app, wait for the intro and the news, scroll the list,
 * switch to videos and back. Everything it touches is compiled ahead of time on install.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()

        // The launch intro types the tagline for a few seconds, then the news list appears
        device.wait(Until.hasObject(By.scrollable(true)), 15_000)

        device.findObject(By.scrollable(true))?.let { list ->
            list.setGestureMargin(device.displayWidth / 5)
            repeat(2) {
                list.fling(Direction.DOWN)
                device.waitForIdle()
            }
            list.fling(Direction.UP)
            device.waitForIdle()
        }

        device.findObject(By.desc(VIDEO_TAB))?.click()
        device.wait(Until.hasObject(By.scrollable(true)), 10_000)
        device.waitForIdle()

        device.findObject(By.desc(HOME_TAB))?.click()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE = "com.aistudio.simantosongbad.news"
        const val VIDEO_TAB = "ভিডিও"
        const val HOME_TAB = "হোম"
    }
}
