package com.thecityandthebike

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

@HiltAndroidTest
class MainActivityTest {

    private val hiltRule = HiltAndroidRule(this)

    /** Mark onboarding completed before the activity launches so tests go straight to MainScreen. */
    private val skipOnboardingRule = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("completed", true).apply()
        }

        override fun after() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(skipOnboardingRule)
        .around(composeTestRule)

    @Test
    fun mainActivity_launchesSuccessfully() {
        // Activity should launch without crashing
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainActivity_mainFeedVisibleAfterLaunch() {
        composeTestRule.waitForIdle()

        // App should show main feed with login button when not authenticated
        composeTestRule
            .onNodeWithContentDescription("Login")
            .assertIsDisplayed()
    }

    @Test
    fun mainActivity_logoutButtonNotVisibleWhenNotLoggedIn() {
        composeTestRule.waitForIdle()

        // Logout button should not be visible when not authenticated
        composeTestRule
            .onNodeWithContentDescription("Logout")
            .assertDoesNotExist()
    }
}
