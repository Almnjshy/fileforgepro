package com.fileforge.pro.feature.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Minimize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fileforge.pro.domain.model.WindowPayload
import com.fileforge.pro.domain.model.WindowSpec
import com.fileforge.pro.domain.model.WindowState
import com.fileforge.pro.domain.model.WindowType

/**
 * Floating window host (Master Spec §19, §21–§25).
 *
 * Renders all open windows as floating Composables above the main content.
 * The [contentRenderer] lambda allows the caller (:app module) to provide
 * real feature screen content for each window type.
 *
 * Each window:
 *   - Has a title bar (drag handle — drag only works on title bar)
 *   - Has min/max/close buttons
 *   - Body is delegated to [contentRenderer]
 */
@Composable
fun WindowHost(
    viewModel: WindowViewModel = hiltViewModel(),
    contentRenderer: @Composable (WindowType, WindowPayload) -> Unit = { type, _ ->
        DefaultWindowContent(type)
    },
) {
    val state by viewModel.uiState.collectAsState()
    if (state.windows.isEmpty()) return

    Box(modifier = Modifier.fillMaxSize()) {
        state.windows
            .filter { it.state != WindowState.MINIMIZED }
            .forEach { window ->
                FloatingWindow(
                    spec = window,
                    isFocused = window.isFocused,
                    onFocus = { viewModel.focus(window.id) },
                    onClose = { viewModel.close(window.id) },
                    onMinimize = { viewModel.minimize(window.id) },
                    onMaximize = { viewModel.maximize(window.id) },
                    onMove = { dx, dy -> viewModel.move(window.id, dx, dy) },
                    contentRenderer = contentRenderer,
                )
            }
    }
}

@Composable
private fun FloatingWindow(
    spec: WindowSpec,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onMove: (dx: Int, dy: Int) -> Unit,
    contentRenderer: @Composable (WindowType, WindowPayload) -> Unit,
) {
    var dragX by remember(spec.id) { mutableStateOf(spec.x.toFloat()) }
    var dragY by remember(spec.id) { mutableStateOf(spec.y.toFloat()) }
    val isMaximized = spec.state == WindowState.MAXIMIZED

    val widthModifier = if (isMaximized) Modifier.fillMaxWidth() else Modifier.width(spec.width.dp)
    val heightModifier = if (isMaximized) Modifier.fillMaxSize() else Modifier.height(spec.height.dp)

    Box(
        modifier = Modifier
            .absoluteOffset(x = dragX.dp, y = dragY.dp)
            .then(widthModifier)
            .then(heightModifier)
            .shadow(elevation = if (isFocused) 12.dp else 4.dp, shape = RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title bar — drag handle (drag only works here, not on content)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(spec.id) {
                        detectDragGestures { change, drag ->
                            dragX += drag.x
                            dragY += drag.y
                            onMove(drag.x.toInt(), drag.y.toInt())
                            change.consume()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isFocused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onMinimize, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Minimize, contentDescription = "Minimize", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMaximize, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Fullscreen, contentDescription = "Maximize", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
            // Body — real content from the caller
            Box(modifier = Modifier.fillMaxSize()) {
                contentRenderer(spec.type, spec.payload)
            }
        }
    }
}

@Composable
private fun DefaultWindowContent(type: WindowType) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "${type.name} window",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
