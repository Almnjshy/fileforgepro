package com.fileforge.pro.domain.model

/**
 * How the file list is rendered. See Master Spec §10 — View Modes.
 */
enum class ViewMode {
    LARGE_GRID,
    MEDIUM_GRID,
    SMALL_GRID,
    LIST,
    COMPACT_LIST,
    DETAILS,
    THUMBNAIL,
    ;

    companion object {
        /** Default for phones (Master Spec §10 — Medium Grid). */
        val DEFAULT_PHONE = MEDIUM_GRID

        /** Default for tablets/desktop (Details). */
        val DEFAULT_TABLET = DETAILS
    }
}

/**
 * Continuous icon/thumbnail size scale (Master Spec §11).
 * 0.0 = smallest, 1.0 = largest. UI converts to concrete dp values.
 */
@JvmInline
value class ItemSize(val fraction: Float) {
    init {
        require(fraction in 0f..1f) { "ItemSize fraction must be 0..1, got $fraction" }
    }

    companion object {
        val SMALL = ItemSize(0.15f)
        val MEDIUM = ItemSize(0.5f)
        val LARGE = ItemSize(0.85f)
        val DEFAULT = MEDIUM
    }
}

/**
 * Per-folder view preferences (Master Spec §12).
 * If [folderPath] is null, this is the global default.
 */
data class ViewSettings(
    val folderPath: FPath? = null,
    val viewMode: ViewMode = ViewMode.DEFAULT_PHONE,
    val itemSize: ItemSize = ItemSize.DEFAULT,
    val showHidden: Boolean = false,
    val sortField: SortField = SortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASC,
)

enum class SortField {
    NAME, SIZE, TYPE, MODIFIED, CREATED, ACCESSED
}

enum class SortDirection {
    ASC, DESC
}
