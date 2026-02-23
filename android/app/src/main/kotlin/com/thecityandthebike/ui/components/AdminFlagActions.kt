package com.thecityandthebike.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminFlagActions(
    flagCount: Int,
    isClearingFlags: Boolean,
    onClearFlags: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$flagCount ${if (flagCount == 1) "flag" else "flags"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        OutlinedButton(
            onClick = onClearFlags,
            enabled = !isClearingFlags,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        ) {
            if (isClearingFlags) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Clear flags", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
