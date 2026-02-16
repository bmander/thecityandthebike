package com.thecityandthebike.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thecityandthebike.ui.theme.ExtendedTheme
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme

@Composable
fun OwnerActions(
    isDeleting: Boolean,
    onDownload: () -> Unit,
    onShowDeleteDialog: () -> Unit,
) {
    if (isDeleting) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    } else {
        Row {
            OutlinedIconButton(
                onClick = onDownload,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download photo"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onShowDeleteDialog,
                shape = RoundedCornerShape(8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ExtendedTheme.colorScheme.destructiveAction,
                    contentColor = ExtendedTheme.colorScheme.onDestructiveAction
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete photo"
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OwnerActionsPreview() {
    TheCityAndTheBikeTheme(dynamicColor = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            OwnerActions(
                isDeleting = false,
                onDownload = {},
                onShowDeleteDialog = {}
            )
        }
    }
}
