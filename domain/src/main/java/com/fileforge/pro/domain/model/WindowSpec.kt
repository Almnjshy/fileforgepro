package com.fileforge.pro.domain.model

/**
 * A floating window in the in-app window manager (Master Spec §19–25).
 */
data class WindowSpec(
    val id: String,
    val title: String,
    val type: WindowType,
    val payload: WindowPayload,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val zOrder: Int,
    val state: WindowState = WindowState.NORMAL,
    val isFocused: Boolean = false,
)

enum class WindowType {
    FILE_BROWSER,
    FOLDER,
    TEXT_EDITOR,
    IMAGE_VIEWER,
    PDF_VIEWER,
    ARCHIVE,
    PROPERTIES,
}

@JvmInline
value class WindowPayload(val raw: String) {
    /** For browser/folder windows: the [FPath] serialized as "sourceId|/a/b/c". */
    val path: FPath?
        get() {
            val parts = raw.split("|", limit = 2)
            if (parts.size != 2) return null
            return FPath.fromString(parts[0], parts[1])
        }

    companion object {
        fun fromPath(path: FPath) = WindowPayload("${path.sourceId}|${path.displayPath}")
    }
}

enum class WindowState {
    NORMAL, MINIMIZED, MAXIMIZED
}
