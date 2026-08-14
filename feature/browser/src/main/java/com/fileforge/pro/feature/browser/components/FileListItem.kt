package com.fileforge.pro.feature.browser.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * List item — file/folder in a vertical row with name + subtitle.
 * Used in LIST and COMPACT_LIST view modes (Master Spec §10).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: FFile,
    isSelected: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = if (isCompact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (file.isDirectory) {
            FolderIcon(sizeDp = if (isCompact) 28 else 36)
        } else {
            FileTypeIcon(
                fileType = file.fileType,
                size = if (isCompact) 28 else 36,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 0.dp else 2.dp),
        ) {
            Text(
                text = file.name,
                style = if (isCompact) MaterialTheme.typography.bodySmall
                else MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            if (!isCompact) {
                val subtitle = if (file.isDirectory) {
                    file.itemCount?.let { FormatUtils.formatItemCount(it) } ?: "—"
                } else {
                    FormatUtils.formatBytes(file.size) + "  ·  " +
                            FormatUtils.formatDate(file.lastModified)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
