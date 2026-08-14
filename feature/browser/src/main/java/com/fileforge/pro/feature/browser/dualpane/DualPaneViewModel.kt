package com.fileforge.pro.feature.browser.dualpane

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.repository.FileOperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Identifies which pane in a dual-pane layout.
 */
enum class PaneId { LEFT, RIGHT }

/**
 * Coordinates cross-pane operations (Master Spec §18 — Dual Pane).
 *
 * Each pane has its own independent [com.fileforge.pro.feature.browser.BrowserViewModel]
 * (instantiated via `hiltViewModel(key = "pane-left")` / `hiltViewModel(key = "pane-right")`).
 * This ViewModel only handles:
 *   - Which pane is currently active (for keyboard / focus routing)
 *   - Cross-pane copy / move operations
 *   - Drag & drop target tracking
 */
data class DualPaneUiState(
    val activePane: PaneId = PaneId.LEFT,
    val leftPath: FPath? = null,
    val rightPath: FPath? = null,
    val dragSourcePane: PaneId? = null,
    val dragOverPane: PaneId? = null,
    val isDualPaneEnabled: Boolean = false,
    val dividerFraction: Float = 0.5f, // 0..1, position of the divider
)

@HiltViewModel
class DualPaneViewModel @Inject constructor(
    private val operationRepo: FileOperationRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DualPaneUiState())
    val uiState: StateFlow<DualPaneUiState> = _uiState.asStateFlow()

    fun setActivePane(pane: PaneId) {
        _uiState.update { it.copy(activePane = pane) }
    }

    fun setPanePath(pane: PaneId, path: FPath) {
        _uiState.update { state ->
            when (pane) {
                PaneId.LEFT -> state.copy(leftPath = path)
                PaneId.RIGHT -> state.copy(rightPath = path)
            }
        }
    }

    fun setDualPaneEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isDualPaneEnabled = enabled) }
    }

    fun setDividerFraction(fraction: Float) {
        _uiState.update {
            it.copy(dividerFraction = fraction.coerceIn(0.2f, 0.8f))
        }
    }

    // ---- Drag & drop state ----

    fun onDragStart(sourcePane: PaneId) {
        _uiState.update { it.copy(dragSourcePane = sourcePane) }
    }

    fun onDragEnter(targetPane: PaneId) {
        _uiState.update { it.copy(dragOverPane = targetPane) }
    }

    fun onDragEnd() {
        _uiState.update { it.copy(dragSourcePane = null, dragOverPane = null) }
    }

    // ---- Cross-pane operations ----

    /**
     * Copy the given [sources] from one pane to the other pane's current directory.
     */
    fun copyToOppositePane(sources: List<FPath>, fromPane: PaneId) {
        val state = _uiState.value
        val destination = when (fromPane) {
            PaneId.LEFT -> state.rightPath
            PaneId.RIGHT -> state.leftPath
        } ?: return

        viewModelScope.launch(dispatchers.io) {
            operationRepo.enqueueCopy(sources, destination)
        }
    }

    /**
     * Move the given [sources] from one pane to the other pane's current directory.
     */
    fun moveToOppositePane(sources: List<FPath>, fromPane: PaneId) {
        val state = _uiState.value
        val destination = when (fromPane) {
            PaneId.LEFT -> state.rightPath
            PaneId.RIGHT -> state.leftPath
        } ?: return

        viewModelScope.launch(dispatchers.io) {
            operationRepo.enqueueMove(sources, destination)
        }
    }
}
