package com.thecityandthebike.ui.screens

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.navigation.Bike
import com.thecityandthebike.navigation.BikeImageDetail
import com.thecityandthebike.navigation.ImageDetail
import com.thecityandthebike.navigation.Main
import com.thecityandthebike.navigation.User
import com.thecityandthebike.navigation.UserImageDetail
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ImageDetailNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testSubmission = SubmissionResponse(
        submissionId = "sub-1",
        userId = "user-1",
        bikeQrId = "BIKE-42",
        capturedDate = "2025-01-15",
        username = "testuser",
        provider = "citibike"
    )

    @Test
    fun homeButton_fromImageDetail_navigatesToMain() {
        lateinit var navController: TestNavHostController

        composeTestRule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController = navController, startDestination = Main) {
                composable<Main> { Text("Main Screen") }
                composable<ImageDetail> {
                    ImageDetailScreen(
                        submission = testSubmission,
                        onHome = { navController.popBackStack<Main>(inclusive = false) }
                    )
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(ImageDetail("sub-1"))
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Home")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Main Screen")
            .assertIsDisplayed()
    }

    @Test
    fun homeButton_fromDeepStack_skipsIntermediateScreens() {
        lateinit var navController: TestNavHostController

        composeTestRule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController = navController, startDestination = Main) {
                composable<Main> { Text("Main Screen") }
                composable<ImageDetail> {
                    ImageDetailScreen(
                        submission = testSubmission,
                        onHome = { navController.popBackStack<Main>(inclusive = false) }
                    )
                }
                composable<Bike> { Text("Bike Screen") }
                composable<BikeImageDetail> {
                    ImageDetailScreen(
                        submission = testSubmission,
                        onHome = { navController.popBackStack<Main>(inclusive = false) }
                    )
                }
            }
        }

        // Build up a deep stack: Main → ImageDetail → Bike → BikeImageDetail
        composeTestRule.runOnUiThread {
            navController.navigate(ImageDetail("sub-1"))
            navController.navigate(Bike("BIKE-42"))
            navController.navigate(BikeImageDetail("sub-1"))
        }
        composeTestRule.waitForIdle()

        // Verify we're on the BikeImageDetail screen
        composeTestRule
            .onNodeWithContentDescription("Home")
            .assertIsDisplayed()

        // Click home — should go all the way back to Main
        composeTestRule
            .onNodeWithContentDescription("Home")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Main Screen")
            .assertIsDisplayed()

        // Verify the back stack only has Main
        val currentRoute = navController.currentBackStackEntry?.toRoute<Main>()
        assertEquals(Main, currentRoute)
    }

    @Test
    fun homeButton_fromUserImageDetail_navigatesToMain() {
        lateinit var navController: TestNavHostController

        composeTestRule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(navController = navController, startDestination = Main) {
                composable<Main> { Text("Main Screen") }
                composable<ImageDetail> {
                    ImageDetailScreen(
                        submission = testSubmission,
                        onHome = { navController.popBackStack<Main>(inclusive = false) }
                    )
                }
                composable<User> { Text("User Screen") }
                composable<UserImageDetail> {
                    ImageDetailScreen(
                        submission = testSubmission,
                        onHome = { navController.popBackStack<Main>(inclusive = false) }
                    )
                }
            }
        }

        // Build stack: Main → ImageDetail → User → UserImageDetail
        composeTestRule.runOnUiThread {
            navController.navigate(ImageDetail("sub-1"))
            navController.navigate(User("user-1"))
            navController.navigate(UserImageDetail("sub-1"))
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Home")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Main Screen")
            .assertIsDisplayed()
    }
}
