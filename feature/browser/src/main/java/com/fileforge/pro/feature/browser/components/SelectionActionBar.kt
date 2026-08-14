package com.fileforge.pro.feature.browser.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Selection action bar — appears at the top when multi-selection is active
 * (Master Spec §15 — Multi Selection).
 *
 * Shows: selection count + close | copy | move | delete | rename | share | select all | properties
 */
@Composable
fun SelectionActionBar(
    selectionCount: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSelectAll: () -> Unit,
    onProperties: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectionCount > 0,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close selection",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(
                text = "$selectionCount selected",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(end = 8.dp),
            )

            // Spacer
            androidx.compose.foundation.layout.Spacer(
                Modifier.weight(1f)
            )

            SelectionAction(Icons.Outlined.ContentCopy, "Copy", onCopy)
            SelectionAction(Icons.Outlined.DriveFileMove, "Move", onMove)
            SelectionAction(Icons.Outlined.Edit, "Rename", onRename, enabled = selectionCount == 1)
            SelectionAction(Icons.Outlined.IosShare, "Share", onShare)
            SelectionAction(Icons.Outlined.SelectAll, "Select all", onSelectAll)
            SelectionAction(Icons.Outlined.Info, "Properties", onProperties)
            SelectionAction(Icons.Outlined.Delete, "Delete", onDelete)
        }
    }
}

@Composable
private fun SelectionAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp)) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp),
        )
    }
}
