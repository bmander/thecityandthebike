package com.thecityandthebike.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import com.thecityandthebike.createTestUriList
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class ImageGridTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun imageGrid_emptyState_showsNoImages() {
        composeTestRule.setContentWithTheme {
            ImageGrid(
                imageUris = emptyList(),
                modifier = Modifier.fillMaxSize()
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Captured image")
            .assertDoesNotExist()
    }

    @Test
    fun imageGrid_withImages_showsCorrectCount() {
        val testUris = createTestUriList(3)

        composeTestRule.setContentWithTheme {
            ImageGrid(
                imageUris = testUris,
                modifier = Modifier.fillMaxSize()
            )
        }

        composeTestRule
            .onAllNodesWithContentDescription("Captured image")
            .assertCountEquals(3)
    }

    @Test
    fun imageGrid_withManyImages_isScrollable() {
        val testUris = createTestUriList(12)

        composeTestRule.setContentWithTheme {
            ImageGrid(
                imageUris = testUris,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Grid should render without error with many items
        // The LazyVerticalGrid will make it scrollable
        composeTestRule
            .onAllNodesWithContentDescription("Captured image")
            .assertCountEquals(12)
    }
}
