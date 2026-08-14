package com.fileforge.pro.feature.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.domain.model.WindowSpec
import com.fileforge.pro.domain.model.WindowType

/**
 * Window task switcher bar — shows all open windows as chips, allows quick
 * switching, focusing, and closing (Master Spec §25).
 *
 * Displayed at the bottom of the screen when there are 2+ open windows.
 */
@Composable
fun WindowTaskSwitcher(
    modifier: Modifier = Modifier,
    viewModel: WindowViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (state.windows.size < 2) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(state.windows, key = { it.id }) { window ->
                WindowChip(
                    window = window,
                    isFocused = window.id == state.focusedId,
                    onClick = { viewModel.focus(window.id) },
                    onClose = { viewModel.close(window.id) },
                )
            }
        }
    }
}

@Composable
private fun WindowChip(
    window: WindowSpec,
    isFocused: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = iconFor(window.type),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isFocused) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = window.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal,
            color = if (isFocused) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
        )
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Close",
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClose() }
                .padding(2.dp),
            tint = if (isFocused) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun iconFor(type: WindowType): ImageVector = when (type) {
    WindowType.FILE_BROWSER, WindowType.FOLDER -> Icons.Outlined.Folder
    WindowType.TEXT_EDITOR -> Icons.Outlined.Article
    WindowType.IMAGE_VIEWER -> Icons.Outlined.Image
    WindowType.PDF_VIEWER -> Icons.Outlined.PictureAsPdf
    WindowType.ARCHIVE -> Icons.Outlined.Archive
    WindowType.PROPERTIES -> Icons.Outlined.Article
}
