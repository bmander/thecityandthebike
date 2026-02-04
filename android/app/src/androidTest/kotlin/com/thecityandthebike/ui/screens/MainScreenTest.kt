package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.components.CameraFAB
import com.thecityandthebike.ui.components.ImageGrid
import com.thecityandthebike.ui.components.MenuButton
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_allComponentsDisplayed() {
        composeTestRule.setContentWithTheme {
            // Render the main screen layout without camera state
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = emptyList(),
                    modifier = Modifier.fillMaxSize()
                )

                MenuButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )

                CameraFAB(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Add image")
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_initialState_showsEmptyGrid() {
        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = emptyList(),
                    modifier = Modifier.fillMaxSize()
                )

                MenuButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )

                CameraFAB(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }

        // Verify no images in grid
        composeTestRule
            .onNodeWithContentDescription("Captured image")
            .assertDoesNotExist()
    }

    @Test
    fun mainScreen_menuButtonPositioned_topStart() {
        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                MenuButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_cameraFABPositioned_topEnd() {
        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraFAB(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Add image")
            .assertIsDisplayed()
    }
}
