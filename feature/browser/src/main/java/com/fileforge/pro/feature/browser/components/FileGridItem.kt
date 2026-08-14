package com.fileforge.pro.feature.browser.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.core.ui.components.FolderIcon
import com.fileforge.pro.core.ui.components.FileTypeIcon
import com.fileforge.pro.domain.model.FFile

/**
 * Grid item — folder/file with name + optional size / item count.
 * Used in LARGE / MEDIUM / SMALL grid view modes (Master Spec §10).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    file: FFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (file.isDirectory) {
                FolderIcon(sizeDp = 48)
            } else {
                FileTypeIcon(
                    fileType = file.fileType,
                    size = 48,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = file.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )

        if (!file.isDirectory) {
            Text(
                text = FormatUtils.formatBytes(file.size),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        } else {
            val count = file.itemCount
            if (count != null) {
                Text(
                    text = FormatUtils.formatItemCount(count),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
