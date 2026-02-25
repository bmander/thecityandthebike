package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                        lastSeenAt = "2025-03-20T14:30:00Z",
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
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
                        submissionCount = 1,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
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
                        submissionCount = 7,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
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
                        submissionCount = 3,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
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

    // --- Admin badge ---

    @Test
    fun userScreen_adminUser_showsAdminBadge() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    userDetail = UserDetailResponse(
                        userId = "admin-1",
                        username = "adminuser",
                        isAdmin = true,
                        submissionCount = 3,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Admin").assertIsDisplayed()
    }

    @Test
    fun userScreen_nonAdminUser_noAdminBadge() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        isAdmin = false,
                        submissionCount = 3,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Admin").assertDoesNotExist()
    }

    // --- Ban button visibility ---

    @Test
    fun userScreen_adminViewingNonBannedUser_showsBanButton() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    currentUserIsAdmin = true,
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        isAdmin = false,
                        isBanned = false,
                        submissionCount = 5,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Ban User").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unban User").assertDoesNotExist()
    }

    @Test
    fun userScreen_adminViewingBannedUser_showsUnbanButton() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    currentUserIsAdmin = true,
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        isAdmin = false,
                        isBanned = true,
                        submissionCount = 5,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Unban User").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ban User").assertDoesNotExist()
    }

    @Test
    fun userScreen_nonAdminViewing_noBanButtons() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    currentUserIsAdmin = false,
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        isBanned = false,
                        submissionCount = 5,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Ban User").assertDoesNotExist()
        composeTestRule.onNodeWithText("Unban User").assertDoesNotExist()
    }

    @Test
    fun userScreen_adminViewingAdmin_noBanButtons() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    currentUserIsAdmin = true,
                    userDetail = UserDetailResponse(
                        userId = "admin-2",
                        username = "otheradmin",
                        isAdmin = true,
                        isBanned = false,
                        submissionCount = 5,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Ban User").assertDoesNotExist()
        composeTestRule.onNodeWithText("Unban User").assertDoesNotExist()
    }

    // --- Ban confirmation dialog ---

    @Test
    fun userScreen_banButtonClick_showsConfirmationDialog() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    currentUserIsAdmin = true,
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "baduser",
                        isAdmin = false,
                        isBanned = false,
                        submissionCount = 0,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Ban User").performClick()

        composeTestRule.onNodeWithText("Are you sure you want to ban baduser? They will no longer be able to post content or log in.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Ban").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    fun userScreen_unbanButtonClick_showsConfirmationDialog() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    currentUserIsAdmin = true,
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "baduser",
                        isAdmin = false,
                        isBanned = true,
                        submissionCount = 0,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Unban User").performClick()

        composeTestRule.onNodeWithText("Are you sure you want to unban baduser? They will be able to post content and log in again.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Unban").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    // --- Ban button disabled while banning ---

    @Test
    fun userScreen_isBanningTrue_banButtonDisabled() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    currentUserIsAdmin = true,
                    isBanning = true,
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        isAdmin = false,
                        isBanned = false,
                        submissionCount = 0,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Ban User").assertDoesNotExist()
    }

    @Test
    fun userScreen_isBanningTrue_unbanButtonDisabled() {
        composeTestRule.setContentWithTheme {
            UserScreenContent(
                state = UserState(
                    currentUserIsAdmin = true,
                    isBanning = true,
                    userDetail = UserDetailResponse(
                        userId = "user-1",
                        username = "testuser",
                        isAdmin = false,
                        isBanned = true,
                        submissionCount = 0,
                        ownedBikeCount = 0,
                        leaderboardRanks = emptyList()
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Unban User").assertDoesNotExist()
    }
}
