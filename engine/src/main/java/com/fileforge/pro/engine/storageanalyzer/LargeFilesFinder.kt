package com.fileforge.pro.engine.storageanalyzer

import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds large files above a given threshold (Master Spec §35 — Large Files).
 *
 * Default thresholds: 100MB / 500MB / 1GB / 5GB
 */
@Singleton
class LargeFilesFinder @Inject constructor(
    private val providerRegistry: StorageProviderRegistry,
) {

    fun find(
        scope: FPath,
        minSize: Long = DEFAULT_THRESHOLD,
    ): Flow<List<FFile>> = flow {
        val results = mutableListOf<FFile>()
        walkAndCollect(scope, minSize) { file ->
            insertSorted(results, file, maxSize = 200)
            emit(results.toList())
        }
        emit(results.toList())
    }

    private suspend fun walkAndCollect(
        path: FPath,
        minSize: Long,
        onMatch: suspend (FFile) -> Unit,
    ) {
        val provider = providerRegistry.get(path) ?: return
        val items = when (val r = provider.list(path)) {
            is com.fileforge.pro.domain.result.Result.Ok -> r.value
            is com.fileforge.pro.domain.result.Result.Err -> return
        }
        for (item in items) {
            if (item.isDirectory) {
                walkAndCollect(item.path, minSize, onMatch)
            } else if (item.size >= minSize) {
                onMatch(item)
            }
        }
    }

    private fun insertSorted(list: MutableList<FFile>, file: FFile, maxSize: Int) {
        val pos = list.binarySearchBy(file.size) { -it.size }
        val insertAt = if (pos < 0) -(pos + 1) else pos
        list.add(insertAt, file)
        if (list.size > maxSize) list.removeAt(list.lastIndex)
    }

    companion object {
        val THRESHOLD_100MB = 100L * 1024 * 1024
        val THRESHOLD_500MB = 500L * 1024 * 1024
        val THRESHOLD_1GB = 1024L * 1024 * 1024
        val THRESHOLD_5GB = 5L * 1024 * 1024 * 1024
        val DEFAULT_THRESHOLD = THRESHOLD_100MB
    }
}
