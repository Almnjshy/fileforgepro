package com.fileforge.pro.feature.archive

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.engine.archive.ArchiveEntry

/**
 * Archive viewer / extractor screen (Master Spec §49 — Archive Manager).
 *
 * Shows:
 *   - Archive info (total size, entry count)
 *   - Streaming list of entries
 *   - Extract button (extracts to current browser directory)
 */
@Composable
fun ArchiveScreen(
    file: FFile,
    viewModel: ArchiveViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(file.path.displayPath) { viewModel.load(file) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Archive, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "Archive size: ${FormatUtils.formatBytes(file.size)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                state.listing?.let { listing ->
                    Text(
                        text = "${listing.entryCount} entries · uncompressed ${FormatUtils.formatBytes(listing.totalUncompressedSize)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Extract button
        Button(
            onClick = {
                // Extract to archive's parent directory
                file.path.parent?.let { parent ->
                    viewModel.extract(parent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isExtracting && state.listing?.entries?.isNotEmpty() == true,
        ) {
            if (state.isExtracting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp).height(16.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Outlined.Folder, contentDescription = null)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (state.isExtracting) "Extracting…" else "Extract to current folder")
        }

        if (state.extractedCount > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Extracted ${state.extractedCount} files",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }

        state.errorMessage?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Entry list
        if (state.isLoading && state.listing == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val entries = state.listing?.entries ?: emptyList()
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (state.listing?.isComplete == true) "Archive is empty or unsupported"
                    else "Loading…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(entries, key = { it.path }) { entry ->
                ArchiveEntryRow(entry)
            }
        }
    }
}

@Composable
private fun ArchiveEntryRow(entry: ArchiveEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { /* TODO: extract single file */ }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!entry.isDirectory && entry.size >= 0) {
            Text(
                text = FormatUtils.formatBytes(entry.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
