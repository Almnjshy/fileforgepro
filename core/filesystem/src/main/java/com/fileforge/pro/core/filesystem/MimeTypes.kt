package com.fileforge.pro.core.filesystem

/**
 * Minimal extension → MIME map. Falls back to Android's MimeTypeMap when
 * available (via ContentResolver), but this table covers the common cases
 * for fast offline detection.
 */
object MimeTypes {

    private val EXT_TO_MIME = mapOf(
        // Text
        "txt" to "text/plain",
        "md" to "text/markdown",
        "markdown" to "text/markdown",
        "csv" to "text/csv",
        "tsv" to "text/tab-separated-values",
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "jsx" to "application/javascript",
        "ts" to "application/typescript",
        "tsx" to "application/typescript",
        "json" to "application/json",
        "json5" to "application/json5",
        "xml" to "application/xml",
        "yaml" to "application/yaml",
        "yml" to "application/yaml",
        "toml" to "application/toml",
        "kt" to "text/x-kotlin",
        "kts" to "text/x-kotlin",
        "java" to "text/x-java-source",
        "py" to "text/x-python",
        "sh" to "application/x-sh",
        "bash" to "application/x-sh",
        "sql" to "application/sql",

        // Images
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "bmp" to "image/bmp",
        "webp" to "image/webp",
        "heic" to "image/heic",
        "heif" to "image/heif",
        "svg" to "image/svg+xml",

        // Video
        "mp4" to "video/mp4",
        "mkv" to "video/x-matroska",
        "avi" to "video/x-msvideo",
        "mov" to "video/quicktime",
        "webm" to "video/webm",
        "3gp" to "video/3gpp",
        "flv" to "video/x-flv",

        // Audio
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "flac" to "audio/flac",
        "aac" to "audio/aac",
        "ogg" to "audio/ogg",
        "opus" to "audio/opus",
        "m4a" to "audio/mp4",

        // Documents
        "pdf" to "application/pdf",

        // Archives
        "zip" to "application/zip",
        "tar" to "application/x-tar",
        "gz" to "application/gzip",
        "tgz" to "application/gzip",
        "gzip" to "application/gzip",
        "7z" to "application/x-7z-compressed",
        "rar" to "application/vnd.rar",
        "bz2" to "application/x-bzip2",
        "xz" to "application/x-xz",

        // APK
        "apk" to "application/vnd.android.package-archive",

        // Other
        "log" to "text/plain",
        "env" to "text/plain",
    )

    fun forExtension(extension: String?): String? {
        if (extension == null) return null
        return EXT_TO_MIME[extension.lowercase().trimStart('.')]
    }

    fun forName(name: String): String? {
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot == name.lastIndex) return null
        return forExtension(name.substring(dot + 1))
    }
}
