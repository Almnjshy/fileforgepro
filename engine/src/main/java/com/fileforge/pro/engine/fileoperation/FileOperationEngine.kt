package com.fileforge.pro.engine.fileoperation

import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.domain.model.ConflictResolution
import com.fileforge.pro.domain.model.FileConflict
import com.fileforge.pro.domain.model.FileOperation
import com.fileforge.pro.domain.model.FileOperationKind
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.OperationState
import com.fileforge.pro.domain.repository.FileRepository
import com.fileforge.pro.domain.repository.FileOperationRepository
import com.fileforge.pro.domain.result.FileError
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Long-running file operations engine (Master Spec §26–29).
 *
 * Features:
 *  - Enqueue copy/move/delete/rename/create/compress/extract operations.
 *  - Track per-operation progress (bytes transferred / total bytes / speed / ETA).
 *  - Support pause / resume / cancel.
 *  - Surface file conflicts via [FileConflict] for UI resolution.
 *  - Emit progress updates via Flow (UI subscribes).
 *
 * Architecture (Master Spec §81):
 *   UI ─▶ ViewModel ─▶ FileOperationRepository (this) ─▶ StorageProvider
 */
@Singleton
class FileOperationEngine @Inject constructor(
    private val fileRepository: FileRepository,
) : FileOperationRepository {

    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisor)
    private val mutex = Mutex()

    /** Currently registered operations (id → operation state). */
    private val operations = ConcurrentHashMap<String, FileOperation>()
    private val runningJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()
    private val cancelledFlags = ConcurrentHashMap<String, Boolean>()

    private val _operationsFlow = MutableStateFlow<List<FileOperation>>(emptyList())
    override fun observeOperations(): StateFlow<List<FileOperation>> = _operationsFlow.asStateFlow()

    private val _pendingConflicts = MutableStateFlow<List<FileConflict>>(emptyList())

    override fun observeOperation(id: String): StateFlow<FileOperation?> =
        _operationsFlow.map { ops -> ops.firstOrNull { it.id == id } }
            .stateIn(
                scope = CoroutineScope(Dispatchers.Default),
                started = SharingStarted.Eagerly,
                initialValue = operations[id],
            )

    private fun emitUpdate() {
        _operationsFlow.value = operations.values.sortedBy { it.createdAt }.toList()
    }

    private fun updateOp(id: String, transform: (FileOperation) -> FileOperation) {
        val current = operations[id] ?: return
        operations[id] = transform(current)
        emitUpdate()
    }

    // ───────────── Enqueue operations ─────────────

    override suspend fun enqueueCopy(sources: List<FPath>, destination: FPath): Result<String> =
        enqueue(FileOperationKind.COPY, sources, destination)

    override suspend fun enqueueMove(sources: List<FPath>, destination: FPath): Result<String> =
        enqueue(FileOperationKind.MOVE, sources, destination)

    override suspend fun enqueueDelete(sources: List<FPath>): Result<String> =
        enqueue(FileOperationKind.DELETE, sources, null)

    override suspend fun enqueueRename(path: FPath, newName: String): Result<String> = resultOf {
        // Rename: encode newName as the destination path
        val id = UUID.randomUUID().toString()
        val op = FileOperation(
            id = id,
            kind = FileOperationKind.RENAME,
            sources = listOf(path),
            destination = null,
            createdAt = System.currentTimeMillis(),
        )
        registerAndStart(op, renameTo = newName)
        id
    }

    override suspend fun enqueueCreateFolder(parent: FPath, name: String): Result<String> = resultOf {
        val id = UUID.randomUUID().toString()
        val op = FileOperation(
            id = id,
            kind = FileOperationKind.CREATE_FOLDER,
            sources = listOf(parent),
            destination = null,
            createdAt = System.currentTimeMillis(),
        )
        registerAndStart(op, newName = name)
        id
    }

    override suspend fun enqueueCreateFile(parent: FPath, name: String): Result<String> = resultOf {
        val id = UUID.randomUUID().toString()
        val op = FileOperation(
            id = id,
            kind = FileOperationKind.CREATE_FILE,
            sources = listOf(parent),
            destination = null,
            createdAt = System.currentTimeMillis(),
        )
        registerAndStart(op, newName = name)
        id
    }

    override suspend fun enqueueCompress(sources: List<FPath>, destination: FPath): Result<String> =
        enqueue(FileOperationKind.COMPRESS, sources, destination)

    override suspend fun enqueueExtract(archive: FPath, destination: FPath): Result<String> =
        enqueue(FileOperationKind.EXTRACT, listOf(archive), destination)

    private suspend fun enqueue(
        kind: FileOperationKind,
        sources: List<FPath>,
        destination: FPath?,
    ): Result<String> = resultOf {
        require(sources.isNotEmpty()) { "Sources cannot be empty" }
        val id = UUID.randomUUID().toString()
        val op = FileOperation(
            id = id,
            kind = kind,
            sources = sources,
            destination = destination,
            createdAt = System.currentTimeMillis(),
        )
        registerAndStart(op)
        id
    }

    private fun registerAndStart(
        op: FileOperation,
        newName: String? = null,
        renameTo: String? = null,
    ) {
        operations[op.id] = op
        emitUpdate()
        val job = scope.launch {
            try {
                executeOperation(op, newName, renameTo)
            } catch (e: CancellationException) {
                Logger.i(LogTags.FILE_OPERATION, "Op ${op.id} cancelled")
                updateOp(op.id) { it.copy(state = OperationState.CANCELLED) }
            } catch (e: Throwable) {
                Logger.e(LogTags.FILE_OPERATION, "Op ${op.id} failed", e)
                updateOp(op.id) {
                    it.copy(state = OperationState.FAILED, error = e.message ?: "Unknown error")
                }
            } finally {
                runningJobs.remove(op.id)
            }
        }
        runningJobs[op.id] = job
    }

    // ───────────── Execution ─────────────

    private suspend fun executeOperation(
        op: FileOperation,
        newName: String? = null,
        renameTo: String? = null,
    ) {
        updateOp(op.id) { it.copy(state = OperationState.RUNNING) }

        when (op.kind) {
            FileOperationKind.COPY -> executeCopy(op)
            FileOperationKind.MOVE -> executeMove(op)
            FileOperationKind.DELETE -> executeDelete(op)
            FileOperationKind.RENAME -> executeRename(op, renameTo!!)
            FileOperationKind.CREATE_FOLDER -> executeCreateFolder(op, newName!!)
            FileOperationKind.CREATE_FILE -> executeCreateFile(op, newName!!)
            FileOperationKind.COMPRESS -> executeCompress(op)
            FileOperationKind.EXTRACT -> executeExtract(op)
        }

        // Mark complete if not already cancelled/failed
        val current = operations[op.id]
        if (current?.state == OperationState.RUNNING) {
            updateOp(op.id) {
                it.copy(state = OperationState.COMPLETED, progress = 1f)
            }
        }
    }

    private suspend fun executeCopy(op: FileOperation) {
        val dest = op.destination ?: throw IllegalStateException("Copy requires destination")
        var totalDone = 0L
        val totalSize = computeTotalSize(op.sources)
        updateOp(op.id) { it.copy(totalBytes = totalSize) }

        for (src in op.sources) {
            checkCancelled(op.id)
            val srcFile = fileRepository.stat(src).getOrNull() ?: continue
            val targetPath = dest / resolveName(srcFile, op.conflictResolution)

            // Conflict check
            if (fileRepository.exists(targetPath)) {
                handleConflict(op, srcFile, targetPath) ?: continue
            }

            val before = totalDone
            val r = fileRepository.copy(listOf(src), dest)
            if (r is Result.Err) {
                updateOp(op.id) { it.copy(state = OperationState.FAILED, error = r.error.message) }
                return
            }
            totalDone += srcFile.size
            updateProgress(op.id, totalDone, totalSize)
        }
    }

    private suspend fun executeMove(op: FileOperation) {
        val dest = op.destination ?: throw IllegalStateException("Move requires destination")
        var totalDone = 0L
        val totalSize = computeTotalSize(op.sources)
        updateOp(op.id) { it.copy(totalBytes = totalSize) }

        for (src in op.sources) {
            checkCancelled(op.id)
            val srcFile = fileRepository.stat(src).getOrNull() ?: continue

            if (fileRepository.exists(dest / srcFile.name)) {
                handleConflict(op, srcFile, dest / srcFile.name) ?: continue
            }

            val r = fileRepository.move(listOf(src), dest)
            if (r is Result.Err) {
                updateOp(op.id) { it.copy(state = OperationState.FAILED, error = r.error.message) }
                return
            }
            totalDone += srcFile.size
            updateProgress(op.id, totalDone, totalSize)
        }
    }

    private suspend fun executeDelete(op: FileOperation) {
        var done = 0
        val total = op.sources.size
        updateOp(op.id) { it.copy(totalBytes = total.toLong()) }

        for (src in op.sources) {
            checkCancelled(op.id)
            when (val r = fileRepository.delete(src)) {
                is Result.Ok -> {
                    done++
                    updateOp(op.id) {
                        it.copy(bytesTransferred = done.toLong(), progress = done.toFloat() / total)
                    }
                }
                is Result.Err -> {
                    Logger.w(LogTags.FILE_OPERATION, "Delete failed: ${src.displayPath}", r.error.cause)
                }
            }
            yield()
        }
    }

    private suspend fun executeRename(op: FileOperation, newName: String) {
        val src = op.sources.first()
        checkCancelled(op.id)
        when (val r = fileRepository.rename(src, newName)) {
            is Result.Ok -> updateOp(op.id) { it.copy(progress = 1f) }
            is Result.Err -> updateOp(op.id) {
                it.copy(state = OperationState.FAILED, error = r.error.message)
            }
        }
    }

    private suspend fun executeCreateFolder(op: FileOperation, name: String) {
        val parent = op.sources.first()
        checkCancelled(op.id)
        when (val r = fileRepository.createDirectory(parent, name)) {
            is Result.Ok -> updateOp(op.id) { it.copy(progress = 1f) }
            is Result.Err -> updateOp(op.id) {
                it.copy(state = OperationState.FAILED, error = r.error.message)
            }
        }
    }

    private suspend fun executeCreateFile(op: FileOperation, name: String) {
        val parent = op.sources.first()
        checkCancelled(op.id)
        when (val r = fileRepository.createFile(parent, name)) {
            is Result.Ok -> updateOp(op.id) { it.copy(progress = 1f) }
            is Result.Err -> updateOp(op.id) {
                it.copy(state = OperationState.FAILED, error = r.error.message)
            }
        }
    }

    private suspend fun executeCompress(op: FileOperation) {
        // Phase 12 will implement full archive creation.
        // For now we mark as failed with a clear message.
        updateOp(op.id) {
            it.copy(state = OperationState.FAILED, error = "Compress will be implemented in Phase 12")
        }
    }

    private suspend fun executeExtract(op: FileOperation) {
        updateOp(op.id) {
            it.copy(state = OperationState.FAILED, error = "Extract will be implemented in Phase 12")
        }
    }

    // ───────────── Conflict handling ─────────────

    private suspend fun handleConflict(
        op: FileOperation,
        source: FFile,
        destination: FPath,
    ): ConflictResolution? {
        val resolution = op.conflictResolution
        if (resolution == ConflictResolution.ASK) {
            // Pause op and surface conflict to UI
            updateOp(op.id) { it.copy(state = OperationState.AWAITING_CONFLICT) }
            // In a real implementation we'd push to a queue and suspend until resolved.
            // For now, default to SKIP.
            Logger.i(LogTags.FILE_OPERATION, "Conflict on ${destination.displayPath}, defaulting to SKIP")
            return null
        }
        return resolution
    }

    private fun resolveName(source: FFile, resolution: ConflictResolution): String {
        return when (resolution) {
            ConflictResolution.KEEP_BOTH -> {
                val dot = source.name.lastIndexOf('.')
                if (dot > 0) {
                    "${source.name.substring(0, dot)} (copy)${source.name.substring(dot)}"
                } else {
                    "${source.name} (copy)"
                }
            }
            else -> source.name
        }
    }

    override suspend fun resolveConflict(
        id: String,
        resolution: ConflictResolution,
    ): Result<Unit> = resultOf {
        updateOp(id) { it.copy(conflictResolution = resolution, state = OperationState.RUNNING) }
        // Resume: simply unblock (the suspended handleConflict would re-check)
        pausedFlags.remove(id)
    }

    // ───────────── Pause / Resume / Cancel ─────────────

    override suspend fun pause(id: String): Result<Unit> = resultOf {
        pausedFlags[id] = true
        updateOp(id) { it.copy(state = OperationState.PAUSED) }
        runningJobs[id]?.cancel()
        Logger.i(LogTags.FILE_OPERATION, "Paused $id")
    }

    override suspend fun resume(id: String): Result<Unit> = resultOf {
        pausedFlags.remove(id)
        val op = operations[id] ?: return@resultOf
        if (op.state == OperationState.PAUSED) {
            updateOp(id) { it.copy(state = OperationState.RUNNING) }
            // Re-launch — in a full implementation we'd checkpoint progress.
            val job = scope.launch {
                try {
                    executeOperation(op)
                } catch (e: CancellationException) {
                    updateOp(id) { it.copy(state = OperationState.CANCELLED) }
                } catch (e: Throwable) {
                    updateOp(id) {
                        it.copy(state = OperationState.FAILED, error = e.message ?: "Unknown")
                    }
                } finally {
                    runningJobs.remove(id)
                }
            }
            runningJobs[id] = job
        }
    }

    override suspend fun cancel(id: String): Result<Unit> = resultOf {
        cancelledFlags[id] = true
        runningJobs[id]?.cancel()
        runningJobs.remove(id)
        updateOp(id) { it.copy(state = OperationState.CANCELLED) }
        Logger.i(LogTags.FILE_OPERATION, "Cancelled $id")
    }

    override suspend fun retry(id: String): Result<Unit> = resultOf {
        val op = operations[id] ?: return@resultOf
        updateOp(id) {
            it.copy(
                state = OperationState.QUEUED,
                progress = 0f,
                bytesTransferred = 0,
                error = null,
            )
        }
        registerAndStart(op)
    }

    override suspend fun getActiveOperationsCount(): Int =
        operations.values.count {
            it.state == OperationState.RUNNING || it.state == OperationState.QUEUED
        }

    override suspend fun getPendingConflicts(): List<FileConflict> = _pendingConflicts.value

    // ───────────── Helpers ─────────────

    private suspend fun computeTotalSize(paths: List<FPath>): Long {
        var total = 0L
        for (p in paths) {
            val f = fileRepository.stat(p).getOrNull() ?: continue
            total += if (f.isDirectory) {
                fileRepository.computeDirectorySize(p).getOrNull() ?: 0L
            } else {
                f.size
            }
        }
        return total
    }

    private fun updateProgress(id: String, done: Long, total: Long) {
        val fraction = if (total == 0L) 1f else (done.toFloat() / total).coerceIn(0f, 1f)
        updateOp(id) {
            it.copy(
                bytesTransferred = done,
                totalBytes = total,
                progress = fraction,
            )
        }
    }

    private fun checkCancelled(id: String) {
        if (cancelledFlags[id] == true) {
            throw CancellationException("Operation $id cancelled")
        }
    }

    /** Shut down the engine — used by tests and Application.onTerminate. */
    fun shutdown() {
        runningJobs.values.forEach { it.cancel() }
        scope.cancel()
    }
}
