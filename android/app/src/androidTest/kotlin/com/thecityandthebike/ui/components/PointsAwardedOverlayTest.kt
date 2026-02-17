package com.thecityandthebike.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.thecityandthebike.data.model.dto.ScoringBreakdown
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class PointsAwardedOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overlay_displaysBreakdownLabelsAndPoints() {
        val breakdown = listOf(
            ScoringBreakdown(eventType = "tag", label = "Tag Created", points = 5),
            ScoringBreakdown(eventType = "part", label = "Bike Part", points = 10)
        )

        composeTestRule.setContentWithTheme {
            PointsAwardedOverlay(breakdown = breakdown, onDismiss = {})
        }

        composeTestRule.onNodeWithText("Tag Created").assertIsDisplayed()
        composeTestRule.onNodeWithText("+5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bike Part").assertIsDisplayed()
        composeTestRule.onNodeWithText("+10").assertIsDisplayed()
    }

    @Test
    fun overlay_displaysTotalPoints() {
        val breakdown = listOf(
            ScoringBreakdown(eventType = "tag", label = "Tag Created", points = 5),
            ScoringBreakdown(eventType = "part", label = "Bike Part", points = 10)
        )

        composeTestRule.setContentWithTheme {
            PointsAwardedOverlay(breakdown = breakdown, onDismiss = {})
        }

        composeTestRule.onNodeWithText("+15").assertIsDisplayed()
    }

    @Test
    fun overlay_singleItem_displaysTotalMatchingItem() {
        val breakdown = listOf(
            ScoringBreakdown(eventType = "tag", label = "Tag Created", points = 7)
        )

        composeTestRule.setContentWithTheme {
            PointsAwardedOverlay(breakdown = breakdown, onDismiss = {})
        }

        composeTestRule.onNodeWithText("Tag Created").assertIsDisplayed()
        // "+7" appears twice: once in the breakdown row and once as the total
        composeTestRule.onAllNodesWithText("+7").assertCountEquals(2)
    }
}
