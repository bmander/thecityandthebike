package com.thecityandthebike.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.setContentWithTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageDetailScreenTest {

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
    fun imageDetailScreen_displaysSubmissionInfo() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {}
            )
        }

        composeTestRule.onNodeWithText("testuser").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jan 15, 2025").assertIsDisplayed()
    }

    @Test
    fun imageDetailScreen_ownerSeesDeleteAndDownloadButtons() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isOwner = true
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete photo").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Download photo").assertIsDisplayed()
    }

    @Test
    fun imageDetailScreen_nonOwnerDoesNotSeeDeleteDownloadButtons() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isOwner = false
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete photo").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Download photo").assertDoesNotExist()
    }

    @Test
    fun imageDetailScreen_deleteDialogAppearsAndDismisses() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isOwner = true,
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Delete photo").performClick()

        composeTestRule.onNodeWithText("Delete Photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete this photo? This action cannot be undone.")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithText("Delete Photo").assertDoesNotExist()
    }

    @Test
    fun homeButton_isDisplayed() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Home")
            .assertIsDisplayed()
    }

    @Test
    fun homeButton_clickCallsOnHome() {
        var homeCalled = false

        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = { homeCalled = true }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Home")
            .performClick()

        assertTrue("onHome should be called when home button is clicked", homeCalled)
    }

    @Test
    fun homeButton_doesNotShowBackArrow() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Back")
            .assertDoesNotExist()
    }

    // --- Tag UI tests ---

    @Test
    fun addTagButton_visibleWhenLoggedIn() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true
            )
        }

        composeTestRule.onNodeWithContentDescription("Add tag").assertIsDisplayed()
    }

    @Test
    fun addTagButton_hiddenWhenNotLoggedIn() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = false
            )
        }

        composeTestRule.onNodeWithContentDescription("Add tag").assertDoesNotExist()
    }

    @Test
    fun addTagButton_clickCallsOnEnterTagMode() {
        var enterCalled = false

        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                onEnterTagMode = { enterCalled = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Add tag").performClick()

        assertTrue("onEnterTagMode should be called", enterCalled)
    }

    @Test
    fun tagMode_showsDiscardAndNextButtons() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                isTagMode = true
            )
        }

        composeTestRule.onNodeWithText("Discard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun tagMode_hidesAddTagButton() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                isTagMode = true
            )
        }

        composeTestRule.onNodeWithContentDescription("Add tag").assertDoesNotExist()
    }

    @Test
    fun tagMode_discardCallsOnExitTagMode() {
        var exitCalled = false

        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                isTagMode = true,
                onExitTagMode = { exitCalled = true }
            )
        }

        composeTestRule.onNodeWithText("Discard").performClick()

        assertTrue("onExitTagMode should be called", exitCalled)
    }

    @Test
    fun tagMode_nextDisabledWithNoStrokes() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                isTagMode = true
            )
        }

        composeTestRule.onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test
    fun tagOutlineOverlay_renderedWhenTagsHaveRingData() {
        val tags = listOf(
            TagResponse(
                tagId = "tag-1",
                submissionId = "sub-1",
                userId = "user-1",
                imageUrl = "/uploads/images/tag1.png",
                ring = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f)),
                ringWidth = 200,
                ringHeight = 200,
                createdAt = "2026-01-01T00:00:00Z"
            )
        )

        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                tags = tags
            )
        }

        composeTestRule.onNodeWithTag("tagOutlineOverlay").assertIsDisplayed()
    }

    // --- Confirm phase tests ---

    @Test
    fun confirmPhase_showsBackAndConfirmButtons() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                isTagMode = true,
                processedRing = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f)),
                processedMaskWidth = 200,
                processedMaskHeight = 200
            )
        }

        composeTestRule.onNodeWithText("Back").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm tag").assertIsDisplayed()
    }

    @Test
    fun confirmPhase_backCallsOnBackToDrawing() {
        var backCalled = false

        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                isTagMode = true,
                processedRing = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f)),
                processedMaskWidth = 200,
                processedMaskHeight = 200,
                onBackToDrawing = { backCalled = true }
            )
        }

        composeTestRule.onNodeWithText("Back").performClick()

        assertTrue("onBackToDrawing should be called", backCalled)
    }

    @Test
    fun confirmPhase_showsSpinnerWhenCreatingTag() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                isTagMode = true,
                isCreatingTag = true,
                processedRing = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f)),
                processedMaskWidth = 200,
                processedMaskHeight = 200
            )
        }

        composeTestRule.onNodeWithText("Confirm tag").assertDoesNotExist()
    }

    @Test
    fun processingPhase_showsSpinnerOnNextButton() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                isLoggedIn = true,
                isTagMode = true,
                isProcessingMask = true
            )
        }

        composeTestRule.onNodeWithText("Next").assertDoesNotExist()
    }

    // --- Tag selection and deletion tests ---

    private val testTags = listOf(
        TagResponse(
            tagId = "tag-1",
            submissionId = "sub-1",
            userId = "user-1",
            imageUrl = "/uploads/images/tag1.png",
            ring = listOf(listOf(0f, 0f), listOf(100f, 0f), listOf(100f, 100f)),
            ringWidth = 200,
            ringHeight = 200,
            createdAt = "2026-01-01T00:00:00Z"
        )
    )

    @Test
    fun selectedTag_showsDeleteButtonWhenDeletable() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                tags = testTags,
                selectedTagId = "tag-1",
                isTagDeletable = { true }
            )
        }

        composeTestRule.onNodeWithTag("deleteTagButton").assertIsDisplayed()
    }

    @Test
    fun selectedTag_hidesDeleteButtonWhenNotDeletable() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                tags = testTags,
                selectedTagId = "tag-1",
                isTagDeletable = { false }
            )
        }

        composeTestRule.onNodeWithTag("deleteTagButton").assertDoesNotExist()
    }

    @Test
    fun noSelectedTag_hidesDeleteButton() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                tags = testTags,
                selectedTagId = null,
                isTagDeletable = { true }
            )
        }

        composeTestRule.onNodeWithTag("deleteTagButton").assertDoesNotExist()
    }

    @Test
    fun deleteTagButton_showsConfirmDialog() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                tags = testTags,
                selectedTagId = "tag-1",
                isTagDeletable = { true }
            )
        }

        composeTestRule.onNodeWithTag("deleteTagButton").performClick()

        composeTestRule.onNodeWithText("Delete Tag").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete this tag?")
            .assertIsDisplayed()
    }

    @Test
    fun deleteTagDialog_cancelDismisses() {
        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                tags = testTags,
                selectedTagId = "tag-1",
                isTagDeletable = { true }
            )
        }

        composeTestRule.onNodeWithTag("deleteTagButton").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()

        composeTestRule.onNodeWithText("Delete Tag").assertDoesNotExist()
    }

    @Test
    fun deleteTagDialog_confirmCallsOnDeleteTag() {
        var deletedTagId: String? = null

        composeTestRule.setContentWithTheme {
            ImageDetailScreen(
                submission = testSubmission,
                onHome = {},
                tags = testTags,
                selectedTagId = "tag-1",
                isTagDeletable = { true },
                onDeleteTag = { deletedTagId = it }
            )
        }

        composeTestRule.onNodeWithTag("deleteTagButton").performClick()
        composeTestRule.onNodeWithText("Delete").performClick()

        assertTrue("onDeleteTag should be called with tag-1", deletedTagId == "tag-1")
    }
}
