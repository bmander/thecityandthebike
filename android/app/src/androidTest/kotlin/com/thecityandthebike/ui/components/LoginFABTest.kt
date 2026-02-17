package com.thecityandthebike.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginFABTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginFAB_isDisplayed() {
        composeTestRule.setContentWithTheme {
            LoginFAB(onClick = {})
        }

        composeTestRule
            .onNodeWithText("Sign in")
            .assertIsDisplayed()
    }

    @Test
    fun loginFAB_clickCallsOnClick() {
        var clicked = false

        composeTestRule.setContentWithTheme {
            LoginFAB(onClick = { clicked = true })
        }

        composeTestRule
            .onNodeWithText("Sign in")
            .performClick()

        assertTrue("onClick should be called when FAB is clicked", clicked)
    }

    @Test
    fun loginFAB_displaysSignInText() {
        composeTestRule.setContentWithTheme {
            LoginFAB(onClick = {})
        }

        composeTestRule
            .onNodeWithText("Sign in")
            .assertExists()
    }
}
