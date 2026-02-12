package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class PrivacyCopyrightScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun privacyCopyrightScreen_displaysTitle() {
        composeTestRule.setContentWithTheme {
            PrivacyCopyrightScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Privacy & Copyright").assertIsDisplayed()
    }

    @Test
    fun privacyCopyrightScreen_backButtonPresent() {
        composeTestRule.setContentWithTheme {
            PrivacyCopyrightScreen(onBack = {})
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun privacyCopyrightScreen_displaysPrivacySection() {
        composeTestRule.setContentWithTheme {
            PrivacyCopyrightScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Privacy").assertIsDisplayed()
    }

    @Test
    fun privacyCopyrightScreen_displaysCopyrightSection() {
        composeTestRule.setContentWithTheme {
            PrivacyCopyrightScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Copyright").assertIsDisplayed()
    }
}
