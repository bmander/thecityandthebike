package com.thecityandthebike.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.data.model.dto.ScoringBreakdown
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun overlay_showsDismissButton() {
        val breakdown = listOf(
            ScoringBreakdown(eventType = "tag", label = "Tag Created", points = 5)
        )

        composeTestRule.setContentWithTheme {
            PointsAwardedOverlay(breakdown = breakdown, onDismiss = {})
        }

        composeTestRule.onNodeWithText("\u00D7").assertExists()
    }

    @Test
    fun overlay_dismissButton_callsOnDismiss() {
        var dismissed = false
        val breakdown = listOf(
            ScoringBreakdown(eventType = "tag", label = "Tag Created", points = 5)
        )

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContentWithTheme {
            PointsAwardedOverlay(
                breakdown = breakdown,
                onDismiss = { dismissed = true }
            )
        }

        // Advance past entry animation (200 + 300 + 150 = 650ms)
        composeTestRule.mainClock.advanceTimeBy(1000)

        // Click the dismiss button
        composeTestRule.onNodeWithText("\u00D7").performClick()

        // Advance past exit animation (300ms)
        composeTestRule.mainClock.advanceTimeBy(500)

        assertTrue(dismissed)
    }

    @Test
    fun overlay_showsScoreRulesButton_whenCallbackProvided() {
        val breakdown = listOf(
            ScoringBreakdown(eventType = "tag", label = "Tag Created", points = 5)
        )

        composeTestRule.setContentWithTheme {
            PointsAwardedOverlay(
                breakdown = breakdown,
                onDismiss = {},
                onShowScoreRules = {}
            )
        }

        composeTestRule.onNodeWithText("?").assertExists()
    }

    @Test
    fun overlay_hidesScoreRulesButton_whenCallbackNull() {
        val breakdown = listOf(
            ScoringBreakdown(eventType = "tag", label = "Tag Created", points = 5)
        )

        composeTestRule.setContentWithTheme {
            PointsAwardedOverlay(
                breakdown = breakdown,
                onDismiss = {},
                onShowScoreRules = null
            )
        }

        composeTestRule.onNodeWithText("?").assertDoesNotExist()
    }

    @Test
    fun overlay_scoreRulesButton_callsBothCallbacks() {
        var dismissed = false
        var scoreRulesShown = false
        val breakdown = listOf(
            ScoringBreakdown(eventType = "tag", label = "Tag Created", points = 5)
        )

        composeTestRule.setContentWithTheme {
            PointsAwardedOverlay(
                breakdown = breakdown,
                onDismiss = { dismissed = true },
                onShowScoreRules = { scoreRulesShown = true }
            )
        }

        composeTestRule.onNodeWithText("?").performClick()

        assertTrue(dismissed)
        assertTrue(scoreRulesShown)
    }
}
