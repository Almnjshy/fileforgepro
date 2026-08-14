package com.fileforge.pro.feature.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.core.ui.components.FileTypeIcon
import com.fileforge.pro.core.ui.components.FolderIcon
import com.fileforge.pro.domain.model.FFile

/**
 * Search screen (Master Spec §33).
 *
 * Layout:
 *   ┌─────────────────────────────────────┐
 *   │ Search bar with hint                │
 *   │ Suggestions chips (when empty)       │
 *   │ History chips (when empty)           │
 *   ├─────────────────────────────────────┤
 *   │ Status: "Found N · Scanned M · 1.2s" │
 *   │ Results list (streaming)             │
 *   └─────────────────────────────────────┘
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search bar
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search… e.g. *.pdf, name:project, size>500MB") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (state.isSearching) {
                    IconButton(onClick = viewModel::cancelSearch) {
                        Icon(Icons.Outlined.Cancel, contentDescription = "Cancel")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() }),
        )

        // Suggestions / history (when no results yet)
        if (state.results.isEmpty() && !state.isSearching) {
            if (state.history.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Recent searches", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = viewModel::clearHistory) { Text("Clear") }
                }
                ChipRow(
                    items = state.history,
                    onClick = viewModel::useSuggestion,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Suggestions", style = MaterialTheme.typography.labelMedium)
            ChipRow(
                items = state.suggestions,
                onClick = viewModel::useSuggestion,
            )
            return@Column
        }

        // Status bar
        if (state.isSearching || state.isComplete) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                }
                Text(
                    text = "Found ${state.results.size}" +
                            " · Scanned ${state.scannedPaths}" +
                            " · ${state.elapsedMs / 1000.0}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Results
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(state.results, key = { it.path.displayPath }) { file ->
                SearchResultRow(file)
            }
        }
    }
}

@Composable
private fun ChipRow(items: List<String>, onClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(items) { item ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onClick(item) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun SearchResultRow(file: FFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { /* TODO: open file */ }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (file.isDirectory) {
            FolderIcon(sizeDp = 32)
        } else {
            FileTypeIcon(fileType = file.fileType, size = 32)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = file.path.displayPath,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!file.isDirectory) {
            Text(
                text = FormatUtils.formatBytes(file.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
