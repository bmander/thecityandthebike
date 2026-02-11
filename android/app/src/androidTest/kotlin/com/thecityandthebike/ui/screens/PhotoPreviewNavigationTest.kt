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
import androidx.navigation.toRoute
import com.thecityandthebike.navigation.Main
import com.thecityandthebike.navigation.PhotoCapture
import com.thecityandthebike.navigation.PhotoPreview
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
                startDestination = Main
            ) {
                composable<Main> {
                    Text("Main Screen")
                }
                composable<PhotoCapture> { backStackEntry ->
                    val route = backStackEntry.toRoute<PhotoCapture>()
                    Text("Capture Screen")
                    PhotoCaptureScreen(
                        onPhotoCaptured = { uri ->
                            navController.navigate(PhotoPreview(route.qrId, uri.toString())) {
                                popUpTo<PhotoCapture> { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable<PhotoPreview> { backStackEntry ->
                    val route = backStackEntry.toRoute<PhotoPreview>()
                    val photoUri = Uri.parse(route.photoUri)
                    PhotoPreviewScreen(
                        photoUri = photoUri,
                        onConfirm = {
                            onConfirm()
                            navController.popBackStack<Main>(inclusive = false)
                        },
                        onRetake = {
                            navController.navigate(PhotoCapture(route.qrId)) {
                                popUpTo<PhotoPreview> { inclusive = true }
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
            navController.navigate(PhotoPreview(qrId = "test-qr-123", photoUri = "content://test/photo.jpg"))
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
            navController.navigate(PhotoPreview(qrId = "test-qr-123", photoUri = "content://test/photo.jpg"))
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Confirm photo")
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Main Screen")
            .assertIsDisplayed()
        assertNotNull(navController.currentBackStackEntry?.toRoute<Main>())
    }

    @Test
    fun previewConfirm_callsConfirmCallback() {
        lateinit var navController: TestNavHostController
        var confirmCalled = false

        setupNavGraph(onConfirm = { confirmCalled = true }) { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate(PhotoPreview(qrId = "test-qr-123", photoUri = "content://test/photo.jpg"))
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
            navController.navigate(PhotoPreview(qrId = "test-qr-123", photoUri = "content://test/photo.jpg"))
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Retake photo")
            .performClick()
        composeTestRule.waitForIdle()

        assertNotNull(navController.currentBackStackEntry?.toRoute<PhotoCapture>())
    }

    @Test
    fun previewRetake_preservesQrId() {
        lateinit var navController: TestNavHostController
        val testQrId = "bike-42"

        setupNavGraph { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate(PhotoPreview(qrId = testQrId, photoUri = "content://test/photo.jpg"))
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Retake photo")
            .performClick()
        composeTestRule.waitForIdle()

        val route = navController.currentBackStackEntry?.toRoute<PhotoCapture>()
        assertEquals(testQrId, route?.qrId)
    }

    @Test
    fun uriSurvivesEncodingThroughNavArgs() {
        lateinit var navController: TestNavHostController
        val originalUri = "content://com.thecityandthebike/photos/test.jpg"

        setupNavGraph { navController = it }

        composeTestRule.runOnUiThread {
            navController.navigate(PhotoPreview(qrId = "test-qr", photoUri = originalUri))
        }
        composeTestRule.waitForIdle()

        val route = navController.currentBackStackEntry?.toRoute<PhotoPreview>()
        assertEquals(originalUri, route?.photoUri)
    }
}
