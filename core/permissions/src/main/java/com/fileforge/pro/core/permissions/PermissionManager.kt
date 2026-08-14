package com.fileforge.pro.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Central permission checking (Master Spec §68).
 *
 * IMPORTANT: This class is check-only. Requesting permissions requires an
 * Activity and is delegated to the UI layer via [PermissionRequester].
 */
class PermissionManager(private val context: Context) {

    fun hasManageExternalStorage(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun hasReadMediaImages(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasReadMediaVideo(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasReadMediaAudio(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasPostNotifications(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        true
    }

    fun hasInternet(): Boolean = hasPermission(Manifest.permission.INTERNET)

    /**
     * Do we have at minimum read access to the user's files?
     * Either MANAGE_EXTERNAL_STORAGE (R+) or READ_EXTERNAL_STORAGE (<R).
     */
    fun hasBasicReadAccess(): Boolean = hasManageExternalStorage() ||
            hasReadMediaImages() || hasReadMediaVideo() || hasReadMediaAudio()

    fun hasWriteAccess(): Boolean = hasManageExternalStorage()

    // ---- SAF persisted URI helpers ----

    fun persistSafTreeUri(uri: Uri, flags: Int) {
        val cr = context.contentResolver
        try {
            cr.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Caller didn't pass FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
    }

    fun listPersistedSafTrees(): List<Uri> {
        val cr = context.contentResolver
        return cr.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission }
            .map { it.uri }
    }

    fun releaseSafTreeUri(uri: Uri) {
        val cr = context.contentResolver
        try {
            cr.releasePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // already released
        }
    }

    // ---- Settings intent helpers ----

    fun manageStorageSettingsIntent(): android.content.Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.content.Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun appDetailsSettingsIntent(): android.content.Intent {
        return android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}
