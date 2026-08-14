package com.fileforge.pro.feature.analyzer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.fileforge.pro.core.ui.components.FileTypeIcon
import com.fileforge.pro.core.ui.components.FolderIcon
import com.fileforge.pro.engine.storageanalyzer.LargeFilesFinder

@Composable
fun LargeFilesScreen(
    viewModel: LargeFilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Large Files",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))

        // Threshold chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                FilterChip(
                    selected = state.minSize == LargeFilesFinder.THRESHOLD_100MB,
                    onClick = { viewModel.setThreshold(LargeFilesFinder.THRESHOLD_100MB) },
                    label = { Text(">100 MB") },
                )
            }
            item {
                FilterChip(
                    selected = state.minSize == LargeFilesFinder.THRESHOLD_500MB,
                    onClick = { viewModel.setThreshold(LargeFilesFinder.THRESHOLD_500MB) },
                    label = { Text(">500 MB") },
                )
            }
            item {
                FilterChip(
                    selected = state.minSize == LargeFilesFinder.THRESHOLD_1GB,
                    onClick = { viewModel.setThreshold(LargeFilesFinder.THRESHOLD_1GB) },
                    label = { Text(">1 GB") },
                )
            }
            item {
                FilterChip(
                    selected = state.minSize == LargeFilesFinder.THRESHOLD_5GB,
                    onClick = { viewModel.setThreshold(LargeFilesFinder.THRESHOLD_5GB) },
                    label = { Text(">5 GB") },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.isLoading && state.files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (state.files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No files above threshold", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(state.files, key = { it.path.displayPath }) { file ->
                LargeFileRow(file)
            }
        }
    }
}

@Composable
private fun LargeFileRow(file: com.fileforge.pro.domain.model.FFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { /* TODO: open */ }
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
        Text(
            text = FormatUtils.formatBytes(file.size),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
