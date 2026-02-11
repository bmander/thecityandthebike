package com.thecityandthebike.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val MAX_ID_LENGTH = 16

private fun abbreviateId(id: String): String {
    if (id.length <= MAX_ID_LENGTH) return id
    val keep = (MAX_ID_LENGTH - 1) / 2
    return id.take(keep) + "\u2026" + id.takeLast(keep)
}

@Composable
fun BikeIdBadge(provider: String?, bikeQrId: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        if (provider != null) {
            Text(
                text = provider.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(start = 7.dp, top = 2.dp, bottom = 2.dp, end = 3.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 3.dp, vertical = 0.dp)
            )
        }
        Text(
            text = abbreviateId(bikeQrId),
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start=2.dp, end = 7.dp, bottom = 2.dp, top=2.dp)
        )
    }
}
