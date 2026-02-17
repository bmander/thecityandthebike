package com.thecityandthebike.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TagOutlineOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // A tag with a square polygon covering most of a 100x100 area
    private val testTag = TagResponse(
        tagId = "tag-1",
        submissionId = "sub-1",
        userId = "user-1",
        imageUrl = "https://example.com/tag.jpg",
        ring = listOf(
            listOf(10f, 10f),
            listOf(90f, 10f),
            listOf(90f, 90f),
            listOf(10f, 90f)
        ),
        ringWidth = 100,
        ringHeight = 100,
        createdAt = "2024-01-01T00:00:00Z"
    )

    @Test
    fun tapInsideTag_callsOnTagTappedWithTagId() {
        val tappedIds = mutableListOf<String?>()

        composeTestRule.setContentWithTheme {
            TagOutlineOverlay(
                tags = listOf(testTag),
                selectedTagId = null,
                onTagTapped = { tappedIds.add(it) },
                modifier = Modifier.size(200.dp)
            )
        }

        composeTestRule.onNodeWithTag("tagOutlineOverlay")
            .performTouchInput { click(center) }

        composeTestRule.waitForIdle()
        assertEquals(listOf("tag-1"), tappedIds)
    }

    @Test
    fun tapInsideSelectedTag_callsOnTagTappedWithNull() {
        val tappedIds = mutableListOf<String?>()

        composeTestRule.setContentWithTheme {
            TagOutlineOverlay(
                tags = listOf(testTag),
                selectedTagId = "tag-1",
                onTagTapped = { tappedIds.add(it) },
                modifier = Modifier.size(200.dp)
            )
        }

        composeTestRule.onNodeWithTag("tagOutlineOverlay")
            .performTouchInput { click(center) }

        composeTestRule.waitForIdle()
        assertEquals(listOf<String?>(null), tappedIds)
    }

    @Test
    fun tapOutsideTag_callsOnTagTappedWithNull() {
        val tappedIds = mutableListOf<String?>()

        composeTestRule.setContentWithTheme {
            TagOutlineOverlay(
                tags = listOf(testTag),
                selectedTagId = null,
                onTagTapped = { tappedIds.add(it) },
                modifier = Modifier.size(200.dp)
            )
        }

        // Tap in the top-left corner, outside the tag polygon
        composeTestRule.onNodeWithTag("tagOutlineOverlay")
            .performTouchInput { click(Offset(1f, 1f)) }

        composeTestRule.waitForIdle()
        assertEquals(listOf<String?>(null), tappedIds)
    }

    @Test
    fun selectedTagIdChange_isReflectedInTapHandler() {
        // This test verifies the fix for issue #200: after selectedTagId changes
        // externally (e.g. via navigation back from tag screen), the tap handler
        // must use the updated value for its toggle logic.
        val tappedIds = mutableListOf<String?>()
        var selectedTagId by mutableStateOf<String?>(null)

        composeTestRule.setContentWithTheme {
            TagOutlineOverlay(
                tags = listOf(testTag),
                selectedTagId = selectedTagId,
                onTagTapped = { tappedIds.add(it) },
                modifier = Modifier.size(200.dp)
            )
        }

        // Tap to select the tag
        composeTestRule.onNodeWithTag("tagOutlineOverlay")
            .performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertEquals("tag-1", tappedIds.last())

        // Simulate external state change (e.g. ViewModel state persisted after navigation)
        selectedTagId = "tag-1"
        composeTestRule.waitForIdle()

        // Tap the same tag again — should toggle OFF (call with null)
        composeTestRule.onNodeWithTag("tagOutlineOverlay")
            .performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertEquals(null, tappedIds.last())

        // Simulate external deselection
        selectedTagId = null
        composeTestRule.waitForIdle()

        // Tap again — should select (call with tag-1)
        composeTestRule.onNodeWithTag("tagOutlineOverlay")
            .performTouchInput { click(center) }
        composeTestRule.waitForIdle()
        assertEquals("tag-1", tappedIds.last())
    }
}
