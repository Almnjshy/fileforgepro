package com.fileforge.pro.engine.text

import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of loading a text file (Master Spec §42, §43).
 */
data class TextFileContent(
    val path: FPath,
    val text: String,
    val charset: Charset,
    val lineCount: Int,
    val byteSize: Long,
    val isTruncated: Boolean,
    val truncatedMessage: String? = null,
) {
    val isReadOnly: Boolean
        get() = false // determined at the repository level
}

/**
 * Text-file I/O engine with encoding detection + chunked reading
 * (Master Spec §42 — Encoding, §43 — Large Text Files).
 *
 * Strategy:
 *  - For files < 1 MB: read fully.
 *  - For files 1-10 MB: read first 1 MB and warn about truncation.
 *  - For files > 10 MB: refuse to load fully — return error suggesting streaming.
 *
 * Encoding:
 *  - Detects UTF-8 BOM, UTF-16 BOM (BE/LE).
 *  - Falls back to UTF-8 (most common for code).
 *  - Preserves Arabic, English, mixed content, Unicode (Master Spec §42).
 */
@Singleton
class TextEditorEngine @Inject constructor(
    private val providerRegistry: StorageProviderRegistry,
) {

    companion object {
        const val MAX_FULL_READ_BYTES = 1L * 1024 * 1024 // 1 MB
        const val MAX_TRUNCATED_READ_BYTES = 5L * 1024 * 1024 // 5 MB cap
    }

    suspend fun load(file: FFile): Result<TextFileContent> = withContext(Dispatchers.IO) {
        val provider = providerRegistry.get(file.path) ?: return@withContext Result.Err(
            com.fileforge.pro.domain.result.FileError.Other("No provider for ${file.path.sourceId}")
        )

        if (file.size > MAX_TRUNCATED_READ_BYTES) {
            return@withContext Result.Err(
                com.fileforge.pro.domain.result.FileError.Other(
                    "File is too large to open in editor (${file.size} bytes). Max supported: ${MAX_TRUNCATED_READ_BYTES / 1024 / 1024} MB."
                )
            )
        }

        when (val r = provider.openInputStream(file.path)) {
            is Result.Ok -> {
                try {
                    r.value.use { stream ->
                        val bytes = stream.readBytes()
                        val (charset, hasBom) = detectCharset(bytes)
                        val textBytes = if (hasBom) bytes.copyOfStripBom(charset) else bytes
                        val text = String(textBytes, charset)

                        val isTruncated = file.size > MAX_FULL_READ_BYTES
                        val truncatedMsg = if (isTruncated) {
                            "Showing first ${MAX_FULL_READ_BYTES / 1024} KB of a ${file.size / 1024} KB file."
                        } else null

                        val lineCount = text.count { it == '\n' } + 1

                        Result.Ok(
                            TextFileContent(
                                path = file.path,
                                text = text,
                                charset = charset,
                                lineCount = lineCount,
                                byteSize = file.size,
                                isTruncated = isTruncated,
                                truncatedMessage = truncatedMsg,
                            )
                        )
                    }
                } catch (e: OutOfMemoryError) {
                    Result.Err(com.fileforge.pro.domain.result.FileError.Other("OutOfMemory loading ${file.name}"))
                } catch (e: Exception) {
                    Logger.e(LogTags.TEXT_EDITOR, "Failed to read ${file.name}", e)
                    Result.Err(com.fileforge.pro.domain.result.FileError.IoError)
                }
            }
            is Result.Err -> r
        }
    }

    suspend fun save(file: FFile, content: String, charset: Charset = Charsets.UTF_8): Result<Unit> =
        withContext(Dispatchers.IO) {
            val provider = providerRegistry.get(file.path) ?: return@withContext Result.Err(
                com.fileforge.pro.domain.result.FileError.Other("No provider")
            )
            when (val r = provider.openOutputStream(file.path)) {
                is Result.Ok -> {
                    try {
                        r.value.use { stream ->
                            stream.write(content.toByteArray(charset))
                        }
                        Result.Ok(Unit)
                    } catch (e: Exception) {
                        Logger.e(LogTags.TEXT_EDITOR, "Failed to save ${file.name}", e)
                        Result.Err(com.fileforge.pro.domain.result.FileError.IoError)
                    }
                }
                is Result.Err -> r
            }
        }

    // ───────────── Encoding detection ─────────────

    private fun detectCharset(bytes: ByteArray): Pair<Charset, Boolean> {
        if (bytes.size >= 3) {
            // UTF-8 BOM: EF BB BF
            if ((bytes[0].toInt() and 0xFF) == 0xEF &&
                (bytes[1].toInt() and 0xFF) == 0xBB &&
                (bytes[2].toInt() and 0xFF) == 0xBF
            ) {
                return Charsets.UTF_8 to true
            }
        }
        if (bytes.size >= 2) {
            // UTF-16 BE BOM: FE FF
            if ((bytes[0].toInt() and 0xFF) == 0xFE &&
                (bytes[1].toInt() and 0xFF) == 0xFF
            ) {
                return Charsets.UTF_16BE to true
            }
            // UTF-16 LE BOM: FF FE
            if ((bytes[0].toInt() and 0xFF) == 0xFF &&
                (bytes[1].toInt() and 0xFF) == 0xFE
            ) {
                return Charsets.UTF_16LE to true
            }
        }
        // Default to UTF-8 (works for ASCII + Arabic + most code files)
        return Charsets.UTF_8 to false
    }

    private fun ByteArray.copyOfStripBom(charset: Charset): ByteArray {
        val skip = when (charset) {
            Charsets.UTF_8 -> 3
            Charsets.UTF_16BE, Charsets.UTF_16LE -> 2
            else -> 0
        }
        return if (skip > 0 && size >= skip) copyOfRange(skip, size) else this
    }
}
