package com.fileforge.pro.domain.model

/**
 * Type of storage source. Drives icon, permissions flow, and capabilities.
 */
enum class StorageSourceKind {
    INTERNAL,
    SD_CARD,
    USB_OTG,
    SAF_TREE,
    MEDIA_STORE,
    FTP,
    SFTP,
    SMB,
    WEBDAV,
    CLOUD,
    VAULT,
}

/**
 * A mountable storage source (Internal, SD, USB, FTP, SMB, ...).
 *
 * The File Browser NEVER talks to the underlying filesystem directly —
 * it always goes through a [com.fileforge.pro.core.storage.StorageProvider]
 * keyed by [id].
 */
data class StorageSource(
    val id: String,
    val kind: StorageSourceKind,
    val name: String,
    val description: String? = null,
    val isWritable: Boolean,
    val isAvailable: Boolean,
    val totalBytes: Long? = null,
    val freeBytes: Long? = null,
    val iconKey: String? = null,
) {
    val isRemovable: Boolean
        get() = kind == StorageSourceKind.SD_CARD ||
                kind == StorageSourceKind.USB_OTG ||
                kind == StorageSourceKind.SAF_TREE

    val isNetwork: Boolean
        get() = kind == StorageSourceKind.FTP ||
                kind == StorageSourceKind.SFTP ||
                kind == StorageSourceKind.SMB ||
                kind == StorageSourceKind.WEBDAV ||
                kind == StorageSourceKind.CLOUD
}
