package com.fileforge.pro.engine.filesystem

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.fileforge.pro.domain.result.FileError
import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.onFailure
import com.fileforge.pro.domain.result.resultOf
import com.fileforge.pro.core.filesystem.FileBridge
import com.fileforge.pro.core.filesystem.FileTypeDetector
import com.fileforge.pro.core.storage.StorageProvider
import com.fileforge.pro.core.storage.StorageStats
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.model.StorageSource
import com.fileforge.pro.domain.model.StorageSourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * [StorageProvider] for the device's internal emulated storage
 * (/storage/emulated/0) and any directly-mounted volumes (SD/USB) reachable
 * via java.io.File.
 *
 * Master Spec §5, §6, §81. This provider handles "internal", "sdcard-X",
 * "usb-X" source IDs.
 */
class LocalFilesystemProvider(
    override val sourceId: String,
    private val rootPath: String,
    override val source: StorageSource,
) : StorageProvider {

    override suspend fun list(path: FPath): Result<List<FFile>> = withContext(Dispatchers.IO) {
        resultOf {
            val dir = FileBridge.toFile(rootPath, path)
            if (!dir.exists()) throw FileNotFoundException(dir.absolutePath)
            if (!dir.isDirectory) throw IOException("Not a directory: ${dir.absolutePath}")
            if (!dir.canRead()) throw SecurityException("Cannot read: ${dir.absolutePath}")

            val children = dir.listFiles()
                ?: throw IOException("Cannot list: ${dir.absolutePath}")

            children
                .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                .map { FileBridge.toFFile(sourceId, rootPath, it) }
        }.onFailure { Logger.w(LogTags.STORAGE, "list($path) failed: ${it.code}", it.cause) }
    }

    override suspend fun stat(path: FPath): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val file = FileBridge.toFile(rootPath, path)
            if (!file.exists()) throw FileNotFoundException(file.absolutePath)
            FileBridge.toFFile(sourceId, rootPath, file)
        }
    }

    override suspend fun exists(path: FPath): Boolean = withContext(Dispatchers.IO) {
        FileBridge.toFile(rootPath, path).exists()
    }

    override suspend fun createDirectory(parent: FPath, name: String): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            validateName(name)
            val parentDir = FileBridge.toFile(rootPath, parent)
            val newDir = File(parentDir, name)
            if (newDir.exists()) throw IOException("Already exists: ${newDir.absolutePath}")
            if (!parentDir.canWrite()) throw SecurityException("Read-only parent: ${parentDir.absolutePath}")
            if (!newDir.mkdir()) throw IOException("mkdir failed: ${newDir.absolutePath}")
            FileBridge.toFFile(sourceId, rootPath, newDir)
        }
    }

    override suspend fun createFile(parent: FPath, name: String): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            validateName(name)
            val parentDir = FileBridge.toFile(rootPath, parent)
            val newFile = File(parentDir, name)
            if (newFile.exists()) throw IOException("Already exists: ${newFile.absolutePath}")
            if (!parentDir.canWrite()) throw SecurityException("Read-only parent: ${parentDir.absolutePath}")
            if (!newFile.createNewFile()) throw IOException("createNewFile failed: ${newFile.absolutePath}")
            FileBridge.toFFile(sourceId, rootPath, newFile)
        }
    }

    override suspend fun rename(path: FPath, newName: String): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            validateName(newName)
            val src = FileBridge.toFile(rootPath, path)
            if (!src.exists()) throw FileNotFoundException(src.absolutePath)
            val dst = File(src.parentFile, newName)
            if (dst.exists()) throw IOException("Destination exists: ${dst.absolutePath}")
            if (!src.renameTo(dst)) throw IOException("rename failed: ${src.absolutePath} → ${dst.absolutePath}")
            FileBridge.toFFile(sourceId, rootPath, dst)
        }
    }

    override suspend fun delete(path: FPath): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            val file = FileBridge.toFile(rootPath, path)
            if (!file.exists()) throw FileNotFoundException(file.absolutePath)
            if (!deleteRecursive(file)) throw IOException("delete failed: ${file.absolutePath}")
        }
    }

    override suspend fun copy(source: FPath, destination: FPath): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val src = FileBridge.toFile(rootPath, source)
            val dst = FileBridge.toFile(rootPath, destination)
            if (!src.exists()) throw FileNotFoundException(src.absolutePath)
            if (dst.exists()) throw IOException("Destination exists: ${dst.absolutePath}")
            if (src.isDirectory) {
                dst.mkdirs()
                src.listFiles()?.forEach { child ->
                    val childSrc = source / child.name
                    val childDst = destination / child.name
                    if (!copyInternal(child, FileBridge.toFile(rootPath, childDst))) {
                        throw IOException("copy failed for ${child.absolutePath}")
                    }
                }
            } else {
                if (!copyInternal(src, dst)) throw IOException("copy failed: ${src.absolutePath}")
            }
            FileBridge.toFFile(this@LocalFilesystemProvider.sourceId, rootPath, dst)
        }
    }

    override suspend fun move(source: FPath, destination: FPath): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val src = FileBridge.toFile(rootPath, source)
            val dst = FileBridge.toFile(rootPath, destination)
            if (!src.exists()) throw FileNotFoundException(src.absolutePath)
            if (dst.exists()) throw IOException("Destination exists: ${dst.absolutePath}")
            if (!src.renameTo(dst)) {
                // Fallback: copy then delete (cross-filesystem move)
                copyInternal(src, dst)
                deleteRecursive(src)
            }
            FileBridge.toFFile(this@LocalFilesystemProvider.sourceId, rootPath, dst)
        }
    }

    override suspend fun openInputStream(path: FPath): Result<java.io.InputStream> = withContext(Dispatchers.IO) {
        resultOf {
            val file = FileBridge.toFile(rootPath, path)
            if (!file.exists()) throw FileNotFoundException(file.absolutePath)
            if (!file.canRead()) throw SecurityException("Cannot read: ${file.absolutePath}")
            file.inputStream()
        }
    }

    override suspend fun openOutputStream(path: FPath): Result<java.io.OutputStream> = withContext(Dispatchers.IO) {
        resultOf {
            val file = FileBridge.toFile(rootPath, path)
            if (file.exists() && !file.canWrite()) throw SecurityException("Cannot write: ${file.absolutePath}")
            if (!file.parentFile?.canWrite()!!) throw SecurityException("Cannot write to parent")
            file.outputStream()
        }
    }

    override suspend fun computeDirectorySize(path: FPath): Result<Long> = withContext(Dispatchers.IO) {
        resultOf {
            val dir = FileBridge.toFile(rootPath, path)
            if (!dir.exists()) throw FileNotFoundException(dir.absolutePath)
            sizeRecursive(dir)
        }
    }

    override suspend fun countChildren(path: FPath): Result<Int> = withContext(Dispatchers.IO) {
        resultOf {
            val dir = FileBridge.toFile(rootPath, path)
            if (!dir.exists()) throw FileNotFoundException(dir.absolutePath)
            if (!dir.isDirectory) return@resultOf 0
            dir.list()?.size ?: 0
        }
    }

    override suspend fun queryStorageStats(): StorageStats? = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(rootPath)
            StorageStats(
                totalBytes = stat.totalBytes,
                freeBytes = stat.availableBytes,
            )
        } catch (e: Exception) {
            Logger.w(LogTags.STORAGE, "queryStorageStats failed for $rootPath", e)
            null
        }
    }

    // ---- helpers ----

    private fun deleteRecursive(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        return file.delete()
    }

    private fun copyInternal(src: File, dst: File): Boolean {
        if (src.isDirectory) {
            if (!dst.mkdirs() && !dst.exists()) return false
            src.listFiles()?.forEach { child ->
                val childDst = File(dst, child.name)
                if (!copyInternal(child, childDst)) return false
            }
            return true
        }
        return try {
            src.inputStream().use { input ->
                dst.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (e: IOException) {
            Logger.w(LogTags.FILE_OPERATION, "copyInternal failed: ${src.absolutePath}", e)
            false
        }
    }

    private fun sizeRecursive(file: File): Long {
        if (file.isFile) return file.length()
        var total = 0L
        file.listFiles()?.forEach { total += sizeRecursive(it) }
        return total
    }

    private fun validateName(name: String) {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(name.length <= 255) { "Name too long" }
        require(!name.contains('/')) { "Name cannot contain '/'" }
        require(!name.contains('\u0000')) { "Name cannot contain null byte" }
        require(name != "." && name != "..") { "Name cannot be . or .." }
    }

    companion object {

        /**
         * Create the [LocalFilesystemProvider] for the device's primary
         * external storage (/storage/emulated/0).
         */
        fun forInternal(context: Context): LocalFilesystemProvider {
            val rootPath = Environment.getExternalStorageDirectory().absolutePath
            val source = StorageSource(
                id = "internal",
                kind = StorageSourceKind.INTERNAL,
                name = "Internal Storage",
                description = "Primary device storage",
                isWritable = true,
                isAvailable = true,
            )
            return LocalFilesystemProvider("internal", rootPath, source)
        }

        /**
         * Probe for additional mounted volumes (SD card, USB OTG).
         * Returns one provider per readable mount under /storage.
         */
        fun probeRemovable(context: Context): List<LocalFilesystemProvider> {
            val result = mutableListOf<LocalFilesystemProvider>()
            // Use Android's StorageManager via reflection-free path:
            // /storage/<volume-id> directories.
            val storage = File("/storage")
            if (!storage.exists() || !storage.isDirectory) return result

            storage.listFiles()?.forEach { dir ->
                if (dir.isDirectory && dir.canRead() && dir.name != "emulated" && dir.name != "self") {
                    val id = when {
                        dir.name.startsWith("sdcard") || dir.name.contains("sd", ignoreCase = true) ->
                            "sd-${dir.name}"
                        dir.name.contains("usb", ignoreCase = true) -> "usb-${dir.name}"
                        else -> "vol-${dir.name}"
                    }
                    val kind = if (dir.name.contains("usb", ignoreCase = true)) {
                        StorageSourceKind.USB_OTG
                    } else {
                        StorageSourceKind.SD_CARD
                    }
                    val source = StorageSource(
                        id = id,
                        kind = kind,
                        name = if (kind == StorageSourceKind.USB_OTG) "USB Storage" else "SD Card",
                        description = dir.absolutePath,
                        isWritable = dir.canWrite(),
                        isAvailable = true,
                    )
                    result.add(LocalFilesystemProvider(id, dir.absolutePath, source))
                }
            }
            return result
        }
    }
}
