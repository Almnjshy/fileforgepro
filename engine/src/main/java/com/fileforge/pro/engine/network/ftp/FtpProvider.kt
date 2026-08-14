package com.fileforge.pro.engine.network.ftp

import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.core.storage.StorageProvider
import com.fileforge.pro.core.storage.StorageStats
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.model.StorageSource
import com.fileforge.pro.domain.model.StorageSourceKind
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.onFailure
import com.fileforge.pro.domain.result.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Connection parameters for an FTP server.
 */
data class FtpConnection(
    val id: String,
    val displayName: String,
    val host: String,
    val port: Int = 21,
    val username: String = "anonymous",
    val password: String = "",
    val isPassive: Boolean = true,
    val encoding: String = "UTF-8",
)

/**
 * [StorageProvider] backed by an FTP server (Master Spec §51 — Network Storage).
 */
class FtpProvider(
    override val sourceId: String,
    private val connection: FtpConnection,
) : StorageProvider {

    override val source: StorageSource = StorageSource(
        id = sourceId,
        kind = StorageSourceKind.FTP,
        name = connection.displayName,
        description = "${connection.host}:${connection.port}",
        isWritable = true,
        isAvailable = true,
    )

    private fun connect(): FTPClient = FTPClient().apply {
        controlEncoding = connection.encoding
        connect(connection.host, connection.port)
        login(connection.username, connection.password)
        if (connection.isPassive) enterLocalPassiveMode()
        setFileType(FTPClient.BINARY_FILE_TYPE)
    }

    private fun FTPClient.safeDisconnect() {
        try {
            if (isConnected) { logout(); disconnect() }
        } catch (_: Exception) {}
    }

    override suspend fun list(path: FPath): Result<List<FFile>> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                val remotePath = path.displayPath.ifEmpty { "/" }
                client.listFiles(remotePath).map { it.toFFile(path) }
            } finally { client.safeDisconnect() }
        }.onFailure { Logger.w(LogTags.NETWORK, "FTP list failed: ${it.code}", it.cause) }
    }

    override suspend fun stat(path: FPath): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                val parent = path.parent
                val parentPath = parent?.displayPath ?: "/"
                val files = client.listFiles(parentPath)
                val match = files.firstOrNull { it.name == path.name }
                    ?: throw java.io.FileNotFoundException(path.displayPath)
                match.toFFile(parent ?: path)
            } finally { client.safeDisconnect() }
        }
    }

    override suspend fun exists(path: FPath): Boolean = withContext(Dispatchers.IO) {
        val client = connect()
        try { client.listFiles(path.displayPath.ifEmpty { "/" }).isNotEmpty() }
        catch (e: Exception) { false }
        finally { client.safeDisconnect() }
    }

    override suspend fun createDirectory(parent: FPath, name: String): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                val newPath = parent / name
                if (!client.makeDirectory(newPath.displayPath))
                    throw java.io.IOException("mkdir failed: ${client.replyString}")
                FFile(path = newPath, name = name, isDirectory = true, size = 0,
                    lastModified = System.currentTimeMillis(), fileType = FileType.FOLDER)
            } finally { client.safeDisconnect() }
        }
    }

    override suspend fun createFile(parent: FPath, name: String): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                val newPath = parent / name
                client.storeFile(newPath.displayPath, ByteArrayInputStream(ByteArray(0)))
                FFile(path = newPath, name = name, isDirectory = false, size = 0,
                    lastModified = System.currentTimeMillis(), fileType = FileType.OTHER,
                    extension = name.substringAfterLast('.', "").ifEmpty { null })
            } finally { client.safeDisconnect() }
        }
    }

    override suspend fun rename(path: FPath, newName: String): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                val newPath = path.parent?.let { it / newName } ?: path
                if (!client.rename(path.displayPath, newPath.displayPath))
                    throw java.io.IOException("rename failed: ${client.replyString}")
                FFile(path = newPath, name = newName, isDirectory = false, size = 0,
                    lastModified = System.currentTimeMillis())
            } finally { client.safeDisconnect() }
        }
    }

    override suspend fun delete(path: FPath): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                val ok = client.deleteFile(path.displayPath) || client.removeDirectory(path.displayPath)
                if (!ok) throw java.io.IOException("delete failed: ${client.replyString}")
            } finally { client.safeDisconnect() }
        }
    }

    override suspend fun copy(source: FPath, destination: FPath): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                val out = ByteArrayOutputStream()
                client.retrieveFile(source.displayPath, out)
                val bytes = out.toByteArray()
                client.storeFile(destination.displayPath, ByteArrayInputStream(bytes))
                FFile(path = destination, name = destination.name, isDirectory = false,
                    size = bytes.size.toLong(), lastModified = System.currentTimeMillis())
            } finally { client.safeDisconnect() }
        }
    }

    override suspend fun move(source: FPath, destination: FPath): Result<FFile> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                if (!client.rename(source.displayPath, destination.displayPath))
                    throw java.io.IOException("move failed: ${client.replyString}")
                FFile(path = destination, name = destination.name, isDirectory = false, size = 0,
                    lastModified = System.currentTimeMillis())
            } finally { client.safeDisconnect() }
        }
    }

    override suspend fun openInputStream(path: FPath): Result<InputStream> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try {
                val out = ByteArrayOutputStream()
                if (!client.retrieveFile(path.displayPath, out))
                    throw java.io.IOException("retrieve failed: ${client.replyString}")
                client.safeDisconnect()
                out.toByteArray().inputStream()
            } catch (e: Exception) { client.safeDisconnect(); throw e }
        }
    }

    override suspend fun openOutputStream(path: FPath): Result<OutputStream> = withContext(Dispatchers.IO) {
        resultOf { BufferingUploadStream(this@FtpProvider, path) }
    }

    override suspend fun computeDirectorySize(path: FPath): Result<Long> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try { sumDirectorySize(client, path.displayPath.ifEmpty { "/" }) }
            finally { client.safeDisconnect() }
        }
    }

    override suspend fun countChildren(path: FPath): Result<Int> = withContext(Dispatchers.IO) {
        resultOf {
            val client = connect()
            try { client.listFiles(path.displayPath.ifEmpty { "/" }).size }
            finally { client.safeDisconnect() }
        }
    }

    override suspend fun queryStorageStats(): StorageStats? = null

    private suspend fun sumDirectorySize(client: FTPClient, remotePath: String): Long {
        var total = 0L
        val files = client.listFiles(remotePath)
        for (file in files) {
            if (file.isDirectory) {
                if (file.name != "." && file.name != "..")
                    total += sumDirectorySize(client, "$remotePath/${file.name}".replace("//", "/"))
            } else total += file.size
        }
        return total
    }

    private fun FTPFile.toFFile(parent: FPath): FFile {
        val name = this.name
        val path = parent / name
        val isDir = isDirectory
        val ext = if (isDir) null else name.substringAfterLast('.', "").ifEmpty { null }
        return FFile(
            path = path, name = name, isDirectory = isDir,
            size = if (isDir) 0 else size,
            lastModified = timestamp?.timeInMillis ?: System.currentTimeMillis(),
            fileType = if (isDir) FileType.FOLDER
            else com.fileforge.pro.core.filesystem.FileTypeDetector.detectByExtension(ext),
            extension = ext,
        )
    }

    private class BufferingUploadStream(
        private val provider: FtpProvider,
        private val path: FPath,
    ) : OutputStream() {
        private val buffer = ByteArrayOutputStream()
        override fun write(b: Int) { buffer.write(b) }
        override fun write(b: ByteArray, off: Int, len: Int) { buffer.write(b, off, len) }
        override fun close() {
            val bytes = buffer.toByteArray()
            kotlinx.coroutines.runBlocking {
                withContext(Dispatchers.IO) {
                    val client = provider.connect()
                    try {
                        client.storeFile(path.displayPath, ByteArrayInputStream(bytes))
                    } finally {
                        try {
                            if (client.isConnected) { client.logout(); client.disconnect() }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}
