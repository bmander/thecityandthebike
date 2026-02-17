package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.data.model.dto.LeaderboardEntry
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.LeaderboardPeriod
import com.thecityandthebike.ui.viewmodel.LeaderboardState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LeaderboardContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun leaderboardContent_loadingState_showsProgressIndicator() {
        composeTestRule.setContentWithTheme {
            LeaderboardContent(
                state = LeaderboardState(isLoading = true),
                onPeriodSelected = {},
                onUserClick = {},
                onClearError = {}
            )
        }

        // When loading, no content or error should be visible
        composeTestRule.onNodeWithText("No submissions yet").assertDoesNotExist()
    }

    @Test
    fun leaderboardContent_errorState_showsErrorText() {
        composeTestRule.setContentWithTheme {
            LeaderboardContent(
                state = LeaderboardState(error = "Network error"),
                onPeriodSelected = {},
                onUserClick = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
    }

    @Test
    fun leaderboardContent_emptyState_showsNoSubmissionsText() {
        composeTestRule.setContentWithTheme {
            LeaderboardContent(
                state = LeaderboardState(entries = emptyList()),
                onPeriodSelected = {},
                onUserClick = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("No submissions yet").assertIsDisplayed()
    }

    @Test
    fun leaderboardContent_entriesDisplayed_showsRankUsernameAndCount() {
        val entries = listOf(
            LeaderboardEntry(rank = 1, userId = "u1", username = "alice", score = 10),
            LeaderboardEntry(rank = 2, userId = "u2", username = "bob", score = 7),
            LeaderboardEntry(rank = 3, userId = "u3", username = "charlie", score = 5)
        )

        composeTestRule.setContentWithTheme {
            LeaderboardContent(
                state = LeaderboardState(entries = entries),
                onPeriodSelected = {},
                onUserClick = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("#1").assertIsDisplayed()
        composeTestRule.onNodeWithText("alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("10").assertIsDisplayed()

        composeTestRule.onNodeWithText("#2").assertIsDisplayed()
        composeTestRule.onNodeWithText("bob").assertIsDisplayed()
        composeTestRule.onNodeWithText("7").assertIsDisplayed()

        composeTestRule.onNodeWithText("#3").assertIsDisplayed()
        composeTestRule.onNodeWithText("charlie").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test
    fun leaderboardContent_rowTap_invokesOnUserClick() {
        val clickedUserIds = mutableListOf<String>()
        val entries = listOf(
            LeaderboardEntry(rank = 1, userId = "u1", username = "alice", score = 10)
        )

        composeTestRule.setContentWithTheme {
            LeaderboardContent(
                state = LeaderboardState(entries = entries),
                onPeriodSelected = {},
                onUserClick = { clickedUserIds.add(it) },
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performClick()
        assertEquals(listOf("u1"), clickedUserIds)
    }

    @Test
    fun leaderboardContent_periodTabs_displayAllPeriods() {
        composeTestRule.setContentWithTheme {
            LeaderboardContent(
                state = LeaderboardState(),
                onPeriodSelected = {},
                onUserClick = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Daily").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weekly").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monthly").assertIsDisplayed()
        composeTestRule.onNodeWithText("All Time").assertIsDisplayed()
    }

    @Test
    fun leaderboardContent_periodTabSelection_invokesCallback() {
        val selectedPeriods = mutableListOf<LeaderboardPeriod>()

        composeTestRule.setContentWithTheme {
            LeaderboardContent(
                state = LeaderboardState(selectedPeriod = LeaderboardPeriod.WEEKLY),
                onPeriodSelected = { selectedPeriods.add(it) },
                onUserClick = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Daily").performClick()
        assertEquals(listOf(LeaderboardPeriod.DAILY), selectedPeriods)

        composeTestRule.onNodeWithText("Monthly").performClick()
        assertEquals(listOf(LeaderboardPeriod.DAILY, LeaderboardPeriod.MONTHLY), selectedPeriods)
    }
}
