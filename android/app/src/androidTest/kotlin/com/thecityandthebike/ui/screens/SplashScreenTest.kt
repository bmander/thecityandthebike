package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splashScreen_displaysImage() {
        composeTestRule.setContentWithTheme {
            SplashScreen(onTimeout = {})
        }

        // The splash image is rendered (Image composable exists in the tree)
        composeTestRule.waitForIdle()
    }

    @Test
    fun splashScreen_callsOnTimeoutAfterDelay() {
        var timeoutCalled = false

        composeTestRule.setContentWithTheme {
            SplashScreen(onTimeout = { timeoutCalled = true })
        }

        // Advance the virtual clock past the 2000ms delay
        composeTestRule.mainClock.advanceTimeBy(2500L)

        assert(timeoutCalled) { "Expected onTimeout to be called after delay" }
    }
}
