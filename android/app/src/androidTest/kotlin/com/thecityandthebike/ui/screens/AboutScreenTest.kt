package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.thecityandthebike.BuildConfig
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class AboutScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutScreen_displaysTitle() {
        composeTestRule.setContentWithTheme {
            AboutScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("About")[0].assertIsDisplayed()
    }

    @Test
    fun aboutScreen_backButtonPresent() {
        composeTestRule.setContentWithTheme {
            AboutScreen(onBack = {})
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_displaysProjectName() {
        composeTestRule.setContentWithTheme {
            AboutScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("The City and The Bike").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_displaysCreator() {
        composeTestRule.setContentWithTheme {
            AboutScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("An art project by Brandon Martin-Anderson").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_displaysEmail() {
        composeTestRule.setContentWithTheme {
            AboutScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("thecityandthebike@gmail.com").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_displaysGitHub() {
        composeTestRule.setContentWithTheme {
            AboutScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("github.com/bmander/thecityandthebike").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_displaysBuildInfo() {
        composeTestRule.setContentWithTheme {
            AboutScreen(onBack = {})
        }

        val expected = "${BuildConfig.VERSION_NAME} · ${BuildConfig.FLAVOR} · ${BuildConfig.BUILD_TYPE}"
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }
}
