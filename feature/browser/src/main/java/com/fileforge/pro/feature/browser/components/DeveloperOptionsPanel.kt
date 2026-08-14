package com.fileforge.pro.feature.browser.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileforge.pro.domain.model.FFile

/**
 * Developer Options panel — shown when Developer Mode is enabled in Settings
 * (Master Spec §54 — Developer Mode).
 *
 * Allows:
 *   - Show hidden files toggle
 *   - Show file permissions
 *   - Show MIME type
 *   - Show raw URI
 *   - Open in Terminal (Termux)
 *   - Git detection info
 */
@Composable
fun DeveloperOptionsPanel(
    file: FFile,
    realPath: String?,
    developerModeEnabled: Boolean,
    onShowHiddenChange: (Boolean) -> Unit,
    onOpenInTerminal: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!developerModeEnabled) return

    var showHidden by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Code, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Developer Options")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Toggle: show hidden files
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Folder, contentDescription = null,
                        modifier = Modifier.width(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Show hidden files", modifier = Modifier.weight(1f))
                    Switch(
                        checked = showHidden,
                        onCheckedChange = {
                            showHidden = it
                            onShowHiddenChange(it)
                        },
                    )
                }

                // Toggle: show file details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Description, contentDescription = null,
                        modifier = Modifier.width(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Show file details", modifier = Modifier.weight(1f))
                    Switch(
                        checked = showDetails,
                        onCheckedChange = { showDetails = it },
                    )
                }

                if (showDetails && realPath != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            DetailLine("Path", realPath)
                            DetailLine("MIME", file.mimeType ?: "—")
                            DetailLine("Type", file.fileType.name)
                            DetailLine("Extension", file.extension ?: "—")
                            DetailLine("Size (bytes)", file.size.toString())
                            DetailLine("Modified", file.lastModified.toString())
                            DetailLine("Readable", file.isReadable.toString())
                            DetailLine("Writable", file.isWritable.toString())
                            DetailLine("Executable", file.isExecutable.toString())
                            DetailLine("Hidden", file.isHidden.toString())
                            DetailLine("Raw URI", "file://$realPath")
                        }
                    }
                }

                // Open in Terminal
                if (realPath != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                            .let { it },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Terminal, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Open in Terminal", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onOpenInTerminal(realPath) }) {
                            Text("Open")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}
