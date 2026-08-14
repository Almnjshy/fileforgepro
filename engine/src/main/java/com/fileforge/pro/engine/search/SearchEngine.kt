package com.fileforge.pro.engine.search

import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FileFilter
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.repository.SearchRepository
import com.fileforge.pro.domain.repository.SearchResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streaming file search engine (Master Spec §33).
 *
 * Walks the directory tree starting from a [FPath] scope, applying the parsed
 * [SearchQuery] as it goes. Emits partial [SearchResult] batches so the UI
 * can show results incrementally.
 *
 * Architecture: this engine talks ONLY to [StorageProviderRegistry] — it does
 * not know whether files come from internal storage, USB, FTP, or anywhere else.
 */
@Singleton
class SearchEngine @Inject constructor(
    private val providerRegistry: StorageProviderRegistry,
) : SearchRepository {

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    @Volatile
    private var currentJob: Job? = null

    override fun search(
        query: String,
        scope: FPath,
        filter: FileFilter,
    ): Flow<SearchResult> = flow {
        val parsed = SearchQuery.parse(query)
        val combinedFilter = mergeFilter(parsed.filter, filter)

        _isRunning.value = true
        val scannedPaths = AtomicInteger(0)
        val foundItems = mutableListOf<FFile>()
        val startTime = System.currentTimeMillis()

        try {
            walkAndEmit(scope, combinedFilter, parsed.nameContains) { file, scanned ->
                scannedPaths.addAndGet(scanned)
                foundItems.add(file)
                // Emit in batches of 20, or first result immediately
                if (foundItems.size == 1 || foundItems.size % 20 == 0) {
                    emit(
                        SearchResult(
                            query = query,
                            items = foundItems.toList(),
                            isComplete = false,
                            scannedPaths = scannedPaths.get(),
                            elapsedMs = System.currentTimeMillis() - startTime,
                        )
                    )
                }
            }
            // Final emit — complete
            emit(
                SearchResult(
                    query = query,
                    items = foundItems.toList(),
                    isComplete = true,
                    scannedPaths = scannedPaths.get(),
                    elapsedMs = System.currentTimeMillis() - startTime,
                )
            )
        } catch (e: CancellationException) {
            Logger.i(LogTags.SEARCH, "Search cancelled: $query")
            emit(
                SearchResult(
                    query = query,
                    items = foundItems.toList(),
                    isComplete = true,
                    scannedPaths = scannedPaths.get(),
                    elapsedMs = System.currentTimeMillis() - startTime,
                )
            )
        } catch (e: Throwable) {
            Logger.e(LogTags.SEARCH, "Search failed: $query", e)
        } finally {
            _isRunning.value = false
        }
    }

    override suspend fun cancel() {
        currentJob?.cancel()
        currentJob = null
        _isRunning.value = false
    }

    // ───────────── Helpers ─────────────

    private suspend fun walkAndEmit(
        path: FPath,
        filter: FileFilter,
        nameContains: String?,
        onMatch: suspend (FFile, Int) -> Unit,
    ) {
        val provider = providerRegistry.get(path) ?: return
        val items = when (val r = provider.list(path)) {
            is com.fileforge.pro.domain.result.Result.Ok -> r.value
            is com.fileforge.pro.domain.result.Result.Err -> {
                Logger.w(LogTags.SEARCH, "Cannot list ${path.displayPath}: ${r.error.code}")
                return
            }
        }

        for (item in items) {
            yield() // cooperative cancellation
            if (matches(item, filter, nameContains)) {
                onMatch(item, 1)
            }
        }

        // Recurse into subdirectories
        for (dir in items.filter { it.isDirectory }) {
            walkAndEmit(dir.path, filter, nameContains, onMatch)
        }
    }

    private fun matches(file: FFile, filter: FileFilter, nameContains: String?): Boolean {
        // Always exclude hidden files unless explicitly requested
        if (file.isHidden && !filter.showHidden) return false

        // File type filter
        filter.fileTypes?.let { types ->
            if (file.fileType !in types) return false
        }

        // Extension filter
        filter.extensions?.let { exts ->
            val ext = file.extension?.lowercase() ?: return false
            if (ext !in exts) return false
        }

        // Size filter
        if (!file.isDirectory) {
            filter.minSize?.let { if (file.size < it) return false }
            filter.maxSize?.let { if (file.size > it) return false }
        }

        // Date filter
        filter.modifiedAfter?.let { if (file.lastModified < it) return false }
        filter.modifiedBefore?.let { if (file.lastModified > it) return false }

        // Name contains
        if (nameContains != null) {
            if (!file.name.lowercase().contains(nameContains)) return false
        }

        return true
    }

    private fun mergeFilter(a: FileFilter, b: FileFilter): FileFilter {
        return FileFilter(
            fileTypes = a.fileTypes ?: b.fileTypes,
            extensions = a.extensions ?: b.extensions,
            minSize = a.minSize ?: b.minSize,
            maxSize = a.maxSize ?: b.maxSize,
            modifiedAfter = a.modifiedAfter ?: b.modifiedAfter,
            modifiedBefore = a.modifiedBefore ?: b.modifiedBefore,
            showHidden = a.showHidden || b.showHidden,
            searchQuery = a.searchQuery ?: b.searchQuery,
        )
    }
}
