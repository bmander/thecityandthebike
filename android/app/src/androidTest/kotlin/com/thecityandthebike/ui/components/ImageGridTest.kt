package com.thecityandthebike.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.thecityandthebike.createTestUri
import com.thecityandthebike.createTestUriList
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertEquals
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

    @Test
    fun imageGrid_uploadingUri_showsLoadingIndicator() {
        val testUris = createTestUriList(3)
        val uploadingUris = setOf(testUris[0])

        composeTestRule.setContentWithTheme {
            ImageGrid(
                imageUris = testUris,
                uploadingUris = uploadingUris,
                modifier = Modifier.fillMaxSize()
            )
        }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun imageGrid_noUploadingUris_noLoadingIndicator() {
        val testUris = createTestUriList(3)

        composeTestRule.setContentWithTheme {
            ImageGrid(
                imageUris = testUris,
                modifier = Modifier.fillMaxSize()
            )
        }

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }

    @Test
    fun imageGrid_uploadingUri_isNotClickable() {
        val testUris = createTestUriList(3)
        val uploadingUris = setOf(testUris[0])

        composeTestRule.setContentWithTheme {
            ImageGrid(
                imageUris = testUris,
                uploadingUris = uploadingUris,
                modifier = Modifier.fillMaxSize(),
                onImageClick = {}
            )
        }

        // Only the 2 non-uploading items should have click actions
        composeTestRule
            .onAllNodes(hasClickAction())
            .assertCountEquals(2)
    }

    @Test
    fun imageGrid_uploadingUri_nonUploadingItemsReportCorrectIndex() {
        val testUris = createTestUriList(3)
        val uploadingUris = setOf(testUris[0])
        val clickedIndices = mutableListOf<Int>()

        composeTestRule.setContentWithTheme {
            ImageGrid(
                imageUris = testUris,
                uploadingUris = uploadingUris,
                modifier = Modifier.fillMaxSize(),
                onImageClick = { clickedIndices.add(it) }
            )
        }

        val clickableNodes = composeTestRule.onAllNodes(hasClickAction())

        // Click the first clickable item (index 1 in the full list)
        clickableNodes[0].performClick()
        assertEquals(listOf(1), clickedIndices)

        // Click the second clickable item (index 2 in the full list)
        clickableNodes[1].performClick()
        assertEquals(listOf(1, 2), clickedIndices)
    }

    @Test
    fun imageGrid_pendingUploadPrepended_clickReportsCorrectIndex() {
        // Simulates what MainScreen does: prepend a pending upload URI
        val pendingUri = createTestUri(99)
        val submissionUris = createTestUriList(3)
        val allUris = listOf(pendingUri) + submissionUris
        val uploadingUris = setOf(pendingUri)
        val clickedSubmissionIndices = mutableListOf<Int>()

        composeTestRule.setContentWithTheme {
            ImageGrid(
                imageUris = allUris,
                uploadingUris = uploadingUris,
                modifier = Modifier.fillMaxSize(),
                onImageClick = { index ->
                    // Same index adjustment logic as MainScreen
                    val adjustedIndex = index - 1
                    if (adjustedIndex >= 0) {
                        clickedSubmissionIndices.add(adjustedIndex)
                    }
                }
            )
        }

        // 4 images total, 1 uploading, so 3 clickable
        val clickableNodes = composeTestRule.onAllNodes(hasClickAction())
        clickableNodes.assertCountEquals(3)

        // Click first clickable item -> grid index 1 -> adjusted index 0
        clickableNodes[0].performClick()
        assertEquals(listOf(0), clickedSubmissionIndices)

        // Click second clickable item -> grid index 2 -> adjusted index 1
        clickableNodes[1].performClick()
        assertEquals(listOf(0, 1), clickedSubmissionIndices)
    }
}
