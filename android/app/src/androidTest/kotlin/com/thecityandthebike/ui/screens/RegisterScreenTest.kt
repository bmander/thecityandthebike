package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.AuthState
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun registerScreen_allFormFieldsDisplayed() {
        composeTestRule.setContentWithTheme {
            RegisterScreen(
                state = AuthState(),
                onRegister = { _, _, _ -> },
                onNavigateBack = {},
                onClearError = {},
                onClearRegistrationSuccess = {}
            )
        }

        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
    }

    @Test
    fun registerScreen_createAccountButtonDisabledWhenFormEmpty() {
        composeTestRule.setContentWithTheme {
            RegisterScreen(
                state = AuthState(),
                onRegister = { _, _, _ -> },
                onNavigateBack = {},
                onClearError = {},
                onClearRegistrationSuccess = {}
            )
        }

        composeTestRule.onNodeWithText("Create Account").assertIsNotEnabled()
    }

    @Test
    fun registerScreen_createAccountButtonEnabledWhenFormValid() {
        composeTestRule.setContentWithTheme {
            RegisterScreen(
                state = AuthState(),
                onRegister = { _, _, _ -> },
                onNavigateBack = {},
                onClearError = {},
                onClearRegistrationSuccess = {}
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("testuser")
        composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("password123")

        // Check both checkboxes
        composeTestRule.onNodeWithText("I've read this and I understand").performClick()
        composeTestRule.onNodeWithText("I agree to license my photos under CC BY-NC 4.0").performClick()

        composeTestRule.onNodeWithText("Create Account").assertIsEnabled()
    }

    @Test
    fun registerScreen_errorTextDisplayed() {
        composeTestRule.setContentWithTheme {
            RegisterScreen(
                state = AuthState(error = "Username already taken"),
                onRegister = { _, _, _ -> },
                onNavigateBack = {},
                onClearError = {},
                onClearRegistrationSuccess = {}
            )
        }

        composeTestRule.onNodeWithText("Username already taken").assertIsDisplayed()
    }

    @Test
    fun registerScreen_loadingStateDisablesFields() {
        composeTestRule.setContentWithTheme {
            RegisterScreen(
                state = AuthState(isLoading = true),
                onRegister = { _, _, _ -> },
                onNavigateBack = {},
                onClearError = {},
                onClearRegistrationSuccess = {}
            )
        }

        composeTestRule.onNodeWithText("Username").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Email").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Password").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Confirm Password").assertIsNotEnabled()
    }

    @Test
    fun registerScreen_passwordMismatchShowsError() {
        composeTestRule.setContentWithTheme {
            RegisterScreen(
                state = AuthState(),
                onRegister = { _, _, _ -> },
                onNavigateBack = {},
                onClearError = {},
                onClearRegistrationSuccess = {}
            )
        }

        composeTestRule.onNodeWithText("Password").performTextInput("password123")
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("different")

        composeTestRule.onNodeWithText("Passwords don't match").assertIsDisplayed()
    }
}
