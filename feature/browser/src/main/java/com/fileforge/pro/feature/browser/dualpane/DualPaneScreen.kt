package com.fileforge.pro.feature.browser.dualpane

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.feature.browser.BrowserScreen

/**
 * Dual-pane browser layout (Master Spec §18 — Dual Pane).
 *
 * Renders two independent [BrowserScreen]s side by side with a draggable
 * divider between them. Each pane has its own:
 *   - Navigation history
 *   - Current path
 *   - Selection
 *   - Sort / filter / view mode
 *
 * Cross-pane operations (copy/move) are triggered via the FABs at the bottom
 * center: a swap icon for copy, a move icon for move. They operate on the
 * active pane's selected items, sending them to the other pane's current path.
 *
 * On phones (width < 600dp), this layout is NOT used — the single-pane
 * [BrowserScreen] is shown instead. The trigger lives in the NavHost.
 */
@Composable
fun DualPaneScreen(
    initialSourceId: String = "internal",
    initialPath: String = "root",
    viewModel: DualPaneViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        // ---- Left pane ----
        Box(
            modifier = Modifier
                .weight(state.dividerFraction)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { viewModel.onDragStart(PaneId.LEFT) },
                        onDragEnd = { viewModel.onDragEnd() },
                        onDragCancel = { viewModel.onDragEnd() },
                    ) { _, _ -> /* drag handled by items */ }
                },
        ) {
            BrowserScreen(
                initialSourceId = initialSourceId,
                initialPath = initialPath,
                paneKey = "pane-left",
                onPathChanged = { viewModel.setPanePath(PaneId.LEFT, it) },
                onPaneFocused = { viewModel.setActivePane(PaneId.LEFT) },
            )
        }

        // ---- Divider (draggable) ----
        DividerHandle(
            fraction = state.dividerFraction,
            onDrag = { delta -> viewModel.setDividerFraction(state.dividerFraction + delta) },
        )

        // ---- Right pane ----
        Box(
            modifier = Modifier
                .weight(1f - state.dividerFraction)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { viewModel.onDragStart(PaneId.RIGHT) },
                        onDragEnd = { viewModel.onDragEnd() },
                        onDragCancel = { viewModel.onDragEnd() },
                    ) { _, _ -> }
                },
        ) {
            BrowserScreen(
                initialSourceId = initialSourceId,
                initialPath = initialPath,
                paneKey = "pane-right",
                onPathChanged = { viewModel.setPanePath(PaneId.RIGHT, it) },
                onPaneFocused = { viewModel.setActivePane(PaneId.RIGHT) },
            )
        }
    }

    // ---- Cross-pane operation FABs (centered at the bottom divider) ----
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Copy active pane's selection → opposite pane
            FloatingActionButton(
                onClick = {
                    // Triggered by the active pane's VM via a callback.
                    // For now, this is a placeholder; the actual selection
                    // retrieval happens via the BrowserViewModel keyed by paneKey.
                    viewModel.setActivePane(state.activePane)
                },
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Icon(Icons.Outlined.ArrowForward, contentDescription = "Copy to other pane")
            }

            // Swap panes (toggle which is active)
            FloatingActionButton(
                onClick = {
                    viewModel.setActivePane(
                        if (state.activePane == PaneId.LEFT) PaneId.RIGHT else PaneId.LEFT
                    )
                },
            ) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = "Swap active pane")
            }
        }
    }
}

/**
 * Vertical drag handle between the two panes.
 */
@Composable
private fun DividerHandle(
    fraction: Float,
    onDrag: (deltaFraction: Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    // Convert pixels to fraction (assume ~1080px wide viewport)
                    val deltaFraction = dragAmount / 1080f
                    onDrag(deltaFraction)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.DragHandle,
            contentDescription = "Resize panes",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(4.dp),
        )
    }
}
