package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysIntroContentAndSteps() {
        composeTestRule.setContent {
            OnboardingScreen(onFinished = {})
        }

        composeTestRule.onNodeWithText("What is this?").assertIsDisplayed()
        composeTestRule.onNodeWithText("How do I do it?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scan the bike QR code").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take a picture of the rear wheel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try to catch them all").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_get_started").assertIsDisplayed()
    }

    @Test
    fun getStarted_triggersOnFinished() {
        var finished = false

        composeTestRule.setContent {
            OnboardingScreen(onFinished = { finished = true })
        }

        composeTestRule.onNodeWithTag("onboarding_get_started").performClick()

        assertTrue(finished)
    }
}
