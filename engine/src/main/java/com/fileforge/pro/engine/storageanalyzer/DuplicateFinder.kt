package com.fileforge.pro.engine.storageanalyzer

import com.fileforge.pro.core.security.FileHasher
import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Group of duplicate files (Master Spec §36 — Duplicate Finder).
 */
data class DuplicateGroup(
    val size: Long,
    val hash: String,
    val files: List<FFile>,
) {
    val wastedSpace: Long get() = size * (files.size - 1)
}

/**
 * Result of the duplicate-finding scan.
 */
data class DuplicateResult(
    val groups: List<DuplicateGroup>,
    val scannedFiles: Int,
    val totalWastedBytes: Long,
    val isComplete: Boolean,
)

/**
 * Finds duplicate files via 3-stage strategy (Master Spec §36):
 *   1. Group by exact size (fast, free).
 *   2. For size groups > 1, hash first 4KB (quick fingerprint).
 *   3. For groups still > 1, hash entire file (final proof).
 */
@Singleton
class DuplicateFinder @Inject constructor(
    private val providerRegistry: StorageProviderRegistry,
) {

    fun find(scope: FPath): Flow<DuplicateResult> = flow {
        // Stage 1 — collect all files grouped by size
        val bySize = mutableMapOf<Long, MutableList<FFile>>()
        var scanned = 0

        collectFiles(scope) { file ->
            scanned++
            bySize.getOrPut(file.size) { mutableListOf() }.add(file)
            if (scanned % 200 == 0) {
                emit(
                    DuplicateResult(
                        groups = emptyList(),
                        scannedFiles = scanned,
                        totalWastedBytes = 0,
                        isComplete = false,
                    )
                )
            }
        }

        // Stage 2 + 3 — only sizes with 2+ files are candidates
        val candidates = bySize.filter { it.value.size > 1 }.values.toList()
        val groups = mutableListOf<DuplicateGroup>()
        var wasted = 0L

        for (sizeGroup in candidates) {
            // Stage 2: hash first 4KB
            val byHeadHash = mutableMapOf<String, MutableList<FFile>>()
            for (file in sizeGroup) {
                val headHash = hashHead(file) ?: continue
                byHeadHash.getOrPut(headHash) { mutableListOf() }.add(file)
            }

            // Stage 3: for head-hash groups > 1, hash full file
            for (headGroup in byHeadHash.values.filter { it.size > 1 }) {
                val byFullHash = mutableMapOf<String, MutableList<FFile>>()
                for (file in headGroup) {
                    val fullHash = hashFull(file) ?: continue
                    byFullHash.getOrPut(fullHash) { mutableListOf() }.add(file)
                }
                for ((fullHash, fullGroup) in byFullHash.filter { it.value.size > 1 }) {
                    val group = DuplicateGroup(
                        size = fullGroup.first().size,
                        hash = fullHash,
                        files = fullGroup.toList(),
                    )
                    groups.add(group)
                    wasted += group.wastedSpace
                }
            }
        }

        emit(
            DuplicateResult(
                groups = groups.sortedByDescending { it.wastedSpace },
                scannedFiles = scanned,
                totalWastedBytes = wasted,
                isComplete = true,
            )
        )
    }

    private suspend fun collectFiles(path: FPath, onFile: suspend (FFile) -> Unit) {
        val provider = providerRegistry.get(path) ?: return
        val items = when (val r = provider.list(path)) {
            is Result.Ok -> r.value
            is Result.Err -> return
        }
        for (item in items) {
            if (item.isDirectory) {
                collectFiles(item.path, onFile)
            } else if (item.size > 0) {
                onFile(item)
            }
        }
    }

    private suspend fun hashHead(file: FFile): String? = withContext(Dispatchers.IO) {
        try {
            val provider = providerRegistry.get(file.path) ?: return@withContext null
            when (val r = provider.openInputStream(file.path)) {
                is Result.Ok -> r.value.use { stream ->
                    val head = ByteArray(4096)
                    val read = stream.read(head)
                    FileHasher.hashHead(if (read > 0) head.copyOf(read) else ByteArray(0))
                }
                is Result.Err -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun hashFull(file: FFile): String? = withContext(Dispatchers.IO) {
        try {
            val provider = providerRegistry.get(file.path) ?: return@withContext null
            when (val r = provider.openInputStream(file.path)) {
                is Result.Ok -> r.value.use { stream ->
                    val bytes = stream.readBytes()
                    FileHasher.hashFull(bytes)
                }
                is Result.Err -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
