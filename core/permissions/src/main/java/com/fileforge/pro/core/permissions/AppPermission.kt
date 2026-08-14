package com.fileforge.pro.core.permissions

/**
 * All permissions FileForge Pro may request, with rationale strings
 * (Master Spec §68 — Permissions UX).
 *
 * The actual string resources live in app/res/values/strings.xml.
 * This enum maps to those resource IDs indirectly via [rationaleKey].
 */
enum class AppPermission(val rationaleKey: String) {
    MANAGE_EXTERNAL_STORAGE("perm_rationale_manage_storage"),
    READ_MEDIA_IMAGES("perm_rationale_read_media"),
    READ_MEDIA_VIDEO("perm_rationale_read_media"),
    READ_MEDIA_AUDIO("perm_rationale_read_media"),
    POST_NOTIFICATIONS("perm_rationale_notifications"),
    FOREGROUND_SERVICE("perm_rationale_foreground_service"),
    INTERNET("perm_rationale_internet"),
}

/**
 * Result of a permission request.
 */
sealed interface PermissionResult {
    data object Granted : PermissionResult
    data object Denied : PermissionResult
    data object PermanentlyDenied : PermissionResult
    data class RationaleRequired(val permission: AppPermission) : PermissionResult
}
