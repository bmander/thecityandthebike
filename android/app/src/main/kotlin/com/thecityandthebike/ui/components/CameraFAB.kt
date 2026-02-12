package com.thecityandthebike.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun CameraFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "Capture" },
        containerColor = MaterialTheme.colorScheme.primary,
        icon = {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null
            )
        },
        text = { Text("Capture") }
    )
}
