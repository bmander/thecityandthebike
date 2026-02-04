package com.thecityandthebike.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraFABTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun cameraFAB_isDisplayed() {
        composeTestRule.setContentWithTheme {
            CameraFAB(onClick = {})
        }

        composeTestRule
            .onNodeWithContentDescription("Add image")
            .assertIsDisplayed()
    }

    @Test
    fun cameraFAB_clickCallsOnClick() {
        var clicked = false

        composeTestRule.setContentWithTheme {
            CameraFAB(onClick = { clicked = true })
        }

        composeTestRule
            .onNodeWithContentDescription("Add image")
            .performClick()

        assertTrue("onClick should be called when FAB is clicked", clicked)
    }

    @Test
    fun cameraFAB_hasAccessibleContentDescription() {
        composeTestRule.setContentWithTheme {
            CameraFAB(onClick = {})
        }

        composeTestRule
            .onNodeWithContentDescription("Add image")
            .assertExists()
    }
}
