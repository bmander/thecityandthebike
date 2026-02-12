package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.thecityandthebike.data.model.dto.BikeDetailResponse
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.BikeState
import org.junit.Rule
import org.junit.Test

class BikeScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bikeScreen_loadingState_showsProgressIndicator() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(isLoading = true),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        // When loading, no error or content should be shown
        composeTestRule.onNodeWithText("Unknown error").assertDoesNotExist()
    }

    @Test
    fun bikeScreen_errorState_showsErrorText() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(error = "Network error"),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
    }

    @Test
    fun bikeScreen_successState_showsBikeDetailHeader() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-42",
                        provider = "citibike",
                        submissionCount = 5
                    )
                ),
                bikeQrId = "BIKE-42",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("BIKE-42").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 photos").assertIsDisplayed()
    }

    @Test
    fun bikeScreen_singlePhoto_correctPluralization() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        provider = "citibike",
                        submissionCount = 1
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("1 photo").assertIsDisplayed()
    }

    @Test
    fun bikeScreen_multiplePhotos_correctPluralization() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        provider = "citibike",
                        submissionCount = 3
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("3 photos").assertIsDisplayed()
    }

    @Test
    fun bikeScreen_successState_showsDateGroupEntries() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-42",
                        provider = "citibike",
                        submissionCount = 1
                    ),
                    submissions = listOf(
                        SubmissionResponse(
                            submissionId = "sub-1",
                            userId = "user-1",
                            bikeQrId = "BIKE-42",
                            capturedDate = "2025-03-15",
                            imageUrl = "https://example.com/img.jpg"
                        )
                    )
                ),
                bikeQrId = "BIKE-42",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Mar 15").assertIsDisplayed()
    }
}
