package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.BikesListState
import com.thecityandthebike.ui.viewmodel.BikesListViewModel
import com.thecityandthebike.ui.viewmodel.LeaderboardState
import com.thecityandthebike.ui.viewmodel.LeaderboardViewModel
import com.thecityandthebike.ui.viewmodel.MainState
import com.thecityandthebike.ui.viewmodel.MainViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class MainScreenTabTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockViewModels(): Triple<MainViewModel, LeaderboardViewModel, BikesListViewModel> {
        val mainViewModel = mockk<MainViewModel>(relaxed = true)
        every { mainViewModel.state } returns MutableStateFlow(MainState())

        val leaderboardViewModel = mockk<LeaderboardViewModel>(relaxed = true)
        every { leaderboardViewModel.state } returns MutableStateFlow(LeaderboardState())

        val bikesListViewModel = mockk<BikesListViewModel>(relaxed = true)
        every { bikesListViewModel.state } returns MutableStateFlow(BikesListState())

        return Triple(mainViewModel, leaderboardViewModel, bikesListViewModel)
    }

    @Test
    fun mainScreen_showsThreeTabs() {
        val (mainVM, leaderboardVM, bikesListVM) = createMockViewModels()

        composeTestRule.setContentWithTheme {
            MainScreen(
                viewModel = mainVM,
                leaderboardViewModel = leaderboardVM,
                bikesListViewModel = bikesListVM,
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
        val (mainVM, leaderboardVM, bikesListVM) = createMockViewModels()

        composeTestRule.setContentWithTheme {
            MainScreen(
                viewModel = mainVM,
                leaderboardViewModel = leaderboardVM,
                bikesListViewModel = bikesListVM,
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
        val (mainVM, leaderboardVM, bikesListVM) = createMockViewModels()

        composeTestRule.setContentWithTheme {
            MainScreen(
                viewModel = mainVM,
                leaderboardViewModel = leaderboardVM,
                bikesListViewModel = bikesListVM,
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
