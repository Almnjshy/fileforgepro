package com.fileforge.pro.core.storage

/**
 * Snapshot of all storage volumes reported by Android at a given moment.
 * Used by the Home Dashboard "Storage Sources" card and by the Sidebar.
 */
data class StorageVolumeInfo(
    val id: String,
    val description: String,
    val isRemovable: Boolean,
    val isEmulated: Boolean,
    val isPrimary: Boolean,
    val state: String, // MOUNTED, UNMOUNTED, ...
    val totalBytes: Long,
    val freeBytes: Long,
    val rootUri: String?,
)
