package com.fileforge.pro.feature.browser.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileforge.pro.domain.model.ItemSize
import com.fileforge.pro.domain.model.ViewMode

/**
 * View-mode picker + item-size slider dialog (Master Spec §10, §11).
 *
 * Shown when user taps the view-mode icon in the toolbar. Lets the user:
 *   - Pick a view mode (Large Grid / Medium Grid / Small Grid / List / Compact List / Details / Thumbnail)
 *   - Adjust item size via a slider (Small ↔ Large)
 */
@Composable
fun ViewModePickerDialog(
    currentMode: ViewMode,
    currentItemSize: ItemSize,
    onModeSelected: (ViewMode) -> Unit,
    onItemSizeChanged: (ItemSize) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    var sizeFraction by remember(currentItemSize) { mutableFloatStateOf(currentItemSize.fraction) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("View Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "View Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                ViewModeGrid(
                    selectedMode = selectedMode,
                    onModeSelected = { mode ->
                        selectedMode = mode
                        onModeSelected(mode)
                    },
                )

                Text(
                    text = "Item Size",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.List, contentDescription = null)
                    Slider(
                        value = sizeFraction,
                        onValueChange = {
                            sizeFraction = it
                            onItemSizeChanged(ItemSize(it))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Outlined.GridView, contentDescription = null)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Small", style = MaterialTheme.typography.labelSmall)
                    Text("Medium", style = MaterialTheme.typography.labelSmall)
                    Text("Large", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun ViewModeGrid(
    selectedMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit,
) {
    val modes = listOf(
        ViewMode.LARGE_GRID to "Large Grid",
        ViewMode.MEDIUM_GRID to "Medium Grid",
        ViewMode.SMALL_GRID to "Small Grid",
        ViewMode.LIST to "List",
        ViewMode.COMPACT_LIST to "Compact",
        ViewMode.DETAILS to "Details",
        ViewMode.THUMBNAIL to "Thumbnails",
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        modes.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { (mode, label) ->
                    val isSelected = mode == selectedMode
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Fill remaining slots if row has < 3 items
                repeat(3 - row.size) {
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
