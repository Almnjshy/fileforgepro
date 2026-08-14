package com.fileforge.pro.core.security

import java.security.MessageDigest

/**
 * Hashing helpers for the Duplicate Finder (Master Spec §36).
 *
 * Strategy:
 *   1. Group files by exact size (fast, free).
 *   2. For groups with size > 1, hash first 4 KB (quick fingerprint).
 *   3. For groups still > 1 after step 2, hash the whole file (final proof).
 */
object FileHasher {

    /** SHA-256 of the entire file. */
    fun hashFull(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(bytes).toHex()
    }

    /** SHA-256 of the first 4KB only — fast fingerprint for grouping. */
    fun hashHead(bytes: ByteArray, headBytes: Int = 4096): String {
        val head = if (bytes.size > headBytes) bytes.copyOf(headBytes) else bytes
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(head).toHex()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(HEX[(b.toInt() ushr 4) and 0x0F])
            sb.append(HEX[b.toInt() and 0x0F])
        }
        return sb.toString()
    }

    private val HEX = "0123456789abcdef".toCharArray()
}
