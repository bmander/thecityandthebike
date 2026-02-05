package com.thecityandthebike

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
    fun mainActivity_loginScreenVisibleAfterLaunch() {
        composeTestRule.waitForIdle()

        // App should show login screen when not authenticated
        composeTestRule
            .onNodeWithText("The City and the Bike")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Sign In")
            .assertIsDisplayed()
    }
}
