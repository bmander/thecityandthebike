package com.thecityandthebike

import android.content.Context
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File

@HiltAndroidTest
class ScreenshotCaptureTest {

    private val hiltRule = HiltAndroidRule(this)

    private val skipOnboardingRule = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("completed", true).apply()
        }

        override fun after() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(skipOnboardingRule)
        .around(composeTestRule)

    private val device: UiDevice by lazy {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    private fun takeScreenshot(name: String) {
        // Use /sdcard/screenshots/ which is accessible via adb pull on CI emulators.
        // On devices with scoped storage this may fail silently — that's OK,
        // screenshots are primarily for CI.
        val dir = File("/sdcard/screenshots")
        dir.mkdirs()
        device.takeScreenshot(File(dir, name))
    }

    @Test
    fun captureAllScreenshots() {
        // Step 1: Wait for splash to pass and main feed to load
        composeTestRule.mainClock.advanceTimeBy(3000)
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodesWithContentDescription("Captured image")
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Let images load
        Thread.sleep(3000)
        takeScreenshot("01_main_screen.png")

        // Step 2: Navigate to Leaderboard tab
        composeTestRule.onNodeWithText("Leaderboard").performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithText("#1")
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(2000)
        takeScreenshot("05_leaderboard.png")

        // Step 3: Find "bmander" on leaderboard and navigate to user screen
        // Try Weekly tab first, then All Time if not found
        val bmanderOnWeekly = composeTestRule.onAllNodesWithText("bmander")
            .fetchSemanticsNodes().isNotEmpty()
        if (!bmanderOnWeekly) {
            composeTestRule.onNodeWithText("All Time").performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithText("bmander")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }
        composeTestRule.onAllNodesWithText("bmander")[0].performClick()

        // Wait for user screen to load with images
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithContentDescription("Captured image")
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(3000)
        takeScreenshot("02_user_screen.png")

        // Step 4: Click first photo in bmander's calendar to go to ImageDetail
        composeTestRule.onAllNodesWithContentDescription("Captured image")[0].performClick()
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithContentDescription("Home")
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(3000)

        // Step 5: Tap tagOutlineOverlay center to select a tag (if overlay exists)
        val tagOverlayExists = composeTestRule.onAllNodes(hasTestTag("tagOutlineOverlay"))
            .fetchSemanticsNodes().isNotEmpty()

        if (tagOverlayExists) {
            composeTestRule.onNodeWithTag("tagOutlineOverlay")
                .performTouchInput { click(center) }
            composeTestRule.waitForIdle()
            Thread.sleep(1000)
        }
        takeScreenshot("03_photo_detail.png")

        // Step 6: If viewTagButton exists, click to navigate to TagDetail
        val viewTagButtonExists = composeTestRule.onAllNodes(hasTestTag("viewTagButton"))
            .fetchSemanticsNodes().isNotEmpty()

        if (viewTagButtonExists) {
            composeTestRule.onNodeWithTag("viewTagButton").performClick()
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                composeTestRule.onAllNodesWithContentDescription("Tag image")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            Thread.sleep(3000)
            takeScreenshot("04_tag_screen.png")

            // Navigate back to ImageDetail
            device.pressBack()
            composeTestRule.waitUntil(timeoutMillis = 10_000) {
                composeTestRule.onAllNodesWithContentDescription("Home")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            Thread.sleep(1000)
        }

        // Step 7: Scroll to Bike InfoRow and click to go to BikeScreen
        composeTestRule.onNode(
            hasContentDescription("Bike").and(hasClickAction())
        ).performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            composeTestRule.onAllNodesWithContentDescription("Back")
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(3000)
        takeScreenshot("06_bike_screen.png")
    }
}
