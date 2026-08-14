package com.fileforge.pro.feature.network

import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fileforge.pro.engine.network.ConnectionType

@Composable
fun NetworkScreen(
    onNavigate: (String) -> Unit,
    viewModel: NetworkViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Network Storage", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Connect to FTP, SFTP, SMB, or WebDAV servers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            if (state.connections.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Cloud, contentDescription = null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No connections yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tap + to add one", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.connections, key = { it.id }) { conn ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigate("browser/${conn.id}/root") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Outlined.Folder, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(conn.displayName, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text("${conn.type.name} · ${conn.host}:${conn.port}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.removeConnection(conn.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { viewModel.showAddDialog(true) },
            icon = { Icon(Icons.Outlined.Add, contentDescription = "Add") },
            text = { Text("Add connection") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )

        if (state.showAddDialog) {
            AddConnectionDialog(
                onAdd = { type, name, host, port, user, pass ->
                    viewModel.addConnection(type, name, host, port, user, pass)
                },
                onDismiss = { viewModel.showAddDialog(false) },
            )
        }
    }
}

@Composable
private fun AddConnectionDialog(
    onAdd: (ConnectionType, String, String, Int, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(ConnectionType.FTP) }
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("21") }
    var user by remember { mutableStateOf("anonymous") }
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Connection") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ConnectionType.entries.forEach { ct ->
                        FilterChip(
                            selected = type == ct,
                            onClick = {
                                type = ct
                                port = when (ct) {
                                    ConnectionType.FTP -> "21"
                                    ConnectionType.SFTP -> "22"
                                    ConnectionType.SMB -> "445"
                                    ConnectionType.WEBDAV -> "443"
                                }
                            },
                            label = { Text(ct.name) },
                        )
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Display name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = host, onValueChange = { host = it },
                    label = { Text("Host") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = user, onValueChange = { user = it },
                    label = { Text("Username") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pass, onValueChange = { pass = it },
                    label = { Text("Password") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                if (type != ConnectionType.FTP) {
                    Text(
                        "${type.name} support is coming in a future phase",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && host.isNotBlank()) {
                        onAdd(type, name, host, port.toIntOrNull() ?: 21, user, pass)
                    }
                },
                enabled = name.isNotBlank() && host.isNotBlank() && type == ConnectionType.FTP,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
