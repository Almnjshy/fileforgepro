package com.fileforge.pro.domain.repository

import com.fileforge.pro.domain.model.WindowSpec
import com.fileforge.pro.domain.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * In-app floating window manager (Master Spec §19–25).
 * Independent of File Browser — browser just renders the active window's content.
 */
interface WindowManagerRepository {
    fun observeWindows(): Flow<List<WindowSpec>>
    fun observeFocused(): Flow<String?>

    suspend fun openBrowserWindow(title: String, payload: com.fileforge.pro.domain.model.WindowPayload): Result<String>
    suspend fun openWindow(type: com.fileforge.pro.domain.model.WindowType, title: String, payload: com.fileforge.pro.domain.model.WindowPayload): Result<String>
    suspend fun close(id: String): Result<Unit>
    suspend fun focus(id: String): Result<Unit>
    suspend fun minimize(id: String): Result<Unit>
    suspend fun restore(id: String): Result<Unit>
    suspend fun maximize(id: String): Result<Unit>
    suspend fun move(id: String, x: Int, y: Int): Result<Unit>
    suspend fun resize(id: String, width: Int, height: Int): Result<Unit>
    suspend fun snapLeft(id: String): Result<Unit>
    suspend fun snapRight(id: String): Result<Unit>
    suspend fun snapTop(id: String): Result<Unit>
    suspend fun snapBottom(id: String): Result<Unit>
    suspend fun getWindows(): List<WindowSpec>
    suspend fun getFocused(): WindowSpec?

    /** Update the viewport dimensions used for snap calculations. */
    fun setViewport(width: Int, height: Int)
}
