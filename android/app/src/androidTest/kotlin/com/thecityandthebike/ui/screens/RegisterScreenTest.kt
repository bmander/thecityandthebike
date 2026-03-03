package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.AuthState
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun renderRegisterScreen(state: AuthState = AuthState()) {
        composeTestRule.setContentWithTheme {
            RegisterScreen(
                state = state,
                onRegister = { _, _ -> },
                onNavigateBack = {},
                onClearError = {},
                onClearRegistrationSuccess = {}
            )
        }
    }

    @Test
    fun registerScreen_allFormFieldsDisplayed() {
        renderRegisterScreen()

        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Password").assertIsDisplayed()
        composeTestRule.onNode(hasText("Create Account") and hasClickAction())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_createAccountButtonDisabledWhenFormEmpty() {
        renderRegisterScreen()

        composeTestRule.onNode(hasText("Create Account") and hasClickAction())
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun registerScreen_createAccountButtonEnabledWhenFormValid() {
        renderRegisterScreen()

        composeTestRule.onNodeWithText("Username").performTextInput("testuser")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("password123")

        // Check both checkboxes
        composeTestRule.onNodeWithText("I've read this and I understand")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("I agree to license my photos under CC BY-NC 4.0")
            .performScrollTo()
            .performClick()

        composeTestRule.onNode(hasText("Create Account") and hasClickAction())
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    fun registerScreen_errorTextDisplayed() {
        renderRegisterScreen(state = AuthState(error = "Username already taken"))

        composeTestRule.onNodeWithText("Username already taken")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_loadingStateDisablesFields() {
        renderRegisterScreen(state = AuthState(isLoading = true))

        composeTestRule.onNodeWithText("Username").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Password").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Confirm Password").assertIsNotEnabled()
    }

    @Test
    fun registerScreen_shortPasswordKeepsButtonDisabled() {
        renderRegisterScreen()

        composeTestRule.onNodeWithText("Username").performTextInput("testuser")
        composeTestRule.onNodeWithText("Password").performTextInput("short7x")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("short7x")

        composeTestRule.onNodeWithText("I've read this and I understand")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("I agree to license my photos under CC BY-NC 4.0")
            .performScrollTo()
            .performClick()

        composeTestRule.onNode(hasText("Create Account") and hasClickAction())
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun registerScreen_shortUsernameKeepsButtonDisabled() {
        renderRegisterScreen()

        composeTestRule.onNodeWithText("Username").performTextInput("ab")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("password123")

        composeTestRule.onNodeWithText("I've read this and I understand")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("I agree to license my photos under CC BY-NC 4.0")
            .performScrollTo()
            .performClick()

        composeTestRule.onNode(hasText("Create Account") and hasClickAction())
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun registerScreen_shortUsernameShowsValidationError() {
        renderRegisterScreen()

        composeTestRule.onNodeWithText("Username").performTextInput("ab")

        composeTestRule.onNodeWithText("Username must be at least 3 characters")
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_invalidCharactersShowsValidationError() {
        renderRegisterScreen()

        composeTestRule.onNodeWithText("Username").performTextInput("test user")

        composeTestRule.onNodeWithText("Only letters, numbers, and underscores allowed")
            .assertIsDisplayed()
    }

    @Test
    fun registerScreen_usernameWithSpecialCharsKeepsButtonDisabled() {
        renderRegisterScreen()

        composeTestRule.onNodeWithText("Username").performTextInput("user@name")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("password123")

        composeTestRule.onNodeWithText("I've read this and I understand")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("I agree to license my photos under CC BY-NC 4.0")
            .performScrollTo()
            .performClick()

        composeTestRule.onNode(hasText("Create Account") and hasClickAction())
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun registerScreen_passwordMismatchShowsError() {
        renderRegisterScreen()

        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("different")

        composeTestRule.onNodeWithText("Passwords don't match").assertIsDisplayed()
    }
}
