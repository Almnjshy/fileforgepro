package com.fileforge.pro.domain.model

/**
 * Absolute, source-agnostic path inside a [StorageSource].
 *
 * A [FPath] is NOT a java.io.File — it carries the [sourceId] of the
 * [StorageSource] it belongs to, so the same logical "/Download/a.txt" can
 * exist in Internal Storage, USB, FTP, etc. without ambiguity.
 *
 * @param sourceId Identifier of the [StorageSource] (e.g. "internal", "usb-1", "ftp-1").
 * @param segments Path components from the source root. Empty list = root of the source.
 */
data class FPath(
    val sourceId: String,
    val segments: List<String>,
) {
    val name: String get() = segments.lastOrNull() ?: "/"
    val parent: FPath? get() = if (segments.isEmpty()) null else copy(segments = segments.dropLast(1))
    val depth: Int get() = segments.size

    val isRoot: Boolean get() = segments.isEmpty()

    /**
     * Display path with "/" separators. Used only for UI — NOT for I/O.
     * e.g. "Download/Projects/FileForgePro"
     */
    val displayPath: String
        get() = if (segments.isEmpty()) "/" else segments.joinToString("/")

    operator fun div(child: String): FPath = copy(segments = segments + child)

    fun child(name: String): FPath = this / name

    companion object {
        fun root(sourceId: String) = FPath(sourceId, emptyList())

        /**
         * Build a [FPath] from a Unix-style path string relative to the source root.
         * "/a/b/c" → segments ["a","b","c"]
         */
        fun fromString(sourceId: String, path: String): FPath {
            val cleaned = path.trim('/').split('/').filter { it.isNotEmpty() }
            return FPath(sourceId, cleaned)
        }
    }
}
