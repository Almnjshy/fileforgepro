package com.fileforge.pro.feature.apk

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.engine.apk.ApkInfo
import com.fileforge.pro.engine.apk.ApkInspectorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApkUiState(
    val isLoading: Boolean = false,
    val file: FFile? = null,
    val info: ApkInfo? = null,
    val isInstalled: Boolean = false,
    val installedVersionCode: Long? = null,
    val errorMessage: String? = null,
) {
    val canUpdate: Boolean
        get() = info != null && installedVersionCode != null && info.versionCode > installedVersionCode!!
}

@HiltViewModel
class ApkViewModel @Inject constructor(
    @ApplicationContext private val appContext: android.content.Context,
    private val inspector: ApkInspectorEngine,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApkUiState())
    val uiState: StateFlow<ApkUiState> = _uiState.asStateFlow()

    fun load(file: FFile) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, file = file, errorMessage = null) }
            val info = inspector.inspect(file)
            if (info == null) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Not a valid APK file")
                }
                return@launch
            }
            val installed = inspector.isInstalled(info.packageName)
            val installedVc = if (installed) inspector.installedVersionCode(info.packageName) else null
            _uiState.update {
                it.copy(
                    isLoading = false,
                    info = info,
                    isInstalled = installed,
                    installedVersionCode = installedVc,
                )
            }
        }
    }

    /**
     * Build the install intent for the APK. The UI layer (Activity) is
     * responsible for launching it, because Intent resolution requires
     * an Activity context.
     */
    fun buildInstallIntent(): Intent? {
        val file = _uiState.value.file ?: return null
        val realPath = resolveRealPath(file) ?: return null
        val apkFile = java.io.File(realPath)
        if (!apkFile.exists()) return null

        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile,
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Build a share intent for the APK file.
     */
    fun buildShareIntent(): Intent? {
        val file = _uiState.value.file ?: return null
        val realPath = resolveRealPath(file) ?: return null
        val apkFile = java.io.File(realPath)
        if (!apkFile.exists()) return null

        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
