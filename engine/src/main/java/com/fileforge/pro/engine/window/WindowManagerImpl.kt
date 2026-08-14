package com.fileforge.pro.engine.window

import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import com.fileforge.pro.domain.model.WindowPayload
import com.fileforge.pro.domain.model.WindowSpec
import com.fileforge.pro.domain.model.WindowState
import com.fileforge.pro.domain.model.WindowType
import com.fileforge.pro.domain.repository.WindowManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-app floating window manager (Master Spec §19–25, §21 — WindowManager).
 *
 * Holds the canonical list of open windows and their Z-order. UI subscribes
 * to [windows] and renders each window as a floating Composable.
 *
 * Not to be confused with Android's WindowManager — this is purely
 * in-app multi-window for the FileForge "desktop-like" experience.
 */
@Singleton
class WindowManagerImpl @Inject constructor() : WindowManagerRepository {

    private val mutex = Mutex()

    private val _windows = MutableStateFlow<List<WindowSpec>>(emptyList())
    override fun observeWindows(): StateFlow<List<WindowSpec>> = _windows.asStateFlow()

    private val _focusedId = MutableStateFlow<String?>(null)
    override fun observeFocused(): StateFlow<String?> = _focusedId.asStateFlow()

    /** Viewport dimensions — set by the UI layer so snap calculations work. */
    @Volatile
    private var viewportWidth: Int = 1080

    @Volatile
    private var viewportHeight: Int = 1920

    override fun setViewport(width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(320)
        viewportHeight = height.coerceAtLeast(400)
    }

    override suspend fun openBrowserWindow(title: String, payload: WindowPayload): Result<String> =
        openWindow(WindowType.FILE_BROWSER, title, payload)

    override suspend fun openWindow(
        type: WindowType,
        title: String,
        payload: WindowPayload,
    ): Result<String> = mutex.withLock {
        resultOf {
            val id = UUID.randomUUID().toString()
            val z = (_windows.value.maxOfOrNull { it.zOrder } ?: 0) + 1
            val window = WindowSpec(
                id = id,
                title = title,
                type = type,
                payload = payload,
                x = 60 + (_windows.value.size * 30) % 200,
                y = 80 + (_windows.value.size * 30) % 200,
                width = defaultWidth(type),
                height = defaultHeight(type),
                zOrder = z,
                state = WindowState.NORMAL,
                isFocused = true,
            )
            _windows.update { current ->
                current.map { it.copy(isFocused = false) } + window
            }
            _focusedId.value = id
            Logger.i(LogTags.WINDOW, "Opened window: $title ($id)")
            id
        }
    }

    override suspend fun close(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            _windows.update { current -> current.filterNot { it.id == id } }
            if (_focusedId.value == id) {
                _focusedId.value = _windows.value.lastOrNull()?.id
                _windows.value.lastOrNull()?.let { last ->
                    _windows.update { current ->
                        current.map { if (it.id == last.id) it.copy(isFocused = true) else it }
                    }
                }
            }
            Logger.i(LogTags.WINDOW, "Closed window: $id")
        }
    }

    override suspend fun focus(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            val maxZ = _windows.value.maxOfOrNull { it.zOrder } ?: 0
            _windows.update { current ->
                current.map {
                    when (it.id) {
                        id -> it.copy(isFocused = true, zOrder = maxZ + 1)
                        else -> it.copy(isFocused = false)
                    }
                }
            }
            _focusedId.value = id
        }
    }

    override suspend fun minimize(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            _windows.update { current ->
                current.map {
                    if (it.id == id) it.copy(state = WindowState.MINIMIZED, isFocused = false) else it
                }
            }
            if (_focusedId.value == id) {
                _focusedId.value = _windows.value.lastOrNull { it.state != WindowState.MINIMIZED }?.id
            }
        }
    }

    override suspend fun restore(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            _windows.update { current ->
                current.map {
                    if (it.id == id) it.copy(state = WindowState.NORMAL) else it
                }
            }
        }
        focus(id).getOrNull()
        Result.Ok(Unit)
    }

    override suspend fun maximize(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            _windows.update { current ->
                current.map {
                    if (it.id == id) it.copy(state = WindowState.MAXIMIZED) else it
                }
            }
        }
        focus(id).getOrNull()
        Result.Ok(Unit)
    }

    override suspend fun move(id: String, x: Int, y: Int): Result<Unit> = mutex.withLock {
        resultOf {
            _windows.update { current ->
                current.map { if (it.id == id) it.copy(x = x, y = y) else it }
            }
        }
    }

    override suspend fun resize(id: String, width: Int, height: Int): Result<Unit> = mutex.withLock {
        resultOf {
            _windows.update { current ->
                current.map {
                    if (it.id == id) it.copy(width = width.coerceAtLeast(240), height = height.coerceAtLeast(180)) else it
                }
            }
        }
    }

    override suspend fun snapLeft(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            val halfWidth = viewportWidth / 2
            _windows.update { current ->
                current.map {
                    if (it.id == id) it.copy(
                        x = 0,
                        y = 0,
                        width = halfWidth,
                        height = viewportHeight,
                        state = WindowState.NORMAL,
                    ) else it
                }
            }
        }
        focus(id)
    }

    override suspend fun snapRight(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            val halfWidth = viewportWidth / 2
            _windows.update { current ->
                current.map {
                    if (it.id == id) it.copy(
                        x = halfWidth,
                        y = 0,
                        width = halfWidth,
                        height = viewportHeight,
                        state = WindowState.NORMAL,
                    ) else it
                }
            }
        }
        focus(id)
    }

    /** Snap window to top half (Master Spec §24). */
    override suspend fun snapTop(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            val halfHeight = viewportHeight / 2
            _windows.update { current ->
                current.map {
                    if (it.id == id) it.copy(
                        x = 0,
                        y = 0,
                        width = viewportWidth,
                        height = halfHeight,
                        state = WindowState.NORMAL,
                    ) else it
                }
            }
        }
        focus(id)
    }

    /** Snap window to bottom half (Master Spec §24). */
    override suspend fun snapBottom(id: String): Result<Unit> = mutex.withLock {
        resultOf {
            val halfHeight = viewportHeight / 2
            _windows.update { current ->
                current.map {
                    if (it.id == id) it.copy(
                        x = 0,
                        y = halfHeight,
                        width = viewportWidth,
                        height = halfHeight,
                        state = WindowState.NORMAL,
                    ) else it
                }
            }
        }
        focus(id)
    }

    override suspend fun getWindows(): List<WindowSpec> = _windows.value

    override suspend fun getFocused(): WindowSpec? = _windows.value.firstOrNull { it.isFocused }

    // ---- defaults ----

    private fun defaultWidth(type: WindowType): Int = when (type) {
        WindowType.FILE_BROWSER, WindowType.FOLDER -> 720
        WindowType.TEXT_EDITOR -> 640
        WindowType.IMAGE_VIEWER -> 480
        WindowType.PDF_VIEWER -> 640
        WindowType.ARCHIVE -> 600
        WindowType.PROPERTIES -> 480
    }

    private fun defaultHeight(type: WindowType): Int = when (type) {
        WindowType.FILE_BROWSER, WindowType.FOLDER -> 540
        WindowType.TEXT_EDITOR -> 540
        WindowType.IMAGE_VIEWER -> 540
        WindowType.PDF_VIEWER -> 720
        WindowType.ARCHIVE -> 480
        WindowType.PROPERTIES -> 540
    }
}
