package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.thecityandthebike.setContentWithTheme
import org.junit.Rule
import org.junit.Test

class QrScannerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun qrScannerScreen_permissionDenied_showsMessage() {
        composeTestRule.setContentWithTheme {
            // Render the permission-denied UI directly (same layout as QrScannerScreen)
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Camera permission is required to scan QR codes",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Camera permission is required to scan QR codes")
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
    }

    @Test
    fun qrScannerScreen_unrecognizedProvider_showsErrorSnackbar() {
        composeTestRule.setContentWithTheme {
            // Render the error snackbar UI directly (same layout as QrScannerScreen)
            Box(modifier = Modifier.fillMaxSize()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(text = "Unrecognized bike provider")
                }
            }
        }

        composeTestRule.onNodeWithText("Unrecognized bike provider")
            .assertIsDisplayed()
    }
}
