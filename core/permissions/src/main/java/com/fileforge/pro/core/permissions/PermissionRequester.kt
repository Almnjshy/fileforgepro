package com.fileforge.pro.core.permissions

import android.content.Intent
import android.net.Uri

/**
 * Bridge for UI to request runtime permissions. Implemented in :app
 * because Compose can't request permissions directly without rememberLauncherForActivityResult.
 */
interface PermissionRequester {
    fun requestManageExternalStorage(onResult: (Boolean) -> Unit)
    fun requestReadMediaImages(onResult: (Boolean) -> Unit)
    fun requestReadMediaVideo(onResult: (Boolean) -> Unit)
    fun requestReadMediaAudio(onResult: (Boolean) -> Unit)
    fun requestPostNotifications(onResult: (Boolean) -> Unit)
    fun requestSafTree(initialUri: Uri?, onResult: (Uri?) -> Unit)
    fun openAppDetailsSettings()
    fun openManageStorageSettings()

    /** Convenience: returns the intent for opening SAF tree picker. */
    fun safTreePickerIntent(initialUri: Uri?): Intent
}
