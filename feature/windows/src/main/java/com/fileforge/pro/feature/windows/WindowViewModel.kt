package com.fileforge.pro.feature.windows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.domain.model.WindowPayload
import com.fileforge.pro.domain.model.WindowType
import com.fileforge.pro.domain.repository.WindowManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WindowUiState(
    val windows: List<com.fileforge.pro.domain.model.WindowSpec> = emptyList(),
    val focusedId: String? = null,
)

@HiltViewModel
class WindowViewModel @Inject constructor(
    private val windowManager: WindowManagerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WindowUiState())
    val uiState: StateFlow<WindowUiState> = _uiState.asStateFlow()

    init {
        windowManager.observeWindows().onEach { windows ->
            _uiState.value = _uiState.value.copy(windows = windows)
        }.launchIn(viewModelScope)

        windowManager.observeFocused().onEach { focused ->
            _uiState.value = _uiState.value.copy(focusedId = focused)
        }.launchIn(viewModelScope)
    }

    fun focus(id: String) = viewModelScope.launch { windowManager.focus(id) }
    fun close(id: String) = viewModelScope.launch { windowManager.close(id) }
    fun minimize(id: String) = viewModelScope.launch { windowManager.minimize(id) }
    fun maximize(id: String) = viewModelScope.launch { windowManager.maximize(id) }
    fun restore(id: String) = viewModelScope.launch { windowManager.restore(id) }
    fun move(id: String, dx: Int, dy: Int) = viewModelScope.launch { windowManager.move(id, dx, dy) }

    fun snapLeft(id: String) = viewModelScope.launch { windowManager.snapLeft(id) }
    fun snapRight(id: String) = viewModelScope.launch { windowManager.snapRight(id) }
    fun snapTop(id: String) = viewModelScope.launch { windowManager.snapTop(id) }
    fun snapBottom(id: String) = viewModelScope.launch { windowManager.snapBottom(id) }

    fun setViewport(width: Int, height: Int) {
        windowManager.setViewport(width, height)
    }

    fun openBrowser(title: String, sourceId: String, path: String) {
        viewModelScope.launch {
            val payload = WindowPayload.fromPath(
                com.fileforge.pro.domain.model.FPath.fromString(sourceId, path)
            )
            windowManager.openBrowserWindow(title, payload)
        }
    }

    fun openTextEditor(title: String, sourceId: String, path: String) {
        viewModelScope.launch {
            val payload = WindowPayload.fromPath(
                com.fileforge.pro.domain.model.FPath.fromString(sourceId, path)
            )
            windowManager.openWindow(WindowType.TEXT_EDITOR, title, payload)
        }
    }

    /**
     * Open a floating window of any type with a pre-built payload.
     */
    fun openWindow(type: WindowType, title: String, payload: WindowPayload) {
        viewModelScope.launch {
            windowManager.openWindow(type, title, payload)
        }
    }
}
