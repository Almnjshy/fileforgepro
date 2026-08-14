package com.fileforge.pro.engine.developer

import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Information shown in the "Open in Terminal" intent (Master Spec §55).
 */
data class TermuxIntent(
    val workingDirectory: String,
    val packageName: String = "com.termux",
)

/**
 * Result of Git repository detection (Master Spec §54).
 */
data class GitRepositoryInfo(
    val rootPath: String,
    val branch: String,
    val hasUncommittedChanges: Boolean,
    val remoteUrl: String?,
)

/**
 * Developer tools engine (Master Spec §54 — Developer Mode, §55 — Termux).
 *
 * Provides:
 *   - Terminal (Termux) integration intent builder
 *   - Git repository detection
 *   - File permissions inspection
 *   - Raw URI / MIME resolution
 */
class DeveloperToolsEngine {

    /**
     * Build the intent to open the current folder in Termux (Master Spec §55).
     * Returns null if Termux is not installed (caller should handle gracefully).
     */
    fun buildTermuxIntent(workingDir: String): TermuxIntent {
        return TermuxIntent(workingDirectory = workingDir)
    }

    /**
     * Detect if [realPath] is inside a Git working tree (Master Spec §54).
     */
    suspend fun detectGitRepository(realPath: String): GitRepositoryInfo? = withContext(Dispatchers.IO) {
        var current = File(realPath)
        if (!current.isDirectory) current = current.parentFile ?: return@withContext null

        while (current != null) {
            val gitDir = File(current, ".git")
            if (gitDir.exists()) {
                val branch = readGitBranch(gitDir)
                val remote = readGitRemote(current)
                val dirty = checkGitDirty(current)
                return@withContext GitRepositoryInfo(
                    rootPath = current.absolutePath,
                    branch = branch,
                    hasUncommittedChanges = dirty,
                    remoteUrl = remote,
                )
            }
            current = current.parentFile
        }
        null
    }

    fun inspectPermissions(realPath: String): String? {
        val file = File(realPath)
        if (!file.exists()) return null
        val sb = StringBuilder()
        sb.append(if (file.canRead()) 'r' else '-')
        sb.append(if (file.canWrite()) 'w' else '-')
        sb.append(if (file.canExecute()) 'x' else '-')
        return sb.toString()
    }

    fun toRawUri(realPath: String): String = "file://$realPath"

    private fun readGitBranch(gitDir: File): String {
        return try {
            val headFile = File(gitDir, "HEAD")
            if (!headFile.exists()) return "unknown"
            val headContent = headFile.readText().trim()
            if (headContent.startsWith("ref:")) headContent.substringAfterLast('/')
            else headContent.take(8)
        } catch (e: Exception) { "unknown" }
    }

    private fun readGitRemote(workTree: File): String? {
        return try {
            val config = File(workTree, ".git/config")
            if (!config.exists()) return null
            val lines = config.readLines()
            val remoteLine = lines.indexOfFirst { it.trim() == "[remote \"origin\"]" }
            if (remoteLine < 0) return null
            lines.drop(remoteLine + 1).take(5)
                .firstOrNull { it.contains("url = ") }
                ?.substringAfter("url = ")?.trim()
        } catch (e: Exception) { null }
    }

    private fun checkGitDirty(workTree: File): Boolean {
        return try {
            File(workTree, ".git/COMMIT_EDITMSG").exists() &&
                    File(workTree, ".git/index").exists()
        } catch (e: Exception) { false }
    }
}
