package com.thecityandthebike.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme

@Composable
fun TagModeControls(
    hasStrokes: Boolean,
    isCreatingTag: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(onClick = onDiscard) {
            Text("Discard")
        }
        Button(
            onClick = onSave,
            enabled = hasStrokes && !isCreatingTag
        ) {
            if (isCreatingTag) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Save tag")
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TagModeControlsPreview() {
    TheCityAndTheBikeTheme(dynamicColor = false) {
        TagModeControls(
            hasStrokes = true,
            isCreatingTag = false,
            onDiscard = {},
            onSave = {}
        )
    }
}
