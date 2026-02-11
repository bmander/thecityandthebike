package com.thecityandthebike.ui.screens

import com.thecityandthebike.data.model.dto.SubmissionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BikeScreenDateGroupingTest {

    private fun submission(
        id: String = "sub-1",
        capturedDate: String? = "2024-01-15",
        imageUrl: String? = "https://example.com/img.jpg",
        imageUrlThumbnail: String? = null
    ) = SubmissionResponse(
        submissionId = id,
        userId = "user-1",
        bikeQrId = "bike-1",
        capturedDate = capturedDate,
        imageUrl = imageUrl,
        imageUrlThumbnail = imageUrlThumbnail
    )

    @Test
    fun `groups submissions by date`() {
        val submissions = listOf(
            submission(id = "1", capturedDate = "2024-01-15"),
            submission(id = "2", capturedDate = "2024-01-15"),
            submission(id = "3", capturedDate = "2024-02-20")
        )

        val groups = groupSubmissionsByDate(submissions)

        assertEquals(2, groups.size)
    }

    @Test
    fun `sorts groups reverse chronologically`() {
        val submissions = listOf(
            submission(id = "1", capturedDate = "2024-01-15"),
            submission(id = "2", capturedDate = "2024-06-20"),
            submission(id = "3", capturedDate = "2024-03-10")
        )

        val groups = groupSubmissionsByDate(submissions)

        assertEquals("Jun 20", groups[0].dateLabel)
        assertEquals("Mar 10", groups[1].dateLabel)
        assertEquals("Jan 15", groups[2].dateLabel)
    }

    @Test
    fun `formats date labels as MMM d`() {
        val submissions = listOf(
            submission(id = "1", capturedDate = "2024-01-15")
        )

        val groups = groupSubmissionsByDate(submissions)

        assertEquals("Jan 15", groups[0].dateLabel)
    }

    @Test
    fun `formats year labels as yyyy`() {
        val submissions = listOf(
            submission(id = "1", capturedDate = "2024-01-15")
        )

        val groups = groupSubmissionsByDate(submissions)

        assertEquals("2024", groups[0].yearLabel)
    }

    @Test
    fun `handles null capturedDate`() {
        val submissions = listOf(
            submission(id = "1", capturedDate = null)
        )

        val groups = groupSubmissionsByDate(submissions)

        assertEquals(1, groups.size)
        assertEquals("Unknown date", groups[0].dateLabel)
        assertNull(groups[0].yearLabel)
    }

    @Test
    fun `filters out groups with no photo URLs`() {
        val submissions = listOf(
            submission(id = "1", capturedDate = "2024-01-15", imageUrl = null, imageUrlThumbnail = null),
            submission(id = "2", capturedDate = "2024-02-20", imageUrl = "https://example.com/img.jpg")
        )

        val groups = groupSubmissionsByDate(submissions)

        assertEquals(1, groups.size)
        assertEquals("Feb 20", groups[0].dateLabel)
    }

    @Test
    fun `prefers thumbnail URL over full URL`() {
        val submissions = listOf(
            submission(
                id = "1",
                capturedDate = "2024-01-15",
                imageUrl = "https://example.com/full.jpg",
                imageUrlThumbnail = "https://example.com/thumb.jpg"
            )
        )

        val groups = groupSubmissionsByDate(submissions)

        assertEquals("https://example.com/thumb.jpg", groups[0].photos[0].imageUrl)
    }
}
