package com.thecityandthebike.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MenuButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun menuButton_isDisplayed() {
        composeTestRule.setContentWithTheme {
            MenuButton(onClick = {})
        }

        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertIsDisplayed()
    }

    @Test
    fun menuButton_clickCallsOnClick() {
        var clicked = false

        composeTestRule.setContentWithTheme {
            MenuButton(onClick = { clicked = true })
        }

        composeTestRule
            .onNodeWithContentDescription("Menu")
            .performClick()

        assertTrue("onClick should be called when MenuButton is clicked", clicked)
    }
}
