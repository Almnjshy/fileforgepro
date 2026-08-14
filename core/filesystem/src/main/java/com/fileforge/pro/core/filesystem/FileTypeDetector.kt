package com.fileforge.pro.core.filesystem

import com.fileforge.pro.domain.model.FileType

/**
 * Extension → [FileType] mapping (Master Spec §38, §56).
 *
 * Pure function, no Android dependency. Used by [FileBridge] and by the
 * [com.fileforge.pro.domain.repository.FileTypeRegistry] implementation.
 */
object FileTypeDetector {

    private val IMAGE_EXTS = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif",
        "tiff", "tif", "svg", "raw", "cr2", "nef", "arw", "dng",
    )

    private val VIDEO_EXTS = setOf(
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "3g2",
        "mpeg", "mpg", "m4v", "ts", "vob", "ogv",
    )

    private val AUDIO_EXTS = setOf(
        "mp3", "wav", "flac", "aac", "ogg", "oga", "opus", "m4a", "wma",
        "amr", "aiff", "aif", "alac", "mid", "midi", "xmf", "rtttl", "rtx",
    )

    private val TEXT_EXTS = setOf(
        "txt", "log", "md", "markdown", "csv", "tsv",
        "json", "json5", "xml", "html", "htm", "css", "scss", "sass", "less",
        "js", "jsx", "ts", "tsx", "java", "kt", "kts", "gradle", "properties",
        "ini", "cfg", "conf", "yaml", "yml", "toml", "sh", "bash", "zsh",
        "bat", "cmd", "sql", "php", "py", "rb", "go", "rs", "c", "h", "cpp",
        "hpp", "cs", "swift", "dart", "vue", "svelte", "env",
    )

    private val ARCHIVE_EXTS = setOf(
        "zip", "tar", "gz", "tgz", "gzip", "7z", "rar", "bz2", "xz", "lz4", "zst",
    )

    private val APK_EXTS = setOf("apk", "apks", "xapk", "apkm")

    private val PDF_EXTS = setOf("pdf")

    /**
     * Files without extension that are still text/code (Master Spec §46).
     */
    private val EXTENSIONLESS_TEXT = setOf(
        "gitignore", "dockerfile", "makefile", "license", "readme",
        "gemfile", "rakefile", "bashrc", "zshrc", "vimrc", "editorconfig",
        "eslintrc", "prettierrc", "babelrc",
    )

    fun detectByExtension(extension: String?): FileType {
        if (extension.isNullOrEmpty()) return FileType.OTHER
        val ext = extension.lowercase().trimStart('.')
        return when (ext) {
            in IMAGE_EXTS -> FileType.IMAGE
            in VIDEO_EXTS -> FileType.VIDEO
            in AUDIO_EXTS -> FileType.AUDIO
            in ARCHIVE_EXTS -> FileType.ARCHIVE
            in APK_EXTS -> FileType.APK
            in PDF_EXTS -> FileType.PDF
            in TEXT_EXTS -> FileType.TEXT
            else -> FileType.OTHER
        }
    }

    fun detectByName(name: String): FileType {
        if (name.isEmpty()) return FileType.OTHER
        val lower = name.lowercase()
        val dotIndex = lower.lastIndexOf('.')
        return if (dotIndex <= 0) {
            // No extension or hidden file starting with "."
            if (lower in EXTENSIONLESS_TEXT) FileType.TEXT else FileType.OTHER
        } else {
            detectByExtension(lower.substring(dotIndex + 1))
        }
    }

    fun detectByMime(mimeType: String?): FileType? {
        if (mimeType == null) return null
        val mt = mimeType.lowercase()
        return when {
            mt.startsWith("image/") -> FileType.IMAGE
            mt.startsWith("video/") -> FileType.VIDEO
            mt.startsWith("audio/") -> FileType.AUDIO
            mt.startsWith("text/") -> FileType.TEXT
            mt == "application/pdf" -> FileType.PDF
            mt == "application/vnd.android.package-archive" -> FileType.APK
            mt.contains("zip") || mt.contains("tar") || mt.contains("gzip") ||
                    mt.contains("rar") || mt.contains("7z") || mt.contains("compress") -> FileType.ARCHIVE
            mt.contains("json") || mt.contains("xml") || mt.contains("html") -> FileType.TEXT
            else -> null
        }
    }

    fun detect(name: String, mimeType: String?, isDirectory: Boolean): FileType {
        if (isDirectory) return FileType.FOLDER
        return detectByMime(mimeType) ?: detectByName(name)
    }

    // --- Convenience predicates ---

    fun isImage(name: String, mime: String?) = detect(name, mime, false) == FileType.IMAGE
    fun isVideo(name: String, mime: String?) = detect(name, mime, false) == FileType.VIDEO
    fun isAudio(name: String, mime: String?) = detect(name, mime, false) == FileType.AUDIO
    fun isText(name: String, mime: String?) = detect(name, mime, false) == FileType.TEXT
    fun isArchive(name: String, mime: String?) = detect(name, mime, false) == FileType.ARCHIVE
    fun isApk(name: String, mime: String?) = detect(name, mime, false) == FileType.APK
    fun isPdf(name: String, mime: String?) = detect(name, mime, false) == FileType.PDF
}
