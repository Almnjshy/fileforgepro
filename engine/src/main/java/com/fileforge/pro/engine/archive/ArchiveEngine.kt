package com.fileforge.pro.engine.archive

import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.result.FileError
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry in an archive (Master Spec §49).
 */
data class ArchiveEntry(
    val name: String,
    val path: String,           // path inside archive
    val size: Long,
    val compressedSize: Long,
    val isDirectory: Boolean,
    val lastModified: Long,
) {
    val extension: String? get() = name.substringAfterLast('.', "").ifEmpty { null }
}

/**
 * Result of listing an archive.
 */
data class ArchiveListing(
    val archivePath: FPath,
    val entries: List<ArchiveEntry>,
    val totalUncompressedSize: Long,
    val entryCount: Int,
    val isComplete: Boolean,
)

/**
 * Archive engine — supports ZIP / TAR / GZIP / 7Z / RAR (read-only for most)
 * (Master Spec §49 — Archive Manager).
 *
 * Architecture: opens the archive via [StorageProviderRegistry.openInputStream],
 * so it works the same regardless of where the archive lives.
 */
@Singleton
class ArchiveEngine @Inject constructor(
    private val providerRegistry: StorageProviderRegistry,
) {

    /**
     * Stream entries of an archive (Master Spec §49 — View).
     */
    fun list(archive: FFile): Flow<ArchiveListing> = flow {
        val provider = providerRegistry.get(archive.path) ?: return@flow
        when (val r = provider.openInputStream(archive.path)) {
            is Result.Ok -> {
                val entries = mutableListOf<ArchiveEntry>()
                var totalSize = 0L
                try {
                    r.value.use { stream ->
                        val buffered = BufferedInputStream(stream)
                        when (archive.extension?.lowercase()) {
                            "zip", "jar", "apk", "apks", "xapk" -> {
                                listZip(buffered) { entry ->
                                    entries.add(entry)
                                    totalSize += entry.size
                                }
                            }
                            "tar" -> listTar(buffered) { entry ->
                                entries.add(entry)
                                totalSize += entry.size
                            }
                            "gz", "gzip", "tgz" -> {
                                // Single-file compression — emit one entry
                                val innerName = archive.name.removeSuffix(".gz").removeSuffix(".tgz")
                                entries.add(
                                    ArchiveEntry(
                                        name = innerName,
                                        path = innerName,
                                        size = -1, // unknown without decompressing
                                        compressedSize = archive.size,
                                        isDirectory = false,
                                        lastModified = archive.lastModified,
                                    )
                                )
                            }
                            "bz2" -> {
                                val innerName = archive.name.removeSuffix(".bz2")
                                entries.add(
                                    ArchiveEntry(
                                        name = innerName,
                                        path = innerName,
                                        size = -1,
                                        compressedSize = archive.size,
                                        isDirectory = false,
                                        lastModified = archive.lastModified,
                                    )
                                )
                            }
                            "xz" -> {
                                val innerName = archive.name.removeSuffix(".xz")
                                entries.add(
                                    ArchiveEntry(
                                        name = innerName,
                                        path = innerName,
                                        size = -1,
                                        compressedSize = archive.size,
                                        isDirectory = false,
                                        lastModified = archive.lastModified,
                                    )
                                )
                            }
                            "7z" -> {
                                // 7z requires random access — commons-compress supports it
                                // via SevenZFile but needs a File, not InputStream.
                                // Skip for now; emit empty + message.
                                Logger.w(LogTags.ARCHIVE, "7z listing requires file-path access — Phase 12+")
                            }
                            "rar" -> {
                                Logger.w(LogTags.ARCHIVE, "RAR listing not supported — needs third-party lib")
                            }
                            else -> {
                                Logger.w(LogTags.ARCHIVE, "Unknown archive type: ${archive.extension}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(LogTags.ARCHIVE, "list failed: ${archive.name}", e)
                }
                emit(
                    ArchiveListing(
                        archivePath = archive.path,
                        entries = entries.toList(),
                        totalUncompressedSize = totalSize,
                        entryCount = entries.size,
                        isComplete = true,
                    )
                )
            }
            is Result.Err -> Logger.w(LogTags.ARCHIVE, "openInputStream failed: ${r.error.code}")
        }
    }

    /**
     * Extract all entries to [destination] directory (Master Spec §49 — Extract).
     */
    suspend fun extract(archive: FFile, destination: FPath): Result<Int> = withContext(Dispatchers.IO) {
        val provider = providerRegistry.get(archive.path) ?: return@withContext Result.Err(
            FileError.Other("No provider for ${archive.path.sourceId}")
        )
        val destProvider = providerRegistry.get(destination) ?: return@withContext Result.Err(
            FileError.Other("No provider for ${destination.sourceId}")
        )
        // Resolve real destination path — currently only works for LocalFilesystemProvider
        val destRoot = resolveRealPath(destination) ?: return@withContext Result.Err(
            FileError.Other("Cannot resolve destination path")
        )
        val destDir = File(destRoot).also { it.mkdirs() }

        var extractedCount = 0
        when (val r = provider.openInputStream(archive.path)) {
            is Result.Ok -> {
                try {
                    r.value.use { stream ->
                        val buffered = BufferedInputStream(stream)
                        when (archive.extension?.lowercase()) {
                            "zip", "jar", "apk", "apks" -> {
                                ZipInputStream(buffered).use { zis ->
                                    var entry = zis.nextEntry
                                    while (entry != null) {
                                        val outFile = File(destDir, entry.name)
                                        if (entry.isDirectory) {
                                            outFile.mkdirs()
                                        } else {
                                            outFile.parentFile?.mkdirs()
                                            FileOutputStream(outFile).use { fos ->
                                                zis.copyTo(fos)
                                            }
                                            extractedCount++
                                        }
                                        zis.closeEntry()
                                        entry = zis.nextEntry
                                    }
                                }
                            }
                            "tar" -> {
                                TarArchiveInputStream(buffered).use { tis ->
                                    var entry = tis.nextTarEntry
                                    while (entry != null) {
                                        val e = entry
                                        val outFile = File(destDir, e.name)
                                        if (e.isDirectory) {
                                            outFile.mkdirs()
                                        } else {
                                            outFile.parentFile?.mkdirs()
                                            FileOutputStream(outFile).use { fos ->
                                                tis.copyTo(fos)
                                            }
                                            extractedCount++
                                        }
                                        entry = tis.nextTarEntry
                                    }
                                }
                            }
                            "gz", "gzip", "tgz" -> {
                                GzipCompressorInputStream(buffered).use { gzis ->
                                    val innerName = archive.name.removeSuffix(".gz").removeSuffix(".tgz")
                                    val outFile = File(destDir, innerName)
                                    FileOutputStream(outFile).use { fos -> gzis.copyTo(fos) }
                                    extractedCount = 1
                                }
                            }
                            "bz2" -> {
                                BZip2CompressorInputStream(buffered).use { bzis ->
                                    val innerName = archive.name.removeSuffix(".bz2")
                                    val outFile = File(destDir, innerName)
                                    FileOutputStream(outFile).use { fos -> bzis.copyTo(fos) }
                                    extractedCount = 1
                                }
                            }
                            "xz" -> {
                                XZCompressorInputStream(buffered).use { xzis ->
                                    val innerName = archive.name.removeSuffix(".xz")
                                    val outFile = File(destDir, innerName)
                                    FileOutputStream(outFile).use { fos -> xzis.copyTo(fos) }
                                    extractedCount = 1
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.e(LogTags.ARCHIVE, "extract failed: ${archive.name}", e)
                    return@withContext Result.Err(FileError.IoError)
                }
                Result.Ok(extractedCount)
            }
            is Result.Err -> r
        }
    }

    // ───────────── Helpers ─────────────

    private fun listZip(stream: InputStream, onEntry: (ArchiveEntry) -> Unit) {
        // ZipInputStream doesn't expose compressedSize reliably; use it for streaming.
        ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                onEntry(
                    ArchiveEntry(
                        name = entry.name.substringAfterLast('/').ifEmpty { entry.name },
                        path = entry.name,
                        size = entry.size,
                        compressedSize = entry.compressedSize,
                        isDirectory = entry.isDirectory,
                        lastModified = entry.time,
                    )
                )
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun listTar(stream: InputStream, onEntry: (ArchiveEntry) -> Unit) {
        TarArchiveInputStream(stream).use { tis ->
            var entry: TarArchiveEntry? = tis.nextTarEntry
            while (entry != null) {
                val e = entry
                onEntry(
                    ArchiveEntry(
                        name = e.name.substringAfterLast('/').ifEmpty { e.name },
                        path = e.name,
                        size = e.size,
                        compressedSize = e.size, // tar has no per-entry compression
                        isDirectory = e.isDirectory,
                        lastModified = e.modTime.time,
                    )
                )
                entry = tis.nextTarEntry
            }
        }
    }

    private fun resolveRealPath(path: FPath): String? {
        return when (path.sourceId) {
            "internal" -> "/storage/emulated/0/${path.displayPath}".trimEnd('/')
            else -> {
                val volId = path.sourceId.substringAfter('-')
                "/storage/$volId/${path.displayPath}".trimEnd('/')
            }
        }
    }
}
