package com.fileforge.pro.domain.model

/**
 * A queued or running file operation (Master Spec §26 — File Operations Engine).
 */
data class FileOperation(
    val id: String,
    val kind: FileOperationKind,
    val sources: List<FPath>,
    val destination: FPath?,
    val createdAt: Long,
    val state: OperationState = OperationState.QUEUED,
    val progress: Float = 0f,         // 0..1
    val bytesTransferred: Long = 0,
    val totalBytes: Long = 0,
    val speedBytesPerSec: Long = 0,
    val etaSeconds: Long = -1,
    val error: String? = null,
    val conflictResolution: ConflictResolution = ConflictResolution.ASK,
)

enum class FileOperationKind {
    COPY, MOVE, DELETE, RENAME, CREATE_FOLDER, CREATE_FILE, COMPRESS, EXTRACT
}

enum class OperationState {
    QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED, AWAITING_CONFLICT
}

enum class ConflictResolution {
    ASK, REPLACE, KEEP_BOTH, SKIP, RENAME
}

/**
 * Conflict descriptor surfaced when a destination file already exists
 * (Master Spec §28 — File Conflict Manager).
 */
data class FileConflict(
    val source: FFile,
    val destination: FFile,
)
