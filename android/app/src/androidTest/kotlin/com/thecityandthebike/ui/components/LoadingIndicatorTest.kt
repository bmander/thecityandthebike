package com.thecityandthebike.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class LoadingIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingIndicator_showsInitialFlavorText() {
        composeTestRule.setContentWithTheme {
            LoadingIndicator()
        }

        composeTestRule.onNodeWithText("Waking up servers…").assertIsDisplayed()
    }

    @Test
    fun loadingIndicator_cyclesFlavorTextAfterDelay() {
        composeTestRule.setContentWithTheme {
            LoadingIndicator()
        }

        composeTestRule.onNodeWithText("Waking up servers…").assertIsDisplayed()

        // Advance time by 2 seconds to trigger the first cycle
        composeTestRule.mainClock.advanceTimeBy(2100)

        composeTestRule.onNodeWithText("Sending to space…").assertIsDisplayed()

        // Advance again
        composeTestRule.mainClock.advanceTimeBy(2000)

        composeTestRule.onNodeWithText("Tagging…").assertIsDisplayed()
    }
}
