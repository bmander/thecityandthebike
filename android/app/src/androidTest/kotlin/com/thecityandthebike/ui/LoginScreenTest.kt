package com.thecityandthebike.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.thecityandthebike.ui.screens.LoginScreen
import com.thecityandthebike.ui.viewmodel.AuthState
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysAllElements() {
        composeTestRule.setContent {
            LoginScreen(
                state = AuthState(),
                onLogin = { _, _ -> },
                onNavigateToRegister = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("The City and the Bike").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in to continue").assertIsDisplayed()
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeTestRule.onNodeWithText("Don't have an account? Register").assertIsDisplayed()
    }

    @Test
    fun loginScreen_signInButtonDisabledWhenFieldsEmpty() {
        composeTestRule.setContent {
            LoginScreen(
                state = AuthState(),
                onLogin = { _, _ -> },
                onNavigateToRegister = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Sign In").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_signInButtonEnabledWhenFieldsFilled() {
        composeTestRule.setContent {
            LoginScreen(
                state = AuthState(),
                onLogin = { _, _ -> },
                onNavigateToRegister = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("testuser")
        composeTestRule.onNodeWithText("Password").performTextInput("testpass")
        composeTestRule.onNodeWithText("Sign In").assertIsEnabled()
    }

    @Test
    fun loginScreen_displaysErrorWhenPresent() {
        composeTestRule.setContent {
            LoginScreen(
                state = AuthState(error = "Invalid credentials"),
                onLogin = { _, _ -> },
                onNavigateToRegister = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsLoadingState() {
        composeTestRule.setContent {
            LoginScreen(
                state = AuthState(isLoading = true),
                onLogin = { _, _ -> },
                onNavigateToRegister = {},
                onClearError = {}
            )
        }

        // Fields should be disabled during loading
        composeTestRule.onNodeWithText("Username").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Password").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_callsOnLoginWhenButtonClicked() {
        var loginCalled = false
        var capturedUsername = ""
        var capturedPassword = ""

        composeTestRule.setContent {
            LoginScreen(
                state = AuthState(),
                onLogin = { username, password ->
                    loginCalled = true
                    capturedUsername = username
                    capturedPassword = password
                },
                onNavigateToRegister = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("testuser")
        composeTestRule.onNodeWithText("Password").performTextInput("testpass")
        composeTestRule.onNodeWithText("Sign In").performClick()

        assert(loginCalled)
        assert(capturedUsername == "testuser")
        assert(capturedPassword == "testpass")
    }

    @Test
    fun loginScreen_callsOnNavigateToRegisterWhenLinkClicked() {
        var navigateCalled = false

        composeTestRule.setContent {
            LoginScreen(
                state = AuthState(),
                onLogin = { _, _ -> },
                onNavigateToRegister = { navigateCalled = true },
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Don't have an account? Register").performClick()

        assert(navigateCalled)
    }
}
