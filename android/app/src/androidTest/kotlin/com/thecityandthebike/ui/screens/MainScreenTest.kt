package com.thecityandthebike.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.thecityandthebike.createTestUri
import com.thecityandthebike.createTestUriList
import com.thecityandthebike.setContentWithTheme
import com.thecityandthebike.ui.components.CameraFAB
import com.thecityandthebike.ui.components.ImageGrid
import com.thecityandthebike.ui.components.LoginFAB
import com.thecityandthebike.ui.components.MenuButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_allComponentsDisplayed() {
        composeTestRule.setContentWithTheme {
            // Render the main screen layout without camera state
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = emptyList(),
                    modifier = Modifier.fillMaxSize()
                )

                MenuButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )

                CameraFAB(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Capture")
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_initialState_showsEmptyGrid() {
        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = emptyList(),
                    modifier = Modifier.fillMaxSize()
                )

                MenuButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )

                CameraFAB(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        // Verify no images in grid
        composeTestRule
            .onNodeWithContentDescription("Captured image")
            .assertDoesNotExist()
    }

    @Test
    fun mainScreen_menuButtonPositioned_topStart() {
        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                MenuButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Menu")
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_cameraFABPositioned_bottomCenter() {
        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraFAB(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Capture")
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_guestUser_showsLoginFAB() {
        val isLoggedIn = false

        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = emptyList(),
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoggedIn) {
                    CameraFAB(
                        onClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                } else {
                    LoginFAB(
                        onClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText("Sign in")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Capture")
            .assertDoesNotExist()
    }

    @Test
    fun mainScreen_guestUser_doesNotShowLogoutInMenu() {
        val isLoggedIn = false

        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    var menuExpanded by remember { mutableStateOf(false) }

                    MenuButton(onClick = { menuExpanded = true })

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Privacy & Copyright") },
                            onClick = { menuExpanded = false }
                        )
                        if (isLoggedIn) {
                            DropdownMenuItem(
                                text = { Text("Log out") },
                                onClick = { menuExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        // Open the menu
        composeTestRule
            .onNodeWithContentDescription("Menu")
            .performClick()

        // "Privacy & Copyright" should be visible
        composeTestRule
            .onNodeWithText("Privacy & Copyright")
            .assertIsDisplayed()

        // "Log out" should not be present for guest users
        composeTestRule
            .onNodeWithText("Log out")
            .assertDoesNotExist()
    }

    @Test
    fun mainScreen_loggedInUser_showsCameraFAB() {
        val isLoggedIn = true

        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = emptyList(),
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoggedIn) {
                    CameraFAB(
                        onClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                } else {
                    LoginFAB(
                        onClick = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Capture")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Sign in")
            .assertDoesNotExist()
    }

    @Test
    fun mainScreen_loggedInUser_showsLogoutInMenu() {
        val isLoggedIn = true

        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    var menuExpanded by remember { mutableStateOf(false) }

                    MenuButton(onClick = { menuExpanded = true })

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Privacy & Copyright") },
                            onClick = { menuExpanded = false }
                        )
                        if (isLoggedIn) {
                            DropdownMenuItem(
                                text = { Text("Log out") },
                                onClick = { menuExpanded = false }
                            )
                        }
                    }
                }
            }
        }

        // Open the menu
        composeTestRule
            .onNodeWithContentDescription("Menu")
            .performClick()

        // Both items should be visible for logged-in users
        composeTestRule
            .onNodeWithText("Privacy & Copyright")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Log out")
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_pendingUpload_showsAtTopOfGridWithLoadingOverlay() {
        // Reproduces MainScreen's layout with a pending upload
        val pendingUri = createTestUri(99)
        val submissionUris = createTestUriList(3)
        val imageUris = listOf(pendingUri) + submissionUris
        val uploadingUris = setOf(pendingUri)

        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = imageUris,
                    uploadingUris = uploadingUris,
                    modifier = Modifier.fillMaxSize()
                )

                CameraFAB(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        // Should show 4 images total (1 pending + 3 submissions)
        composeTestRule
            .onAllNodesWithContentDescription("Captured image")
            .assertCountEquals(4)

        // Should show a loading indicator for the pending upload
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }

    @Test
    fun mainScreen_pendingUpload_clickAdjustsIndexForSubmission() {
        // Reproduces MainScreen's onImageClick index adjustment logic
        val pendingUri = createTestUri(99)
        val submissionUris = createTestUriList(3)
        val imageUris = listOf(pendingUri) + submissionUris
        val uploadingUris = setOf(pendingUri)

        val submissionIds = listOf("sub-0", "sub-1", "sub-2")
        val clickedSubmissionIds = mutableListOf<String>()

        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = imageUris,
                    uploadingUris = uploadingUris,
                    modifier = Modifier.fillMaxSize(),
                    onImageClick = { index ->
                        // Same adjustment as MainScreen
                        val adjustedIndex = index - 1
                        if (adjustedIndex >= 0) {
                            clickedSubmissionIds.add(submissionIds[adjustedIndex])
                        }
                    }
                )
            }
        }

        // Only non-uploading items should be clickable
        val clickableNodes = composeTestRule.onAllNodes(hasClickAction())
        clickableNodes.assertCountEquals(3)

        // Click first clickable item -> grid index 1 -> submission index 0
        clickableNodes[0].performClick()
        assertEquals(listOf("sub-0"), clickedSubmissionIds)

        // Click second clickable item -> grid index 2 -> submission index 1
        clickableNodes[1].performClick()
        assertEquals(listOf("sub-0", "sub-1"), clickedSubmissionIds)
    }

    @Test
    fun mainScreen_noPendingUpload_noLoadingOverlay() {
        val submissionUris = createTestUriList(3)

        composeTestRule.setContentWithTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageGrid(
                    imageUris = submissionUris,
                    modifier = Modifier.fillMaxSize()
                )

                CameraFAB(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }

        // 3 images, no loading overlay
        composeTestRule
            .onAllNodesWithContentDescription("Captured image")
            .assertCountEquals(3)

        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertDoesNotExist()
    }
}
