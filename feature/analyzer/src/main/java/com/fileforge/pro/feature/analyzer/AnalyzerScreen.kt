package com.fileforge.pro.feature.analyzer

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.domain.model.FileType

/**
 * Storage Analyzer screen (Master Spec §34).
 *
 * Shows a horizontal stacked bar chart of file types + breakdown list +
 * scan progress.
 */
@Composable
fun AnalyzerScreen(
    viewModel: AnalyzerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Storage Analyzer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(8.dp))

        if (state.isAnalyzing && state.result == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val result = state.result
        if (result == null) {
            Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        // Total card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total Used", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = FormatUtils.formatBytes(result.totalSize),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${result.fileCount} files · ${result.folderCount} folders · scanned ${result.scannedPaths}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Stacked bar chart
        StackedBarChart(
            byFileType = result.byFileType,
            totalSize = result.totalSize,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        )

        Spacer(Modifier.height(16.dp))

        // Breakdown list
        Text("Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(result.byFileType.entries.sortedByDescending { it.value }.toList()) { (type, size) ->
                BreakdownRow(
                    fileType = type,
                    size = size,
                    fraction = if (result.totalSize == 0L) 0f else size.toFloat() / result.totalSize,
                )
            }
        }

        if (state.isAnalyzing) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                "Scanning… ${result.scannedPaths} items",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StackedBarChart(
    byFileType: Map<FileType, Long>,
    totalSize: Long,
    modifier: Modifier = Modifier,
) {
    if (totalSize == 0L) {
        Box(
            modifier = modifier.clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        byFileType.entries.sortedByDescending { it.value }.forEach { (type, size) ->
            val fraction = size.toFloat() / totalSize
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .background(colorFor(type)),
            )
        }
    }
}

@Composable
private fun BreakdownRow(fileType: FileType, size: Long, fraction: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colorFor(fileType)),
        )
        Text(
            text = fileType.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(fraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = FormatUtils.formatBytes(size),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun colorFor(type: FileType): Color = when (type) {
    FileType.IMAGE -> Color(0xFF66BB6A)
    FileType.VIDEO -> Color(0xFFEF5350)
    FileType.AUDIO -> Color(0xFFAB47BC)
    FileType.TEXT -> Color(0xFF42A5F5)
    FileType.ARCHIVE -> Color(0xFF8D6E63)
    FileType.APK -> Color(0xFF26A69A)
    FileType.PDF -> Color(0xFFFF7043)
    FileType.FOLDER -> Color(0xFFFFB300)
    FileType.OTHER -> Color(0xFF607D8B)
}
