package com.fileforge.pro.feature.browser.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.fileforge.pro.domain.model.FFile

@Composable
fun FileContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    file: FFile?,
    onOpen: () -> Unit,
    onOpenWith: () -> Unit,
    onOpenInNewWindow: () -> Unit,
    onOpenInNewPane: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onShare: () -> Unit,
    onAddToFavorites: () -> Unit,
    onProperties: () -> Unit,
) {
    if (file == null) return
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        MenuEntry(Icons.Outlined.OpenInNew, "Open", onOpen)
        if (!file.isDirectory) {
            MenuEntry(Icons.Outlined.OpenInNew, "Open With", onOpenWith)
        }
        MenuEntry(Icons.Outlined.OpenInNew, "Open in New Window", onOpenInNewWindow)
        MenuEntry(Icons.Outlined.OpenInNew, "Open in New Pane", onOpenInNewPane)
        MenuEntry(Icons.Outlined.ContentCopy, "Copy", onCopy)
        MenuEntry(Icons.Outlined.DriveFileMove, "Move", onMove)
        MenuEntry(Icons.Outlined.Edit, "Rename", onRename)
        MenuEntry(Icons.Outlined.Delete, "Delete", onDelete)
        MenuEntry(Icons.Outlined.FolderZip, "Compress", onCompress)
        if (!file.isDirectory) {
            MenuEntry(Icons.Outlined.IosShare, "Share", onShare)
        }
        MenuEntry(Icons.Outlined.Star, "Add to Favorites", onAddToFavorites)
        MenuEntry(Icons.Outlined.Info, "Properties", onProperties)
    }
}

@Composable
private fun MenuEntry(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}

class ContextMenuState {
    var visible: Boolean by mutableStateOf(false)
        private set
    var targetFile: FFile? by mutableStateOf(null)
        private set

    fun show(file: FFile) {
        targetFile = file
        visible = true
    }

    fun dismiss() {
        visible = false
    }
}

@Composable
fun rememberContextMenuState(): ContextMenuState = androidx.compose.runtime.remember { ContextMenuState() }
