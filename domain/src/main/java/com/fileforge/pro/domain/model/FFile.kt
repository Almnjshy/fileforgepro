package com.fileforge.pro.domain.model

/**
 * Coarse classification of a file. Drives which [FileHandler] is invoked.
 *
 * The Browser only knows [FileType] — it does NOT know how to render
 * an image or parse a ZIP. That is delegated to feature modules via
 * [com.fileforge.pro.domain.repository.FileTypeRegistry].
 */
enum class FileType {
    FOLDER,
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    ARCHIVE,
    APK,
    PDF,
    OTHER,
    ;

    val isMedia: Boolean get() = this == IMAGE || this == VIDEO || this == AUDIO
}

/**
 * A file or folder entry returned by a [StorageProvider].
 *
 * Immutable. UI-safe. No java.io.File leak — only the [path] reference.
 */
data class FFile(
    val path: FPath,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val lastAccessed: Long? = null,
    val created: Long? = null,
    val mimeType: String? = null,
    val fileType: FileType = FileType.OTHER,
    val isHidden: Boolean = false,
    val isReadable: Boolean = true,
    val isWritable: Boolean = true,
    val isExecutable: Boolean = false,
    val extension: String? = null,
    val itemCount: Int? = null, // for folders (lazy: null = unknown)
    val thumbnailKey: String? = null,
) {
    val isFile: Boolean get() = !isDirectory

}
