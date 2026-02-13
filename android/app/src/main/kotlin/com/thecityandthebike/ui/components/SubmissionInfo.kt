package com.thecityandthebike.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme

@Composable
fun SubmissionInfo(
    submission: SubmissionResponse,
    formattedDate: String?,
    onUserClick: ((String) -> Unit)?,
    onBikeClick: ((String) -> Unit)?,
) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        submission.username?.let { username ->
            InfoRow(
                icon = Icons.Default.Person,
                iconDescription = "User",
                onClick = onUserClick?.let { { it(submission.userId) } }
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        InfoRow(
            icon = Icons.AutoMirrored.Filled.DirectionsBike,
            iconDescription = "Bike",
            onClick = onBikeClick?.let { { it(submission.bikeQrId) } }
        ) {
            BikeIdBadge(
                provider = submission.provider,
                bikeQrId = submission.bikeQrId
            )
        }

        formattedDate?.let { date ->
            InfoRow(
                icon = Icons.Default.CalendarToday,
                iconDescription = "Date"
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SubmissionInfoPreview() {
    TheCityAndTheBikeTheme(dynamicColor = false) {
        SubmissionInfo(
            submission = SubmissionResponse(
                submissionId = "preview-1",
                userId = "user-1",
                bikeQrId = "BIKE-42",
                capturedDate = "2026-02-11",
                username = "bmander",
                provider = "citibike"
            ),
            formattedDate = "Feb 11, 2026",
            onUserClick = {},
            onBikeClick = {}
        )
    }
}
