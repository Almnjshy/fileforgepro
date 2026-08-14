package com.fileforge.pro.feature.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.engine.vault.VaultEntry

/**
 * Secure Vault screen (Master Spec §53 — Secure Vault).
 *
 * Two states:
 *   - LOCKED: shows password prompt
 *   - UNLOCKED: shows encrypted file entries + actions
 */
@Composable
fun VaultScreen(viewModel: VaultViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Secure Vault", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "AES-256-GCM encrypted · PBKDF2 600k iterations",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))

        if (!state.isUnlocked) {
            LockedView(state = state, onUnlock = viewModel::unlock)
        } else {
            UnlockedView(
                state = state,
                onLock = viewModel::lock,
                onExtract = viewModel::extractEntry,
                onDelete = viewModel::deleteEntry,
            )
        }
    }
}

@Composable
private fun LockedView(state: VaultUiState, onUnlock: (String) -> Unit) {
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (state.hasVault) "Enter password to unlock" else "Create a password to set up your vault",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        state.unlockError?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onUnlock(password) },
            enabled = password.isNotBlank() && !state.isUnlocking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isUnlocking) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.LockOpen, contentDescription = null)
            }
            Spacer(Modifier.size(8.dp))
            Text(if (state.isUnlocking) "Unlocking…" else "Unlock")
        }
    }
}

@Composable
private fun UnlockedView(
    state: VaultUiState,
    onLock: () -> Unit,
    onExtract: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${state.entries.size} encrypted files",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onLock) {
            Icon(Icons.Outlined.Lock, contentDescription = null)
            Spacer(Modifier.size(4.dp))
            Text("Lock")
        }
    }

    Spacer(Modifier.height(12.dp))

    if (state.entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No files in vault yet.\nUse the context menu on any file → Add to Vault",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.entries, key = { it.id }) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.originalName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${FormatUtils.formatBytes(entry.size)} · encrypted ${FormatUtils.formatBytes(entry.encryptedSize)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onExtract(entry) }) {
                        Icon(Icons.Outlined.Download, contentDescription = "Extract")
                    }
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
