package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.thecityandthebike.data.model.ScoreRules
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class ScoreRulesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun scoreRulesScreen_displaysTitle() {
        composeTestRule.setContentWithTheme {
            ScoreRulesScreen(onBack = {})
        }

        composeTestRule.onAllNodesWithText("Score Rules")[0].assertIsDisplayed()
    }

    @Test
    fun scoreRulesScreen_backButtonPresent() {
        composeTestRule.setContentWithTheme {
            ScoreRulesScreen(onBack = {})
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun scoreRulesScreen_displaysPhotoPointsSection() {
        composeTestRule.setContentWithTheme {
            ScoreRulesScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Photo Points").assertIsDisplayed()
        ScoreRules.photoRules.forEach { rule ->
            composeTestRule.onNodeWithText(rule.label).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("A single photo can trigger several bonuses at once \u2014 the points add up!").assertIsDisplayed()
    }

    @Test
    fun scoreRulesScreen_displaysTagPointsSection() {
        composeTestRule.setContentWithTheme {
            ScoreRulesScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Tag Points").assertIsDisplayed()
        ScoreRules.tagRules.forEach { rule ->
            composeTestRule.onNodeWithText(rule.label).assertIsDisplayed()
        }
    }
}
