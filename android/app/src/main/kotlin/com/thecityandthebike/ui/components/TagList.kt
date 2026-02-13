package com.thecityandthebike.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thecityandthebike.data.model.dto.TagResponse
import com.thecityandthebike.ui.theme.TheCityAndTheBikeTheme
import com.thecityandthebike.util.imageUrlToUri

@Composable
fun TagList(
    tags: List<TagResponse>,
    isTagOwner: (TagResponse) -> Boolean,
    onDeleteTag: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            TagItem(
                tag = tag,
                isDeletable = isTagOwner(tag),
                onDelete = { onDeleteTag(tag.tagId) }
            )
        }
    }
}

@Composable
fun TagItem(
    tag: TagResponse,
    isDeletable: Boolean,
    onDelete: () -> Unit,
) {
    Box {
        AsyncImage(
            model = imageUrlToUri(tag.imageUrl),
            contentDescription = "Tag",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        if (isDeletable) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete tag",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TagListPreview() {
    val tags = listOf(
        TagResponse(
            tagId = "tag-1",
            submissionId = "sub-1",
            userId = "user-1",
            imageUrl = "/uploads/images/tag1.png",
            createdAt = "2026-01-01T00:00:00Z"
        ),
        TagResponse(
            tagId = "tag-2",
            submissionId = "sub-1",
            userId = "user-2",
            imageUrl = "/uploads/images/tag2.png",
            createdAt = "2026-01-02T00:00:00Z"
        )
    )
    TheCityAndTheBikeTheme(dynamicColor = false) {
        TagList(
            tags = tags,
            isTagOwner = { it.tagId == "tag-1" },
            onDeleteTag = {}
        )
    }
}
