package com.fileforge.pro.domain.repository

import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.ViewMode
import com.fileforge.pro.domain.model.ViewSettings
import com.fileforge.pro.domain.result.Result
import kotlinx.coroutines.flow.Flow

/** Persistent user favorites (folders/files). */
interface FavoritesRepository {
    fun observe(): Flow<List<FFile>>
    suspend fun add(file: FFile): Result<Unit>
    suspend fun remove(path: FPath): Result<Unit>
    suspend fun contains(path: FPath): Boolean
    suspend fun clear(): Result<Unit>
}

/** Recently accessed files. */
interface RecentRepository {
    fun observe(limit: Int = 100): Flow<List<RecentEntry>>
    suspend fun recordAccess(path: FPath, name: String): Result<Unit>
    suspend fun remove(path: FPath): Result<Unit>
    suspend fun clearHistory(): Result<Unit>
}

data class RecentEntry(
    val path: FPath,
    val name: String,
    val lastAccessed: Long,
)

/** View preferences — global + per-folder. */
interface ViewSettingsRepository {
    fun observeGlobal(): Flow<ViewSettings>
    fun observeForFolder(folder: FPath): Flow<ViewSettings>
    suspend fun setGlobalMode(mode: ViewMode): Result<Unit>
    suspend fun setForFolder(folder: FPath, settings: ViewSettings): Result<Unit>
    suspend fun resetFolderSettings(folder: FPath): Result<Unit>
}

/** Search history (saved recent queries). */
interface SearchHistoryRepository {
    fun observe(limit: Int = 20): Flow<List<String>>
    suspend fun add(query: String): Result<Unit>
    suspend fun clear(): Result<Unit>
}
