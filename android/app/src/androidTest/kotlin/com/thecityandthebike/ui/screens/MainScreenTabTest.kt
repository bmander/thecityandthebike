package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.BikesListState
import com.thecityandthebike.ui.viewmodel.LeaderboardState
import com.thecityandthebike.ui.viewmodel.MainState
import org.junit.Rule
import org.junit.Test

class MainScreenTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_showsThreeTabs() {
        composeTestRule.setContentWithTheme {
            MainScreen(
                mainState = MainState(),
                leaderboardState = LeaderboardState(),
                bikesListState = BikesListState(),
                isLoggedIn = false,
                onLogout = {},
                onLoginClick = {},
                onScanQrCode = {}
            )
        }

        composeTestRule.onNodeWithText("Feed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Leaderboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bikes").assertIsDisplayed()
    }

    @Test
    fun mainScreen_loggedIn_showsMeInMenu() {
        composeTestRule.setContentWithTheme {
            MainScreen(
                mainState = MainState(),
                leaderboardState = LeaderboardState(),
                bikesListState = BikesListState(),
                isLoggedIn = true,
                onLogout = {},
                onLoginClick = {},
                onScanQrCode = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Me").assertIsDisplayed()
    }

    @Test
    fun mainScreen_loggedOut_doesNotShowMeInMenu() {
        composeTestRule.setContentWithTheme {
            MainScreen(
                mainState = MainState(),
                leaderboardState = LeaderboardState(),
                bikesListState = BikesListState(),
                isLoggedIn = false,
                onLogout = {},
                onLoginClick = {},
                onScanQrCode = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithText("Me").assertDoesNotExist()
    }
}
