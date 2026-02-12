package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSubmission = SubmissionResponse(
        submissionId = "sub-1",
        userId = "user-1",
        bikeQrId = "BIKE-42",
        capturedDate = "2025-01-15",
        username = "testuser",
        provider = "citibike"
    )

    @Test
    fun homeButton_isDisplayed() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Home")
            .assertIsDisplayed()
    }

    @Test
    fun homeButton_clickCallsOnHome() {
        var homeCalled = false

        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = { homeCalled = true }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Home")
            .performClick()

        assertTrue("onHome should be called when home button is clicked", homeCalled)
    }

    @Test
    fun homeButton_doesNotShowBackArrow() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertDoesNotExist()
    }
}
