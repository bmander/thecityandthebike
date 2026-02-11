package com.thecityandthebike.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thecityandthebike.data.model.dto.SubmissionResponse
import com.thecityandthebike.ui.components.BikeIdBadge
import com.thecityandthebike.ui.components.InfoRow
import com.thecityandthebike.ui.components.ZoomableImage
import com.thecityandthebike.util.imageUrlToUri
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(
    submission: SubmissionResponse,
    onBack: () -> Unit,
    onBikeClick: ((String) -> Unit)? = null,
    onUserClick: ((String) -> Unit)? = null
) {
    val imageUri = submission.imageUrlOriginal?.let { imageUrlToUri(it) }
    val thumbnailUri = submission.imageUrlThumbnail?.let { imageUrlToUri(it) }

    val formattedDate = submission.capturedDate?.let {
        try {
            val localDate = LocalDate.parse(it)
            DateTimeFormatter.ofPattern("MMM d, yyyy")
                .format(localDate)
        } catch (_: Exception) {
            it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            ZoomableImage(
                imageUri = imageUri,
                thumbnailUri = thumbnailUri,
                contentDescription = "Submission photo"
            )

            Spacer(modifier = Modifier.height(16.dp))

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
    }
}
