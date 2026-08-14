package com.fileforge.pro.engine.apk

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.LogTags
import com.fileforge.pro.domain.model.FFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parsed APK metadata (Master Spec §50).
 */
data class ApkInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdkVersion: Int?,
    val targetSdkVersion: Int?,
    val permissions: List<String>,
    val isSystemApp: Boolean,
    val installerPackageName: String?,
    val icon: Drawable?,
) {
    val displayLabel: String
        get() = if (appName != packageName) "$appName ($packageName)" else packageName
}

/**
 * APK inspector — extracts real app metadata from .apk files
 * (Master Spec §50 — APK Manager).
 */
@Singleton
class ApkInspectorEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun inspect(file: FFile): ApkInfo? = withContext(Dispatchers.IO) {
        val realPath = resolveRealPath(file) ?: return@withContext null
        val apkFile = File(realPath)
        if (!apkFile.exists() || !apkFile.canRead()) {
            Logger.w(LogTags.APK, "Cannot read APK: $realPath")
            return@withContext null
        }

        try {
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(realPath, 0)
                ?: run {
                    Logger.w(LogTags.APK, "Not a valid APK: $realPath")
                    return@withContext null
                }

            pkgInfo.applicationInfo?.apply {
                sourceDir = realPath
                publicSourceDir = realPath
            }

            val appInfo = pkgInfo.applicationInfo
            val appName = pm.getApplicationLabel(appInfo).toString()
            val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
            val permissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()

            ApkInfo(
                appName = appName.ifBlank { appInfo.packageName },
                packageName = appInfo.packageName,
                versionName = pkgInfo.versionName ?: "unknown",
                versionCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pkgInfo),
                minSdkVersion = appInfo.minSdkVersion.takeIf { it > 0 },
                targetSdkVersion = appInfo.targetSdkVersion.takeIf { it > 0 },
                permissions = permissions,
                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                installerPackageName = runCatching { pm.getInstallerPackageName(appInfo.packageName) }.getOrNull(),
                icon = icon,
            )
        } catch (e: Exception) {
            Logger.e(LogTags.APK, "inspect failed: ${file.name}", e)
            null
        }
    }

    suspend fun isInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun installedVersionCode(packageName: String): Long? = withContext(Dispatchers.IO) {
        try {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun resolveRealPath(file: FFile): String? {
        return when (file.path.sourceId) {
            "internal" -> "/storage/emulated/0/${file.path.displayPath}".trimEnd('/')
            else -> {
                val volId = file.path.sourceId.substringAfter('-')
                "/storage/$volId/${file.path.displayPath}".trimEnd('/')
            }
        }
    }
}
