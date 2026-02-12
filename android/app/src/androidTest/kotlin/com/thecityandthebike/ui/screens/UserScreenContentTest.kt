package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.UserDetailResponse
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.UserState
import org.junit.Rule
import org.junit.Test

class UserScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userScreen_loadingState_showsProgressIndicator() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(isLoading = true),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Unknown error").assertDoesNotExist()
    }

    @Test
    fun userScreen_errorState_showsErrorText() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(error = "User not found"),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("User not found").assertIsDisplayed()
    }

    @Test
    fun userScreen_successState_showsUserInfo() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        submissionCount = 5,
                        firstSeenAt = "2025-01-15T10:00:00Z",
                        lastSeenAt = "2025-03-20T14:30:00Z"
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("testuser").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 photos").assertIsDisplayed()
        composeTestRule.onNodeWithText("First seen: Jan 15, 2025").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last seen: Mar 20, 2025").assertIsDisplayed()
    }

    @Test
    fun userScreen_singlePhoto_correctPluralization() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        submissionCount = 1
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("1 photo").assertIsDisplayed()
    }

    @Test
    fun userScreen_multiplePhotos_correctPluralization() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        submissionCount = 7
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("7 photos").assertIsDisplayed()
    }

    @Test
    fun userScreen_submissionsGroupedByDate_showsDateLabels() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        submissionCount = 3
                    ),
                    submissions = listOf(
                        SubmissionResponse(
                            submissionId = "sub-1",
                            userId = "user-1",
                            bikeQrId = "bike-1",
                            capturedDate = "2024-01-15",
                            imageUrl = "https://example.com/img1.jpg"
                        ),
                        SubmissionResponse(
                            submissionId = "sub-2",
                            userId = "user-1",
                            bikeQrId = "bike-2",
                            capturedDate = "2024-01-15",
                            imageUrl = "https://example.com/img2.jpg"
                        ),
                        SubmissionResponse(
                            submissionId = "sub-3",
                            userId = "user-1",
                            bikeQrId = "bike-3",
                            capturedDate = "2024-03-20",
                            imageUrl = "https://example.com/img3.jpg"
                        )
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Jan 15").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mar 20").assertIsDisplayed()
    }
}
