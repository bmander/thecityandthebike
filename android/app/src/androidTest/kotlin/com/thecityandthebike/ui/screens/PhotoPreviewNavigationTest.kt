package com.thecityandthebike.ui.screens

import android.net.Uri
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
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PhotoPreviewNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupNavGraph(
        onConfirm: () -> Unit = {},
        navControllerRef: (TestNavHostController) -> Unit
    ) {
        composeTestRule.setContentWithTheme {
            val navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            navControllerRef(navController)

            NavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable("main") {
                    Text("Main Screen")
                }
                composable("photo_capture/{qrId}") { backStackEntry ->
                    val qrId = backStackEntry.arguments?.getString("qrId") ?: ""
                    Text("Capture Screen")
                    PhotoCaptureScreen(
                        onPhotoCaptured = { uri ->
                            navController.navigate("photo_preview/${Uri.encode(qrId)}/${Uri.encode(uri.toString())}") {
                                popUpTo("photo_capture/{qrId}") { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable("photo_preview/{qrId}/{photoUri}") { backStackEntry ->
                    val qrId = backStackEntry.arguments?.getString("qrId") ?: ""
                    val photoUriString = backStackEntry.arguments?.getString("photoUri") ?: ""
                    val photoUri = Uri.parse(photoUriString)
                    PhotoPreviewScreen(
                        photoUri = photoUri,
                        onConfirm = {
                            onConfirm()
                            navController.popBackStack("main", inclusive = false)
                        },
                        onRetake = {
                            navController.navigate("photo_capture/${Uri.encode(qrId)}") {
                                popUpTo("photo_preview/{qrId}/{photoUri}") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

    @Test
    fun navigateToPreview_displaysPreviewScreen() {
        lateinit var navController: TestNavHostController

        setupNavGraph { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate("photo_preview/test-qr-123/content%3A%2F%2Ftest%2Fphoto.jpg")
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Photo preview")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Confirm photo")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Retake photo")
            .assertIsDisplayed()
    }

    @Test
    fun previewConfirm_returnsToMain() {
        lateinit var navController: TestNavHostController

        setupNavGraph { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate("photo_preview/test-qr-123/content%3A%2F%2Ftest%2Fphoto.jpg")
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Confirm photo")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Main Screen")
            .assertIsDisplayed()
        assertEquals("main", navController.currentDestination?.route)
    }

    @Test
    fun previewConfirm_callsConfirmCallback() {
        lateinit var navController: TestNavHostController
        var confirmCalled = false

        setupNavGraph(onConfirm = { confirmCalled = true }) { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate("photo_preview/test-qr-123/content%3A%2F%2Ftest%2Fphoto.jpg")
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Confirm photo")
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("onConfirm should be called", true, confirmCalled)
    }

    @Test
    fun previewRetake_navigatesToCapture() {
        lateinit var navController: TestNavHostController

        setupNavGraph { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate("photo_preview/test-qr-123/content%3A%2F%2Ftest%2Fphoto.jpg")
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Retake photo")
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals("photo_capture/{qrId}", navController.currentDestination?.route)
    }

    @Test
    fun previewRetake_preservesQrId() {
        lateinit var navController: TestNavHostController
        val testQrId = "bike-42"

        setupNavGraph { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate("photo_preview/$testQrId/content%3A%2F%2Ftest%2Fphoto.jpg")
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Retake photo")
            .performClick()
        composeTestRule.waitForIdle()

        val currentArgs = navController.currentBackStackEntry?.arguments
        assertEquals(testQrId, currentArgs?.getString("qrId"))
    }

    @Test
    fun uriSurvivesEncodingThroughNavArgs() {
        lateinit var navController: TestNavHostController
        val originalUri = "content://com.thecityandthebike/photos/test.jpg"
        val encodedUri = Uri.encode(originalUri)

        setupNavGraph { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate("photo_preview/test-qr/$encodedUri")
        }
        composeTestRule.waitForIdle()

        val photoUriArg = navController.currentBackStackEntry?.arguments?.getString("photoUri")
        assertEquals(originalUri, photoUriArg)
    }
}
