package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class ImageDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSubmission = SubmissionResponse(
        submissionId = "sub-1",
        userId = "user-1",
        bikeQrId = "BIKE-42",
        capturedDate = "2025-03-15",
        username = "testuser",
        provider = "citibike"
    )

    @Test
    fun imageDetailScreen_displaysSubmissionInfo() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("testuser").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mar 15, 2025").assertIsDisplayed()
    }

    @Test
    fun imageDetailScreen_ownerSeesDeleteAndDownloadButtons() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onBack = {},
                isOwner = true
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete photo").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Download photo").assertIsDisplayed()
    }

    @Test
    fun imageDetailScreen_nonOwnerDoesNotSeeDeleteDownloadButtons() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onBack = {},
                isOwner = false
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete photo").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Download photo").assertDoesNotExist()
    }

    @Test
    fun imageDetailScreen_deleteDialogAppearsAndDismisses() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onBack = {},
                isOwner = true,
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete photo").performClick()

        composeTestRule.onNodeWithText("Delete Photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete this photo? This action cannot be undone.")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithText("Delete Photo").assertDoesNotExist()
    }
}
