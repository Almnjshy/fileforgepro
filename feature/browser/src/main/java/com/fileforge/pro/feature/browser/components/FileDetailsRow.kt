package com.fileforge.pro.feature.browser.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.core.ui.components.FileTypeIcon
import com.fileforge.pro.core.ui.components.FolderIcon
import com.fileforge.pro.domain.model.FFile

/**
 * Details row — Windows-style row with columns:
 *   Name | Size | Type | Modified
 * (Master Spec §10 — Details view)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileDetailsRow(
    file: FFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Name column (icon + name)
        Row(
            modifier = Modifier.weight(2f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (file.isDirectory) {
                FolderIcon(sizeDp = 24)
            } else {
                FileTypeIcon(fileType = file.fileType, size = 24)
            }
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        }

        // Size column
        Text(
            text = if (file.isDirectory) "—" else FormatUtils.formatBytes(file.size),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )

        // Type column
        Text(
            text = if (file.isDirectory) "Folder"
            else file.extension?.uppercase() ?: "—",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )

        // Modified column
        Text(
            text = FormatUtils.formatDate(file.lastModified),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1.2f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
