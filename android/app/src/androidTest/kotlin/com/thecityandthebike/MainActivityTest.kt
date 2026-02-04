package com.thecityandthebike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class MainActivityTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(hiltRule).around(composeTestRule)

    @Test
    fun mainActivity_launchesSuccessfully() {
        // Activity should launch without crashing
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainActivity_componentsVisibleAfterLaunch() {
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Add image")
            .assertIsDisplayed()
    }
}
