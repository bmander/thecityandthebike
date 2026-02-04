package com.thecityandthebike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_launchesSuccessfully() {
        // Activity should launch without crashing
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainActivity_componentsVisibleAfterLaunch() {
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Add image")
            .assertIsDisplayed()
    }
}
