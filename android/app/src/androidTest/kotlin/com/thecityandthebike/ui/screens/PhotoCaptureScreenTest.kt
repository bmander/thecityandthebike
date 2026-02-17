package com.thecityandthebike.ui.screens

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.rule.GrantPermissionRule
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertEquals
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
                side = "left",
                onSideChanged = {},
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
                side = "left",
                onSideChanged = {},
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
                side = "left",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Capture photo")
            .assertIsDisplayed()
    }

    @Test
    fun photoCaptureScreen_captureButtonIs108dp() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "left",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Capture photo")
            .assertWidthIsEqualTo(108.dp)
            .assertHeightIsEqualTo(108.dp)
    }

    @Test
    fun photoCaptureScreen_displaysTemplateOverlay() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "left",
                onSideChanged = {},
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
                side = "left",
                onSideChanged = {},
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
                side = "left",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = { backCalled = true }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        assertTrue("onBack should be called when back button is clicked", backCalled)
    }

    @Test
    fun displaysFlashButton() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "left",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Flash auto")
            .assertIsDisplayed()
    }

    @Test
    fun flashToggle_cyclesToOn() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "left",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Flash auto")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Flash on")
            .assertIsDisplayed()
    }

    @Test
    fun flashToggle_cyclesToOff() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "left",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Flash auto")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Flash on")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Flash off")
            .assertIsDisplayed()
    }

    @Test
    fun sideToggle_displaysLeftButton() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "left",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithTag("side_left")
            .assertIsDisplayed()
    }

    @Test
    fun sideToggle_displaysRightButton() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "right",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithTag("side_right")
            .assertIsDisplayed()
    }

    @Test
    fun sideToggle_leftButtonCallsCallback() {
        var selectedSide = ""

        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "right",
                onSideChanged = { selectedSide = it },
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Left side")
            .performClick()

        assertEquals("left", selectedSide)
    }

    @Test
    fun sideToggle_rightButtonCallsCallback() {
        var selectedSide = ""

        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "left",
                onSideChanged = { selectedSide = it },
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Right side")
            .performClick()

        assertEquals("right", selectedSide)
    }

    @Test
    fun flashToggle_cyclesBackToAuto() {
        composeTestRule.setContentWithTheme {
            PhotoCaptureScreen(
                side = "left",
                onSideChanged = {},
                onPhotoCaptured = {},
                onBack = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Flash auto")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Flash on")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Flash off")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Flash auto")
            .assertIsDisplayed()
    }
}
