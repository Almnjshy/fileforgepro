package com.fileforge.pro.engine.vault

import android.content.Context
import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.core.security.AesGcmCrypto
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.result.FileError
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Metadata for a file stored inside the vault.
 */
data class VaultEntry(
    val id: String,
    val originalName: String,
    val originalPath: String,
    val size: Long,
    val encryptedSize: Long,
    val addedAt: Long,
    val mimeType: String?,
) {
    val vaultFileName: String get() = "$id.enc"
}

/**
 * Secure Vault engine (Master Spec §53, §71).
 *
 * Implements REAL encryption using AES-256-GCM with PBKDF2 key derivation
 * (600,000 iterations, 128-bit salt). Each file is encrypted individually.
 */
@Singleton
class VaultEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val vaultDir: File by lazy {
        File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
    }

    private val indexFile: File by lazy { File(vaultDir, ".vault_index") }

    @Volatile
    private var unlockedKey: javax.crypto.SecretKey? = null

    @Volatile
    private var currentPassword: CharArray? = null

    val isUnlocked: Boolean get() = unlockedKey != null

    suspend fun unlock(password: CharArray): Boolean = withContext(Dispatchers.IO) {
        try {
            currentPassword = password.copyOf()
            if (!indexFile.exists()) {
                // First-time unlock: derive key + save empty index
                val salt = AesGcmCrypto.newSalt()
                unlockedKey = AesGcmCrypto.deriveKey(password, salt)
                saveIndex(emptyList(), salt)
                Logger.i(LogTags.VAULT, "Vault initialized")
                true
            } else {
                val bytes = indexFile.readBytes()
                val salt = bytes.copyOfRange(0, 16)
                val ivPlusCipher = bytes.copyOfRange(16, bytes.size)
                val candidateKey = AesGcmCrypto.deriveKey(password, salt)
                val decrypted = runCatching { AesGcmCrypto.decrypt(ivPlusCipher, candidateKey) }
                if (decrypted.isSuccess) {
                    unlockedKey = candidateKey
                    Logger.i(LogTags.VAULT, "Vault unlocked")
                    true
                } else {
                    currentPassword = null
                    Logger.w(LogTags.VAULT, "Wrong password")
                    false
                }
            }
        } catch (e: Exception) {
            Logger.e(LogTags.VAULT, "unlock error", e)
            false
        }
    }

    fun lock() {
        unlockedKey = null
        currentPassword?.fill('\u0000')
        currentPassword = null
        Logger.i(LogTags.VAULT, "Vault locked")
    }

    suspend fun listEntries(): Result<List<VaultEntry>> = withContext(Dispatchers.IO) {
        val key = unlockedKey ?: return@withContext Result.Err(FileError.Other("Vault is locked"))
        resultOf {
            if (!indexFile.exists()) return@resultOf emptyList()
            val bytes = indexFile.readBytes()
            val ivPlusCipher = bytes.copyOfRange(16, bytes.size)
            val decrypted = AesGcmCrypto.decrypt(ivPlusCipher, key)
            deserializeIndex(String(decrypted, Charsets.UTF_8))
        }
    }

    suspend fun addEntry(source: FFile): Result<VaultEntry> = withContext(Dispatchers.IO) {
        val key = unlockedKey ?: return@withContext Result.Err(FileError.Other("Vault is locked"))
        resultOf {
            val id = java.util.UUID.randomUUID().toString()
            val realPath = resolveRealPath(source) ?: throw java.io.IOException("Cannot resolve source path")
            val sourceFile = File(realPath)
            if (!sourceFile.exists()) throw java.io.FileNotFoundException(realPath)

            val encFile = File(vaultDir, "$id.enc")
            val plainBytes = sourceFile.readBytes()
            val encrypted = AesGcmCrypto.encrypt(plainBytes, key)
            encFile.writeBytes(encrypted)

            val entry = VaultEntry(
                id = id,
                originalName = source.name,
                originalPath = source.path.displayPath,
                size = plainBytes.size.toLong(),
                encryptedSize = encrypted.size.toLong(),
                addedAt = System.currentTimeMillis(),
                mimeType = source.mimeType,
            )
            val current = (listEntries() as? Result.Ok)?.value ?: emptyList()
            saveIndex(current + entry)
            Logger.i(LogTags.VAULT, "Added: ${entry.originalName}")
            entry
        }
    }

    suspend fun extractEntry(entryId: String, destinationDir: File): Result<File> = withContext(Dispatchers.IO) {
        val key = unlockedKey ?: return@withContext Result.Err(FileError.Other("Vault is locked"))
        resultOf {
            val entries = (listEntries() as? Result.Ok)?.value ?: emptyList()
            val entry = entries.firstOrNull { it.id == entryId }
                ?: throw java.io.FileNotFoundException("Entry $entryId not found")
            val encFile = File(vaultDir, entry.vaultFileName)
            if (!encFile.exists()) throw java.io.FileNotFoundException(encFile.absolutePath)
            val encBytes = encFile.readBytes()
            val plainBytes = AesGcmCrypto.decrypt(encBytes, key)
            val outFile = File(destinationDir, entry.originalName).also { it.parentFile?.mkdirs() }
            outFile.writeBytes(plainBytes)
            Logger.i(LogTags.VAULT, "Extracted: ${entry.originalName}")
            outFile
        }
    }

    suspend fun deleteEntry(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            val entries = (listEntries() as? Result.Ok)?.value ?: emptyList()
            val entry = entries.firstOrNull { it.id == entryId } ?: return@resultOf
            File(vaultDir, entry.vaultFileName).delete()
            saveIndex(entries.filterNot { it.id == entryId })
            Logger.i(LogTags.VAULT, "Deleted: $entryId")
        }
    }

    // ───────────── Helpers ─────────────

    private fun saveIndex(entries: List<VaultEntry>, salt: ByteArray? = null) {
        val useSalt = salt ?: if (indexFile.exists()) indexFile.readBytes().copyOfRange(0, 16) else AesGcmCrypto.newSalt()
        val key = unlockedKey ?: throw IllegalStateException("Vault not unlocked")
        val json = serializeIndex(entries)
        val plainBytes = json.toByteArray(Charsets.UTF_8)
        val encrypted = AesGcmCrypto.encrypt(plainBytes, key)
        indexFile.writeBytes(useSalt + encrypted)
    }

    private fun serializeIndex(entries: List<VaultEntry>): String {
        return entries.joinToString("\n") { e ->
            "${e.id}\t${e.originalName}\t${e.originalPath}\t${e.size}\t${e.encryptedSize}\t${e.addedAt}\t${e.mimeType ?: ""}"
        }
    }

    private fun deserializeIndex(text: String): List<VaultEntry> {
        if (text.isBlank()) return emptyList()
        return text.lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size < 7) return@mapNotNull null
            VaultEntry(
                id = parts[0],
                originalName = parts[1],
                originalPath = parts[2],
                size = parts[3].toLongOrNull() ?: 0,
                encryptedSize = parts[4].toLongOrNull() ?: 0,
                addedAt = parts[5].toLongOrNull() ?: 0,
                mimeType = parts[6].ifEmpty { null },
            )
        }
    }

    private fun resolveRealPath(file: FFile): String? {
        return when (file.path.sourceId) {
            "internal" -> "/storage/emulated/0/${file.path.displayPath}".trimEnd('/')
            else -> {
                val volId = file.path.sourceId.substringAfter('-')
                "/storage/$volId/${file.path.displayPath}".trimEnd('/')
            }
        }
    }
}
