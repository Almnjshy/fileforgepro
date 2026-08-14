package com.fileforge.pro.domain.result

/**
 * Structured file-operation errors (Master Spec §69).
 *
 * Lives in :domain so domain interfaces (FileRepository, etc.) can reference
 * it without depending on any Android module.
 *
 * Each error carries a [code] for programmatic handling and a [message]
 * that is safe to show to the user. The [cause] is the original throwable
 * for debugging (NEVER shown to the user as-is).
 */
sealed class FileError(
    val code: String,
    val message: String,
    val cause: Throwable? = null,
) {
    data object PermissionDenied : FileError("PERMISSION_DENIED", "Permission denied")
    data object FileNotFound : FileError("FILE_NOT_FOUND", "File or folder not found")
    data object FileExists : FileError("FILE_EXISTS", "A file with this name already exists")
    data object ReadOnly : FileError("READ_ONLY", "This location is read-only")
    data object StorageUnavailable : FileError("STORAGE_UNAVAILABLE", "Storage is not available")
    data object InsufficientStorage : FileError("INSUFFICIENT_STORAGE", "Not enough free space")
    data object IoError : FileError("IO_ERROR", "I/O error")
    data object InvalidArchive : FileError("INVALID_ARCHIVE", "Archive is corrupt or unsupported")
    data object NetworkError : FileError("NETWORK_ERROR", "Network error")
    data object InvalidName : FileError("INVALID_NAME", "Invalid file or folder name")
    data object OperationCancelled : FileError("CANCELLED", "Operation was cancelled")

    data class Other(
        val customMessage: String,
        val throwable: Throwable? = null,
    ) : FileError("OTHER", customMessage, throwable)

    companion object {
        fun fromException(e: Throwable): FileError = when (e) {
            is java.io.FileNotFoundException -> FileNotFound
            is SecurityException -> PermissionDenied
            is java.io.IOException -> when {
                e.message?.contains("space", ignoreCase = true) == true -> InsufficientStorage
                e.message?.contains("EROFS", ignoreCase = true) == true -> ReadOnly
                else -> IoError
            }
            is IllegalArgumentException, is IllegalStateException -> InvalidName
            else -> Other(e.message ?: "Unknown error", e)
        }
    }
}
