package com.fileforge.pro.engine.metadata

import com.fileforge.pro.core.filesystem.FileTypeDetector
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.repository.FileTypeRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [FileTypeRegistry] (Master Spec §56).
 *
 * Detection order:
 *   1. mimeType (most reliable if available)
 *   2. name (extension or known extensionless file like "Dockerfile")
 */
@Singleton
class FileTypeRegistryImpl @Inject constructor() : FileTypeRegistry {

    override fun detect(file: FFile): FileType {
        if (file.isDirectory) return FileType.FOLDER
        return detectByExtension(file.name, file.mimeType)
    }

    override fun detectByExtension(name: String, mimeType: String?): FileType {
        return FileTypeDetector.detect(name, mimeType, isDirectory = false)
    }

    override fun isTextFile(name: String, mimeType: String?): Boolean =
        FileTypeDetector.isText(name, mimeType)

    override fun isImageFile(name: String, mimeType: String?): Boolean =
        FileTypeDetector.isImage(name, mimeType)

    override fun isVideoFile(name: String, mimeType: String?): Boolean =
        FileTypeDetector.isVideo(name, mimeType)

    override fun isAudioFile(name: String, mimeType: String?): Boolean =
        FileTypeDetector.isAudio(name, mimeType)

    override fun isArchive(name: String, mimeType: String?): Boolean =
        FileTypeDetector.isArchive(name, mimeType)

    override fun isApk(name: String, mimeType: String?): Boolean =
        FileTypeDetector.isApk(name, mimeType)

    override fun isPdf(name: String, mimeType: String?): Boolean =
        FileTypeDetector.isPdf(name, mimeType)
}
