package com.fileforge.pro.domain.model

/**
 * Active file filter (Master Spec §14).
 * All fields optional — null means "don't filter by this dimension".
 */
data class FileFilter(
    val fileTypes: Set<FileType>? = null,
    val extensions: Set<String>? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val modifiedAfter: Long? = null,
    val modifiedBefore: Long? = null,
    val showHidden: Boolean = false,
    val searchQuery: String? = null,
) {
    val isEmpty: Boolean
        get() = fileTypes == null && extensions == null && minSize == null &&
                maxSize == null && modifiedAfter == null && modifiedBefore == null &&
                !showHidden && searchQuery == null

    companion object {
        val EMPTY = FileFilter()
    }
}
