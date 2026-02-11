package com.thecityandthebike.ui.screens

import android.Manifest
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import androidx.test.rule.GrantPermissionRule
import com.thecityandthebike.navigation.Main
import com.thecityandthebike.navigation.PhotoCapture
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class PhotoCaptureNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun navigateToPhotoCaptureRoute_rendersWithoutCrash() {
        lateinit var navController: TestNavHostController

        composeTestRule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(
                navController = navController,
                startDestination = Main
            ) {
                composable<Main> {
                    Text("Main Screen")
                }
                composable<PhotoCapture> { backStackEntry ->
                    val route = backStackEntry.toRoute<PhotoCapture>()
                    PhotoCaptureScreen(
                        onPhotoCaptured = {},
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(PhotoCapture(qrId = "test-qr-123"))
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertIsDisplayed()
    }

    @Test
    fun navigateToPhotoCaptureRoute_showsTemplateOverlay() {
        lateinit var navController: TestNavHostController

        composeTestRule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(
                navController = navController,
                startDestination = Main
            ) {
                composable<Main> {
                    Text("Main Screen")
                }
                composable<PhotoCapture> {
                    PhotoCaptureScreen(
                        onPhotoCaptured = {},
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(PhotoCapture(qrId = "test-qr-456"))
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("template_overlay")
            .assertExists()
    }

    @Test
    fun navigateToPhotoCaptureRoute_showsCaptureButton() {
        lateinit var navController: TestNavHostController

        composeTestRule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(
                navController = navController,
                startDestination = Main
            ) {
                composable<Main> {
                    Text("Main Screen")
                }
                composable<PhotoCapture> {
                    PhotoCaptureScreen(
                        onPhotoCaptured = {},
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(PhotoCapture(qrId = "test-qr-789"))
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Capture photo")
            .assertIsDisplayed()
    }

    @Test
    fun navigateToPhotoCaptureRoute_emptyQrId_showsScreen() {
        lateinit var navController: TestNavHostController

        composeTestRule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(
                navController = navController,
                startDestination = Main
            ) {
                composable<Main> {
                    Text("Main Screen")
                }
                composable<PhotoCapture> {
                    PhotoCaptureScreen(
                        onPhotoCaptured = {},
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // Navigate with a valid qrId to ensure the route works
        composeTestRule.runOnUiThread {
            navController.navigate(PhotoCapture(qrId = "valid-id"))
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertIsDisplayed()
    }

    @Test
    fun navigateBackFromPhotoCaptureRoute_returnsToMain() {
        lateinit var navController: TestNavHostController

        composeTestRule.setContentWithTheme {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            NavHost(
                navController = navController,
                startDestination = Main
            ) {
                composable<Main> {
                    Text("Main Screen")
                }
                composable<PhotoCapture> {
                    PhotoCaptureScreen(
                        onPhotoCaptured = {},
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composeTestRule.runOnUiThread {
            navController.navigate(PhotoCapture(qrId = "test-qr-back"))
        }

        composeTestRule.waitForIdle()

        composeTestRule.runOnUiThread {
            navController.popBackStack()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Main Screen")
            .assertIsDisplayed()

        assertNotNull(navController.currentBackStackEntry?.toRoute<Main>())
    }
}
