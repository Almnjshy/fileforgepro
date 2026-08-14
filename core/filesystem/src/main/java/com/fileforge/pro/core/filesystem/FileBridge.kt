package com.fileforge.pro.core.filesystem

import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.model.StorageSource
import com.fileforge.pro.domain.model.StorageSourceKind
import java.io.File
import java.io.IOException

/**
 * Bridges between java.io.File (the classic POSIX-style filesystem) and
 * our domain [FFile] / [FPath] abstractions.
 *
 * Used by [com.fileforge.pro.engine.filesystem.LocalFilesystemProvider] for
 * Internal Storage, SD Card, and USB OTG (when accessible via java.io).
 *
 * NOTE: This is the ONLY place in the codebase that converts java.io.File
 * to/from FFile. Everywhere else operates on FFile only.
 */
object FileBridge {

    /**
     * Convert a java.io.File to an [FFile].
     *
     * @param sourceId The storage source ID (e.g. "internal").
     * @param rootPath The absolute root path of the source (e.g. "/storage/emulated/0").
     * @param file The file to convert.
     */
    fun toFFile(sourceId: String, rootPath: String, file: File): FFile {
        val relativePath = file.absolutePath.removePrefix(rootPath).trim('/')
        val path = if (relativePath.isEmpty()) {
            FPath.root(sourceId)
        } else {
            FPath.fromString(sourceId, relativePath)
        }

        val name = file.name.ifEmpty { "/" }
        val ext = if (file.isDirectory) null else file.extension.takeIf { it.isNotEmpty() }

        return FFile(
            path = path,
            name = name,
            isDirectory = file.isDirectory,
            size = if (file.isFile) file.length() else 0L,
            lastModified = file.lastModified(),
            lastAccessed = file.lastModified(), // java.io.File doesn't expose lastAccess directly
            created = null,
            mimeType = MimeTypes.forExtension(ext),
            fileType = classify(file, ext),
            isHidden = name.startsWith("."),
            isReadable = file.canRead(),
            isWritable = file.canWrite(),
            isExecutable = file.canExecute(),
            extension = ext,
            itemCount = null,
        )
    }

    /**
     * Convert an [FPath] back to a java.io.File.
     * Caller must pass the source's absolute root path.
     */
    fun toFile(rootPath: String, path: FPath): File {
        return if (path.isRoot) File(rootPath)
        else File(rootPath, path.displayPath)
    }

    /**
     * Classify a file's [FileType] from its name + java.io.File.isDirectory.
     */
    fun classify(file: File, extension: String?): FileType {
        if (file.isDirectory) return FileType.FOLDER
        return FileTypeDetector.detectByExtension(extension)
    }
}
