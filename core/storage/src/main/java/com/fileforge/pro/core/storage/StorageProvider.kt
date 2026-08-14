package com.fileforge.pro.core.storage

import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.StorageSource

/**
 * StorageProvider — the abstraction layer between the File Browser and the
 * concrete storage backend (Master Spec §5, §6, §81).
 *
 * The Browser NEVER knows whether it's reading from Internal Storage, USB,
 * FTP, SMB, or Cloud. It always talks to a [StorageProvider].
 *
 * Each provider implementation lives in `:engine` and is registered into
 * [StorageProviderRegistry]. The browser resolves a provider by
 * `path.sourceId` via [StorageProviderRegistry.get].
 */
interface StorageProvider {

    /** The source ID this provider serves. */
    val sourceId: String

    /** The [StorageSource] descriptor. */
    val source: StorageSource

    // ---- Read operations ----

    suspend fun list(path: FPath): Result<List<FFile>>

    suspend fun stat(path: FPath): Result<FFile>

    suspend fun exists(path: FPath): Boolean

    // ---- Write operations ----

    suspend fun createDirectory(parent: FPath, name: String): Result<FFile>

    suspend fun createFile(parent: FPath, name: String): Result<FFile>

    suspend fun rename(path: FPath, newName: String): Result<FFile>

    suspend fun delete(path: FPath): Result<Unit>

    suspend fun copy(source: FPath, destination: FPath): Result<FFile>

    suspend fun move(source: FPath, destination: FPath): Result<FFile>

    // ---- Stream I/O ----

    /** Opens an input stream for reading. Caller must close. */
    suspend fun openInputStream(path: FPath): Result<java.io.InputStream>

    /** Opens an output stream for writing. Caller must close. */
    suspend fun openOutputStream(path: FPath): Result<java.io.OutputStream>

    // ---- Metadata ----

    suspend fun computeDirectorySize(path: FPath): Result<Long>

    suspend fun countChildren(path: FPath): Result<Int>

    /** Optional: free / total bytes, null if not applicable (e.g. FTP). */
    suspend fun queryStorageStats(): StorageStats?
}

data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0)
    val usedFraction: Float get() = if (totalBytes == 0L) 0f else usedBytes.toFloat() / totalBytes
}
