package com.thecityandthebike

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme

/**
 * Creates a test URI for use in instrumented tests.
 */
fun createTestUri(index: Int = 0): Uri {
    return Uri.parse("content://com.thecityandthebike.test/image_$index")
}

/**
 * Creates a list of test URIs for use in instrumented tests.
 */
fun createTestUriList(count: Int): List<Uri> {
    return (0 until count).map { createTestUri(it) }
}

/**
 * Extension function to set content with MaterialTheme wrapper.
 */
fun ComposeContentTestRule.setContentWithTheme(content: @Composable () -> Unit) {
    setContent {
        TheCityAndTheBikeTheme {
            content()
        }
    }
}
