package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.data.model.dto.BikeDetailResponse
import com.thecityandthebike.data.model.dto.BikeOwner
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UserSummary
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.BikeState
import org.junit.Assert.assertEquals
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

    @Test
    fun bikeScreen_showsOwnerWithPhotoCount() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        submissionCount = 5,
                        owners = listOf(
                            BikeOwner(
                                user = UserSummary(name = "alice", id = "user-1"),
                                submissionCount = 3
                            )
                        )
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("alice (3 photos)").assertIsDisplayed()
    }

    @Test
    fun bikeScreen_ownerSinglePhoto_correctPluralization() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        submissionCount = 1,
                        owners = listOf(
                            BikeOwner(
                                user = UserSummary(name = "bob", id = "user-2"),
                                submissionCount = 1
                            )
                        )
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("bob (1 photo)").assertIsDisplayed()
    }

    @Test
    fun bikeScreen_showsMultipleOwners() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        submissionCount = 7,
                        owners = listOf(
                            BikeOwner(
                                user = UserSummary(name = "alice", id = "user-1"),
                                submissionCount = 4
                            ),
                            BikeOwner(
                                user = UserSummary(name = "bob", id = "user-2"),
                                submissionCount = 3
                            )
                        )
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("alice (4 photos)").assertIsDisplayed()
        composeTestRule.onNodeWithText("bob (3 photos)").assertIsDisplayed()
    }

    @Test
    fun bikeScreen_showsFirstCapturedByWithDate() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        submissionCount = 2,
                        firstSeenAt = "2024-03-15T10:00:00Z",
                        firstCapturedBy = UserSummary(name = "alice", id = "user-1")
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("First captured by alice on Mar 15, 2024")
            .assertIsDisplayed()
    }

    @Test
    fun bikeScreen_firstCapturedByWithoutDate() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        submissionCount = 1,
                        firstCapturedBy = UserSummary(name = "bob", id = "user-2")
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("First captured by bob").assertIsDisplayed()
    }

    @Test
    fun bikeScreen_noOwnersOrCapturedBy_noInfoRows() {
        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        submissionCount = 0
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("First captured by", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun bikeScreen_ownerClick_callsOnUserClickWithCorrectId() {
        var clickedUserId = ""

        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        submissionCount = 3,
                        owners = listOf(
                            BikeOwner(
                                user = UserSummary(name = "alice", id = "user-1"),
                                submissionCount = 3
                            )
                        )
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {},
                onUserClick = { clickedUserId = it }
            )
        }

        composeTestRule.onNodeWithText("alice (3 photos)").performClick()

        assertEquals("user-1", clickedUserId)
    }

    @Test
    fun bikeScreen_firstCapturedByClick_callsOnUserClickWithCorrectId() {
        var clickedUserId = ""

        composeTestRule.setContentWithTheme {
            BikeScreenContent(
                state = BikeState(
                    bikeDetail = BikeDetailResponse(
                        bikeQrId = "BIKE-1",
                        submissionCount = 1,
                        firstCapturedBy = UserSummary(name = "bob", id = "user-2")
                    )
                ),
                bikeQrId = "BIKE-1",
                onBack = {},
                onImageClick = {},
                onUserClick = { clickedUserId = it }
            )
        }

        composeTestRule.onNodeWithText("First captured by bob").performClick()

        assertEquals("user-2", clickedUserId)
    }
}
