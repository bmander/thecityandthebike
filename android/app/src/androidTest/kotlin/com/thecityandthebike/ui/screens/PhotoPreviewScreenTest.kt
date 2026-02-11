package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.thecityandthebike.createTestUri
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PhotoPreviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun photoPreviewScreen_displaysImage() {
        composeTestRule.setContentWithTheme {
            PhotoPreviewScreen(
                photoUri = createTestUri(),
                onConfirm = {},
                onRetake = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Photo preview")
            .assertIsDisplayed()
    }

    @Test
    fun photoPreviewScreen_displaysConfirmButton() {
        composeTestRule.setContentWithTheme {
            PhotoPreviewScreen(
                photoUri = createTestUri(),
                onConfirm = {},
                onRetake = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Confirm photo")
            .assertIsDisplayed()
    }

    @Test
    fun photoPreviewScreen_displaysRetakeButton() {
        composeTestRule.setContentWithTheme {
            PhotoPreviewScreen(
                photoUri = createTestUri(),
                onConfirm = {},
                onRetake = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Retake photo")
            .assertIsDisplayed()
    }

    @Test
    fun photoPreviewScreen_confirmButtonCallsCallback() {
        var confirmCalled = false

        composeTestRule.setContentWithTheme {
            PhotoPreviewScreen(
                photoUri = createTestUri(),
                onConfirm = { confirmCalled = true },
                onRetake = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Confirm photo")
            .performClick()

        assertTrue("onConfirm should be called when confirm button is clicked", confirmCalled)
    }

    @Test
    fun photoPreviewScreen_retakeButtonCallsCallback() {
        var retakeCalled = false

        composeTestRule.setContentWithTheme {
            PhotoPreviewScreen(
                photoUri = createTestUri(),
                onConfirm = {},
                onRetake = { retakeCalled = true }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Retake photo")
            .performClick()

        assertTrue("onRetake should be called when retake button is clicked", retakeCalled)
    }
}
