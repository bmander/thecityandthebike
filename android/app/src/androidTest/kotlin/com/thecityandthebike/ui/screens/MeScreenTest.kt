package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.viewmodel.MeViewModel
import com.thecityandthebike.ui.viewmodel.UserState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class MeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockViewModel(): MeViewModel {
        val viewModel = mockk<MeViewModel>(relaxed = true)
        every { viewModel.state } returns MutableStateFlow(UserState())
        return viewModel
    }

    @Test
    fun meScreen_displaysTitle() {
        composeTestRule.setContentWithTheme {
            MeScreen(
                viewModel = createMockViewModel(),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onAllNodesWithText("Me")[0].assertIsDisplayed()
    }

    @Test
    fun meScreen_backButtonPresent() {
        composeTestRule.setContentWithTheme {
            MeScreen(
                viewModel = createMockViewModel(),
                onBack = {},
                onImageClick = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
}
