package com.thecityandthebike.ui.screens

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PhotoCaptureScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun photoCaptureScreen_opensWithoutCrash() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun photoCaptureScreen_displaysBackButton() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertIsDisplayed()
    }

    @Test
    fun photoCaptureScreen_displaysCaptureButton() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Capture photo")
            .assertIsDisplayed()
    }

    @Test
    fun photoCaptureScreen_displaysTemplateOverlay() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithTag("template_overlay")
            .assertExists()
    }

    @Test
    fun photoCaptureScreen_displaysCameraPreview() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithTag("camera_preview")
            .assertExists()
    }

    @Test
    fun photoCaptureScreen_backButtonCallsCallback() {
        var backCalled = false

        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                onPhotoCaptured = {},
                onBack = { backCalled = true }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        assertTrue("onBack should be called when back button is clicked", backCalled)
    }
}
