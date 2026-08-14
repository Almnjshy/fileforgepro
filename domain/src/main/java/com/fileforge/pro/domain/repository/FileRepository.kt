package com.fileforge.pro.domain.repository

import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.StorageSource
import kotlinx.coroutines.flow.Flow

/**
 * Read-only storage source discovery.
 */
interface StorageSourceRepository {
    /** Emits the current set of available storage sources (Internal/SD/USB/Network/...). */
    fun observeSources(): Flow<List<StorageSource>>

    /** Snapshot of current sources. */
    suspend fun getSources(): List<StorageSource>

    suspend fun refreshSources(): List<StorageSource>

    fun getById(id: String): StorageSource?
}

/**
 * File listing / metadata repository. Talks to [StorageProvider]s.
 * The browser/UI never goes below this layer.
 */
interface FileRepository {
    suspend fun listDirectory(path: FPath): Result<List<FFile>>
    suspend fun stat(path: FPath): Result<FFile>
    suspend fun exists(path: FPath): Boolean
    suspend fun createDirectory(parent: FPath, name: String): Result<FFile>
    suspend fun createFile(parent: FPath, name: String): Result<FFile>
    suspend fun rename(path: FPath, newName: String): Result<FFile>
    suspend fun delete(path: FPath): Result<Unit>
    suspend fun copy(sources: List<FPath>, destination: FPath): Result<List<FFile>>
    suspend fun move(sources: List<FPath>, destination: FPath): Result<List<FFile>>

    /** Returns total size of a directory tree (for properties / analyzer). */
    suspend fun computeDirectorySize(path: FPath): Result<Long>

    /** Returns immediate child count of a directory (for folder badges). */
    suspend fun countChildren(path: FPath): Result<Int>
}
