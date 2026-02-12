package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.data.model.dto.BikeListItem
import com.thecityandthebike.data.model.dto.UserSummary
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.BikesListState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BikesContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bikesContent_loadingState_showsProgressIndicator() {
        composeTestRule.setContentWithTheme {
            BikesContent(
                state = BikesListState(isLoading = true),
                onBikeClick = {},
                onLoadMore = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("No bikes yet").assertDoesNotExist()
    }

    @Test
    fun bikesContent_errorState_showsErrorText() {
        composeTestRule.setContentWithTheme {
            BikesContent(
                state = BikesListState(error = "Network error"),
                onBikeClick = {},
                onLoadMore = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
    }

    @Test
    fun bikesContent_emptyState_showsNoBikesText() {
        composeTestRule.setContentWithTheme {
            BikesContent(
                state = BikesListState(bikes = emptyList()),
                onBikeClick = {},
                onLoadMore = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("No bikes yet").assertIsDisplayed()
    }

    @Test
    fun bikesContent_bikesDisplayed_showsBikeIdAndCount() {
        val bikes = listOf(
            BikeListItem(
                bikeQrId = "BIKE-001",
                provider = "citibike",
                submissionCount = 5,
                owner = UserSummary(name = "alice", id = "u1")
            ),
            BikeListItem(
                bikeQrId = "BIKE-002",
                provider = null,
                submissionCount = 3,
                owner = null
            )
        )

        composeTestRule.setContentWithTheme {
            BikesContent(
                state = BikesListState(bikes = bikes),
                onBikeClick = {},
                onLoadMore = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("BIKE-001").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("alice").assertIsDisplayed()

        composeTestRule.onNodeWithText("BIKE-002").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun bikesContent_rowTap_invokesOnBikeClick() {
        val clickedBikeIds = mutableListOf<String>()
        val bikes = listOf(
            BikeListItem(
                bikeQrId = "BIKE-001",
                provider = null,
                submissionCount = 5,
                owner = null
            )
        )

        composeTestRule.setContentWithTheme {
            BikesContent(
                state = BikesListState(bikes = bikes),
                onBikeClick = { clickedBikeIds.add(it) },
                onLoadMore = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("BIKE-001").performClick()
        assertEquals(listOf("BIKE-001"), clickedBikeIds)
    }

    @Test
    fun bikesContent_ownerDisplayed_showsOwnerName() {
        val bikes = listOf(
            BikeListItem(
                bikeQrId = "BIKE-001",
                provider = null,
                submissionCount = 10,
                owner = UserSummary(name = "bob", id = "u2")
            )
        )

        composeTestRule.setContentWithTheme {
            BikesContent(
                state = BikesListState(bikes = bikes),
                onBikeClick = {},
                onLoadMore = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("bob").assertIsDisplayed()
    }

    @Test
    fun bikesContent_providerDisplayed_showsProviderBadge() {
        val bikes = listOf(
            BikeListItem(
                bikeQrId = "BIKE-001",
                provider = "citibike",
                submissionCount = 1,
                owner = null
            )
        )

        composeTestRule.setContentWithTheme {
            BikesContent(
                state = BikesListState(bikes = bikes),
                onBikeClick = {},
                onLoadMore = {},
                onClearError = {}
            )
        }

        composeTestRule.onNodeWithText("CITIBIKE").assertIsDisplayed()
    }
}
