package com.fileforge.pro.feature.browser.operations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.fileforge.pro.domain.model.FileOperation
import com.fileforge.pro.domain.model.FileOperationKind
import com.fileforge.pro.domain.model.OperationState

/**
 * Operations panel — shown at the bottom of the browser when there are active
 * file operations (Master Spec §27 — Progress UI).
 */
@Composable
fun OperationsPanel(
    viewModel: OperationsPanelViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    AnimatedVisibility(
        visible = state.isPanelVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "File Operations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                LazyColumn(
                    modifier = Modifier.height(maxOf(state.operations.size * 80, 80).dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.operations, key = { it.id }) { op ->
                        OperationRow(op = op, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationRow(
    op: FileOperation,
    viewModel: OperationsPanelViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = operationTitle(op),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            if (op.state == OperationState.RUNNING && op.totalBytes > 0) {
                LinearProgressIndicator(
                    progress = { op.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${FormatUtils.formatBytes(op.bytesTransferred)} / ${FormatUtils.formatBytes(op.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${(op.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Text(
                    text = stateLabel(op),
                    style = MaterialTheme.typography.labelSmall,
                    color = stateColor(op),
                )
            }
            op.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Action buttons
        when (op.state) {
            OperationState.RUNNING -> {
                IconButton(onClick = { viewModel.pause(op.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.cancel(op.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Stop, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                }
            }
            OperationState.PAUSED -> {
                IconButton(onClick = { viewModel.resume(op.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.cancel(op.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Stop, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                }
            }
            OperationState.FAILED -> {
                IconButton(onClick = { viewModel.retry(op.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Retry", modifier = Modifier.size(18.dp))
                }
            }
            else -> {}
        }
    }
}

private fun operationTitle(op: FileOperation): String {
    val action = when (op.kind) {
        FileOperationKind.COPY -> "Copying"
        FileOperationKind.MOVE -> "Moving"
        FileOperationKind.DELETE -> "Deleting"
        FileOperationKind.RENAME -> "Renaming"
        FileOperationKind.CREATE_FOLDER -> "Creating folder"
        FileOperationKind.CREATE_FILE -> "Creating file"
        FileOperationKind.COMPRESS -> "Compressing"
        FileOperationKind.EXTRACT -> "Extracting"
    }
    val target = op.sources.firstOrNull()?.name ?: ""
    return if (op.sources.size > 1) {
        "$action ${op.sources.size} items"
    } else {
        "$action $target"
    }
}

@Composable
private fun stateLabel(op: FileOperation): String = when (op.state) {
    OperationState.QUEUED -> "Queued"
    OperationState.RUNNING -> "In progress"
    OperationState.PAUSED -> "Paused"
    OperationState.COMPLETED -> "Completed"
    OperationState.FAILED -> "Failed"
    OperationState.CANCELLED -> "Cancelled"
    OperationState.AWAITING_CONFLICT -> "Awaiting conflict resolution"
}

@Composable
private fun stateColor(op: FileOperation) = when (op.state) {
    OperationState.COMPLETED -> MaterialTheme.colorScheme.primary
    OperationState.FAILED, OperationState.CANCELLED -> MaterialTheme.colorScheme.error
    OperationState.AWAITING_CONFLICT -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
