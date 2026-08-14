package com.fileforge.pro.engine.storageanalyzer

import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a storage analysis (Master Spec §34).
 */
data class StorageAnalysisResult(
    val totalSize: Long,
    val byFileType: Map<FileType, Long>,
    val topLargestFiles: List<FFile>,
    val folderCount: Int,
    val fileCount: Int,
    val scannedPaths: Int,
    val isComplete: Boolean,
) {
    val usedFraction: Map<FileType, Float>
        get() = byFileType.mapValues { (_, size) ->
            if (totalSize == 0L) 0f else size.toFloat() / totalSize
        }
}

/**
 * Walks a storage source and classifies files by type + size
 * (Master Spec §34 — Storage Analyzer).
 */
@Singleton
class StorageAnalyzerEngine @Inject constructor(
    private val providerRegistry: StorageProviderRegistry,
) {

    /**
     * Stream analysis progress. Emits partial results periodically so the UI
     * can show progress bar + incrementally filled chart.
     */
    fun analyze(scope: FPath): Flow<StorageAnalysisResult> = flow {
        val provider = providerRegistry.get(scope) ?: return@flow
        val byType = mutableMapOf<FileType, Long>()
        val largest = mutableListOf<FFile>()
        var folderCount = 0
        var fileCount = 0
        var totalSize = 0L
        var scanned = 0

        emit(
            StorageAnalysisResult(
                totalSize = 0,
                byFileType = emptyMap(),
                topLargestFiles = emptyList(),
                folderCount = 0,
                fileCount = 0,
                scannedPaths = 0,
                isComplete = false,
            )
        )

        walkAndAccumulate(scope) { file ->
            scanned++
            if (file.isDirectory) {
                folderCount++
            } else {
                fileCount++
                totalSize += file.size
                byType[file.fileType] = (byType[file.fileType] ?: 0L) + file.size
                // Keep top 50 largest files
                insertSorted(largest, file, maxSize = 50)
            }

            // Emit every 200 items
            if (scanned % 200 == 0) {
                emit(
                    StorageAnalysisResult(
                        totalSize = totalSize,
                        byFileType = byType.toMap(),
                        topLargestFiles = largest.toList(),
                        folderCount = folderCount,
                        fileCount = fileCount,
                        scannedPaths = scanned,
                        isComplete = false,
                    )
                )
            }
        }

        emit(
            StorageAnalysisResult(
                totalSize = totalSize,
                byFileType = byType.toMap(),
                topLargestFiles = largest.toList(),
                folderCount = folderCount,
                fileCount = fileCount,
                scannedPaths = scanned,
                isComplete = true,
            )
        )
    }

    private suspend fun walkAndAccumulate(path: FPath, onFile: suspend (FFile) -> Unit) {
        val provider = providerRegistry.get(path) ?: return
        val items = when (val r = provider.list(path)) {
            is com.fileforge.pro.domain.result.Result.Ok -> r.value
            is com.fileforge.pro.domain.result.Result.Err -> {
                Logger.w(LogTags.ANALYZER, "Cannot list ${path.displayPath}: ${r.error.code}")
                return
            }
        }
        for (item in items) {
            onFile(item)
            if (item.isDirectory) {
                walkAndAccumulate(item.path, onFile)
            }
        }
    }

    private fun insertSorted(list: MutableList<FFile>, file: FFile, maxSize: Int) {
        // Insertion sort — keeps list sorted descending by size
        val pos = list.binarySearchBy(file.size) { -it.size }
        val insertAt = if (pos < 0) -(pos + 1) else pos
        list.add(insertAt, file)
        if (list.size > maxSize) list.removeAt(list.lastIndex)
    }
}
