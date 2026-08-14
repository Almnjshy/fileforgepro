package com.fileforge.pro.data.repository

import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import com.fileforge.pro.core.storage.StorageProvider
import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegates every operation to the appropriate [StorageProvider] via
 * [StorageProviderRegistry] (Master Spec §81).
 */
@Singleton
class FileRepositoryImpl @Inject constructor(
    private val registry: StorageProviderRegistry,
) : FileRepository {

    private fun providerFor(path: FPath): StorageProvider =
        registry.get(path) ?: error("No provider for source ${path.sourceId}")

    override suspend fun listDirectory(path: FPath): Result<List<FFile>> = withContext(Dispatchers.IO) {
        providerFor(path).list(path)
    }

    override suspend fun stat(path: FPath): Result<FFile> = withContext(Dispatchers.IO) {
        providerFor(path).stat(path)
    }

    override suspend fun exists(path: FPath): Boolean = withContext(Dispatchers.IO) {
        registry.get(path)?.exists(path) ?: false
    }

    override suspend fun createDirectory(parent: FPath, name: String): Result<FFile> = withContext(Dispatchers.IO) {
        providerFor(parent).createDirectory(parent, name)
    }

    override suspend fun createFile(parent: FPath, name: String): Result<FFile> = withContext(Dispatchers.IO) {
        providerFor(parent).createFile(parent, name)
    }

    override suspend fun rename(path: FPath, newName: String): Result<FFile> = withContext(Dispatchers.IO) {
        providerFor(path).rename(path, newName)
    }

    override suspend fun delete(path: FPath): Result<Unit> = withContext(Dispatchers.IO) {
        providerFor(path).delete(path)
    }

    override suspend fun copy(sources: List<FPath>, destination: FPath): Result<List<FFile>> = withContext(Dispatchers.IO) {
        resultOf {
            val results = mutableListOf<FFile>()
            for (src in sources) {
                val dst = destination / src.name
                when (val r = providerFor(src).copy(src, dst)) {
                    is Result.Ok -> results.add(r.value)
                    is Result.Err -> throw r.error.cause ?: java.io.IOException(r.error.message)
                }
            }
            results
        }
    }

    override suspend fun move(sources: List<FPath>, destination: FPath): Result<List<FFile>> = withContext(Dispatchers.IO) {
        resultOf {
            val results = mutableListOf<FFile>()
            for (src in sources) {
                val dst = destination / src.name
                when (val r = providerFor(src).move(src, dst)) {
                    is Result.Ok -> results.add(r.value)
                    is Result.Err -> throw r.error.cause ?: java.io.IOException(r.error.message)
                }
            }
            results
        }
    }

    override suspend fun computeDirectorySize(path: FPath): Result<Long> = withContext(Dispatchers.IO) {
        providerFor(path).computeDirectorySize(path)
    }

    override suspend fun countChildren(path: FPath): Result<Int> = withContext(Dispatchers.IO) {
        providerFor(path).countChildren(path)
    }
}
