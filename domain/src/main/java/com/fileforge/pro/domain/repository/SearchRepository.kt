package com.fileforge.pro.domain.repository

import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FileFilter
import com.fileforge.pro.domain.model.FPath
import kotlinx.coroutines.flow.Flow

/**
 * Search engine (Master Spec §33). Supports query syntax:
 *   *.mp4                  — extension
 *   name:project           — name contains
 *   type:image             — file type
 *   size > 500MB           — size range
 *   modified:today         — date
 */
interface SearchRepository {
    /** Streaming results — emits partial batches as they're found. */
    fun search(query: String, scope: FPath, filter: FileFilter = FileFilter.EMPTY): Flow<SearchResult>

    /** Cancel any running search started from this repository. */
    suspend fun cancel()

    val isRunning: Flow<Boolean>
}

data class SearchResult(
    val query: String,
    val items: List<FFile>,
    val isComplete: Boolean,
    val scannedPaths: Int,
    val elapsedMs: Long,
)
