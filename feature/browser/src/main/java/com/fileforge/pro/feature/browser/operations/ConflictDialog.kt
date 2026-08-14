package com.fileforge.pro.feature.browser.operations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileforge.pro.domain.model.ConflictResolution
import com.fileforge.pro.domain.model.FileConflict

/**
 * Conflict resolution dialog (Master Spec §28 — File Conflict Manager).
 *
 * Shown when a copy/move target already exists. Offers:
 *   Replace / Keep Both / Skip / Rename + Apply to all
 */
@Composable
fun ConflictDialog(
    conflict: FileConflict,
    onResolve: (ConflictResolution) -> Unit,
    onDismiss: () -> Unit,
) {
    var applyToAll by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File already exists") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = conflict.destination.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "A file with this name already exists in this location. What would you like to do?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it },
                    )
                    Text("Apply to all", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onResolve(ConflictResolution.REPLACE) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Replace") }
                OutlinedButton(
                    onClick = { onResolve(ConflictResolution.KEEP_BOTH) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Keep Both") }
                OutlinedButton(
                    onClick = { onResolve(ConflictResolution.SKIP) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip") }
                TextButton(
                    onClick = { onResolve(ConflictResolution.RENAME) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Rename") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
