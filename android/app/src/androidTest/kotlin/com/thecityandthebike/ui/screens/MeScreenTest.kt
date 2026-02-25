package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.UserState
import org.junit.Rule
import org.junit.Test

class MeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun meScreen_displaysTitle() {
        composeTestRule.setContentWithTheme {
            MeScreen(
                state = UserState(),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onAllNodesWithText("Me")[0].assertIsDisplayed()
    }

    @Test
    fun meScreen_backButtonPresent() {
        composeTestRule.setContentWithTheme {
            MeScreen(
                state = UserState(),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
}
