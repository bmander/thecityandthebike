package com.thecityandthebike.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.createTestUri
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CalendarEntryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createPhotos(count: Int): List<CalendarPhoto> =
        (0 until count).map { i ->
            CalendarPhoto(
                submissionId = "sub-$i",
                imageUri = createTestUri(i)
            )
        }

    @Test
    fun displaysDateLabel() {
        composeTestRule.setContentWithTheme {
            CalendarEntry(
                dateLabel = "Jan 15",
                yearLabel = null,
                photos = createPhotos(1),
                onImageClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("Jan 15")
            .assertIsDisplayed()
    }

    @Test
    fun displaysYearLabelWhenProvided() {
        composeTestRule.setContentWithTheme {
            CalendarEntry(
                dateLabel = "Jan 15",
                yearLabel = "2024",
                photos = createPhotos(1),
                onImageClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("2024")
            .assertIsDisplayed()
    }

    @Test
    fun hidesYearLabelWhenNull() {
        composeTestRule.setContentWithTheme {
            CalendarEntry(
                dateLabel = "Jan 15",
                yearLabel = null,
                photos = createPhotos(1),
                onImageClick = {}
            )
        }

        composeTestRule
            .onNodeWithText("2024")
            .assertDoesNotExist()
    }

    @Test
    fun showsCorrectNumberOfPhotos() {
        composeTestRule.setContentWithTheme {
            CalendarEntry(
                dateLabel = "Jan 15",
                yearLabel = "2024",
                photos = createPhotos(5),
                onImageClick = {}
            )
        }

        composeTestRule
            .onAllNodesWithContentDescription("Captured image")
            .assertCountEquals(5)
    }

    @Test
    fun clickTriggersOnImageClickWithCorrectId() {
        var clickedId: String? = null

        composeTestRule.setContentWithTheme {
            CalendarEntry(
                dateLabel = "Jan 15",
                yearLabel = "2024",
                photos = createPhotos(3),
                onImageClick = { clickedId = it }
            )
        }

        composeTestRule
            .onAllNodesWithContentDescription("Captured image")[1]
            .performClick()

        assertEquals("sub-1", clickedId)
    }

    @Test
    fun emptyPhotosListRendersWithoutError() {
        composeTestRule.setContentWithTheme {
            CalendarEntry(
                dateLabel = "Jan 15",
                yearLabel = "2024",
                photos = emptyList(),
                onImageClick = {}
            )
        }

        composeTestRule
            .onAllNodesWithContentDescription("Captured image")
            .assertCountEquals(0)

        composeTestRule
            .onNodeWithText("Jan 15")
            .assertIsDisplayed()
    }
}
