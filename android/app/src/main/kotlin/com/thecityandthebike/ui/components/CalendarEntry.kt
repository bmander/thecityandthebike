package com.thecityandthebike.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

data class CalendarPhoto(
    val submissionId: String,
    val imageUri: Uri
)

@Composable
fun CalendarEntry(
    dateLabel: String,
    yearLabel: String?,
    photos: List<CalendarPhoto>,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .width(56.dp)
                .padding(top = 4.dp)
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (yearLabel != null) {
                Text(
                    text = yearLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            photos.chunked(3).forEach { rowPhotos ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    rowPhotos.forEach { photo ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(onClickLabel = "View photo") {
                                    onImageClick(photo.submissionId)
                                }
                        ) {
                            AsyncImage(
                                model = photo.imageUri,
                                contentDescription = "Captured image",
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    // Fill remaining space in incomplete rows
                    repeat(3 - rowPhotos.size) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
