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
    fun firstPage_displaysIntroContent() {
        composeTestRule.setContent {
            OnboardingScreen(onFinished = {})
        }

        composeTestRule.onNodeWithText("What is this?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_next").assertIsDisplayed()
    }

    @Test
    fun secondPage_displaysSteps() {
        composeTestRule.setContent {
            OnboardingScreen(onFinished = {})
        }

        composeTestRule.onNodeWithTag("onboarding_next").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("How do I do it?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Find a bike rack with a QR code sticker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scan the code with the in-app scanner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Take a photo and submit it to the map").assertIsDisplayed()
    }

    @Test
    fun thirdPage_displaysPrivacyContent() {
        composeTestRule.setContent {
            OnboardingScreen(onFinished = {})
        }

        // Navigate to page 3
        composeTestRule.onNodeWithTag("onboarding_next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_next").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Privacy & Copyright").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Privacy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content License").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_get_started").assertIsDisplayed()
    }

    @Test
    fun backButton_navigatesToPreviousPage() {
        composeTestRule.setContent {
            OnboardingScreen(onFinished = {})
        }

        // Navigate to page 2
        composeTestRule.onNodeWithTag("onboarding_next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("How do I do it?").assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithTag("onboarding_back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("What is this?").assertIsDisplayed()
    }

    @Test
    fun getStarted_triggersOnFinished() {
        var finished = false

        composeTestRule.setContent {
            OnboardingScreen(onFinished = { finished = true })
        }

        // Navigate to page 3
        composeTestRule.onNodeWithTag("onboarding_next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_next").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("onboarding_get_started").performClick()

        assertTrue(finished)
    }
}
