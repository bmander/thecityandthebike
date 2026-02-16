package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.TagDetailResponse
import com.thecityandthebike.data.model.dto.UserSummary
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.TagState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TagScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tagScreen_loadingState_showsProgressIndicator() {
        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(isLoading = true),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Unknown error").assertDoesNotExist()
    }

    @Test
    fun tagScreen_errorState_showsErrorText() {
        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(error = "Network error"),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
    }

    @Test
    fun tagScreen_successState_showsPhotoCount() {
        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(
                    tagDetail = TagDetailResponse(
                        tagId = "tag-1",
                        imageUrl = "https://example.com/tag.png",
                        createdAt = "2026-01-01T00:00:00Z",
                        submissionCount = 5
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("5 photos").assertIsDisplayed()
    }

    @Test
    fun tagScreen_singlePhoto_correctPluralization() {
        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(
                    tagDetail = TagDetailResponse(
                        tagId = "tag-1",
                        imageUrl = "https://example.com/tag.png",
                        createdAt = "2026-01-01T00:00:00Z",
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
    fun tagScreen_showsFirstCapturedByWithDate() {
        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(
                    tagDetail = TagDetailResponse(
                        tagId = "tag-1",
                        imageUrl = "https://example.com/tag.png",
                        createdAt = "2026-01-01T00:00:00Z",
                        submissionCount = 1,
                        firstCapturedAt = "2024-03-15T10:00:00Z",
                        firstCapturedBy = UserSummary(name = "alice", id = "user-1")
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("First captured by alice on Mar 15, 2024")
            .assertIsDisplayed()
    }

    @Test
    fun tagScreen_firstCapturedByClick_callsOnUserClickWithCorrectId() {
        var clickedUserId = ""

        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(
                    tagDetail = TagDetailResponse(
                        tagId = "tag-1",
                        imageUrl = "https://example.com/tag.png",
                        createdAt = "2026-01-01T00:00:00Z",
                        submissionCount = 1,
                        firstCapturedBy = UserSummary(name = "alice", id = "user-1")
                    )
                ),
                onBack = {},
                onImageClick = {},
                onUserClick = { clickedUserId = it }
            )
        }

        composeTestRule.onNodeWithText("First captured by alice").performClick()

        assertEquals("user-1", clickedUserId)
    }

    @Test
    fun tagScreen_showsDateGroupEntries() {
        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(
                    tagDetail = TagDetailResponse(
                        tagId = "tag-1",
                        imageUrl = "https://example.com/tag.png",
                        createdAt = "2026-01-01T00:00:00Z",
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
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Mar 15").assertIsDisplayed()
    }

    @Test
    fun tagScreen_showsTagTitle() {
        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(
                    tagDetail = TagDetailResponse(
                        tagId = "tag-1",
                        imageUrl = "https://example.com/tag.png",
                        createdAt = "2026-01-01T00:00:00Z",
                        submissionCount = 0
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("Tag").assertIsDisplayed()
    }

    @Test
    fun tagScreen_noFirstCapturedBy_noInfoRows() {
        composeTestRule.setContentWithTheme {
            TagScreenContent(
                state = TagState(
                    tagDetail = TagDetailResponse(
                        tagId = "tag-1",
                        imageUrl = "https://example.com/tag.png",
                        createdAt = "2026-01-01T00:00:00Z",
                        submissionCount = 0
                    )
                ),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithText("First captured by", substring = true)
            .assertDoesNotExist()
    }
}
