package com.thecityandthebike.ui.components

import com.thecityandthebike.data.model.dto.SubmissionResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DateGroupPhoto(
    val submissionId: String,
    val imageUrl: String
)

data class DateGroup(
    val dateLabel: String,
    val yearLabel: String?,
    val photos: List<DateGroupPhoto>
)

fun groupSubmissionsByDate(submissions: List<SubmissionResponse>): List<DateGroup> {
    val dayFormat = DateTimeFormatter.ofPattern("MMM d")
    val yearFormat = DateTimeFormatter.ofPattern("yyyy")
    return submissions
        .groupBy { submission ->
            submission.capturedDate?.let { dateStr ->
                try {
                    LocalDate.parse(dateStr)
                } catch (_: Exception) {
                    null
                }
            }
        }
        .entries
        .sortedByDescending { it.key }
        .map { (date, submissions) ->
            val photos = submissions.mapNotNull { submission ->
                val url = submission.imageUrlThumbnail ?: submission.imageUrl
                    ?: return@mapNotNull null
                DateGroupPhoto(
                    submissionId = submission.submissionId,
                    imageUrl = url
                )
            }
            DateGroup(
                dateLabel = date?.format(dayFormat) ?: "Unknown date",
                yearLabel = date?.format(yearFormat),
                photos = photos
            )
        }
        .filter { it.photos.isNotEmpty() }
}
