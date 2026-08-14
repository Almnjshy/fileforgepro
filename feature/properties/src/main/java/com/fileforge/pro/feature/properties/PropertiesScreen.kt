package com.fileforge.pro.feature.properties

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.core.ui.components.FileTypeIcon
import com.fileforge.pro.core.ui.components.FolderIcon
import com.fileforge.pro.domain.model.FFile

/**
 * File Properties screen (Master Spec §37).
 *
 * Shows: name, path, type, MIME, size, created, modified, accessed,
 * readable, writable, executable.
 */
@Composable
fun PropertiesScreen(
    file: FFile,
    viewModel: PropertiesViewModel = hiltViewModel(),
) {
    LaunchedEffect(file.path.displayPath) { viewModel.load(file) }

    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val f = state.file ?: file

        // Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (f.isDirectory) {
                    FolderIcon(sizeDp = 56)
                } else {
                    FileTypeIcon(fileType = f.fileType, size = 56)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = f.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = f.fileType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Properties list
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                PropertyRow("Name", f.name)
                PropertyRow("Path", f.path.displayPath)
                PropertyRow("Type", if (f.isDirectory) "Folder" else "File")
                PropertyRow("MIME", f.mimeType ?: "—")
                PropertyRow("Size", FormatUtils.formatBytes(f.size))
                PropertyRow("Extension", f.extension ?: "—")
                PropertyRow("Modified", FormatUtils.formatDate(f.lastModified))
                f.created?.let { PropertyRow("Created", FormatUtils.formatDate(it)) }
                f.lastAccessed?.let { PropertyRow("Accessed", FormatUtils.formatDate(it)) }
                PropertyRow("Readable", f.isReadable.toString())
                PropertyRow("Writable", f.isWritable.toString())
                PropertyRow("Executable", f.isExecutable.toString())
                PropertyRow("Hidden", f.isHidden.toString())
            }
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}
