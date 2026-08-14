package com.fileforge.pro.domain.repository

import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FileType

/**
 * Registry for file-type detection and handler routing.
 * Master Spec §56 — FileTypeRegistry.
 *
 * The browser only knows [FileType]; it asks this registry for a handler
 * when the user opens a file. New types can be added without touching the
 * browser.
 */
interface FileTypeRegistry {
    fun detect(file: FFile): FileType
    fun detectByExtension(name: String, mimeType: String?): FileType
    fun isTextFile(name: String, mimeType: String?): Boolean
    fun isImageFile(name: String, mimeType: String?): Boolean
    fun isVideoFile(name: String, mimeType: String?): Boolean
    fun isAudioFile(name: String, mimeType: String?): Boolean
    fun isArchive(name: String, mimeType: String?): Boolean
    fun isApk(name: String, mimeType: String?): Boolean
    fun isPdf(name: String, mimeType: String?): Boolean
}

/** File-type handler — opens a file in the appropriate UI. */
interface FileHandler {
    val supportedTypes: Set<FileType>
    fun canHandle(file: FFile): Boolean
}
