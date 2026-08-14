package com.fileforge.pro.domain.repository

import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FileOperation
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Long-running file operations engine (Master Spec §26–29).
 * UI enqueues operations here and observes their progress via [observeOperations].
 */
interface FileOperationRepository {
    fun observeOperations(): Flow<List<FileOperation>>
    fun observeOperation(id: String): Flow<FileOperation?>

    suspend fun enqueueCopy(sources: List<FPath>, destination: FPath): Result<String>
    suspend fun enqueueMove(sources: List<FPath>, destination: FPath): Result<String>
    suspend fun enqueueDelete(sources: List<FPath>): Result<String>
    suspend fun enqueueRename(path: FPath, newName: String): Result<String>
    suspend fun enqueueCreateFolder(parent: FPath, name: String): Result<String>
    suspend fun enqueueCreateFile(parent: FPath, name: String): Result<String>
    suspend fun enqueueCompress(sources: List<FPath>, destination: FPath): Result<String>
    suspend fun enqueueExtract(archive: FPath, destination: FPath): Result<String>

    suspend fun pause(id: String): Result<Unit>
    suspend fun resume(id: String): Result<Unit>
    suspend fun cancel(id: String): Result<Unit>
    suspend fun retry(id: String): Result<Unit>
    suspend fun resolveConflict(id: String, resolution: com.fileforge.pro.domain.model.ConflictResolution): Result<Unit>

    suspend fun getActiveOperationsCount(): Int
    suspend fun getPendingConflicts(): List<com.fileforge.pro.domain.model.FileConflict>
}
