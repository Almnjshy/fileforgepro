package com.fileforge.pro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.ItemSize
import com.fileforge.pro.domain.model.SortDirection
import com.fileforge.pro.domain.model.SortField
import com.fileforge.pro.domain.model.ViewMode
import com.fileforge.pro.domain.model.ViewSettings
import com.fileforge.pro.domain.repository.ViewSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_GLOBAL_MODE = stringPreferencesKey("global_view_mode")
private val KEY_GLOBAL_SIZE = stringPreferencesKey("global_item_size")
private val KEY_GLOBAL_SORT = stringPreferencesKey("global_sort_field")
private val KEY_GLOBAL_SORT_DIR = stringPreferencesKey("global_sort_dir")
private val KEY_GLOBAL_SHOW_HIDDEN = stringPreferencesKey("global_show_hidden")

@Singleton
class ViewSettingsRepositoryImpl @Inject constructor(
    private val store: DataStore<Preferences>,
) : ViewSettingsRepository {

    override fun observeGlobal(): Flow<ViewSettings> = store.data.map { p ->
        ViewSettings(
            folderPath = null,
            viewMode = p[KEY_GLOBAL_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.DEFAULT_PHONE,
            itemSize = p[KEY_GLOBAL_SIZE]?.toFloatOrNull()?.let { runCatching { ItemSize(it) }.getOrNull() } ?: ItemSize.DEFAULT,
            sortField = p[KEY_GLOBAL_SORT]?.let { runCatching { SortField.valueOf(it) }.getOrNull() } ?: SortField.NAME,
            sortDirection = p[KEY_GLOBAL_SORT_DIR]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() } ?: SortDirection.ASC,
            showHidden = p[KEY_GLOBAL_SHOW_HIDDEN]?.toBoolean() ?: false,
        )
    }

    override fun observeForFolder(folder: FPath): Flow<ViewSettings> {
        // For Phase 3 we return global; per-folder will be added later.
        return observeGlobal()
    }

    override suspend fun setGlobalMode(mode: ViewMode): Result<Unit> = resultOf {
        store.edit { it[KEY_GLOBAL_MODE] = mode.name }
    }

    override suspend fun setForFolder(folder: FPath, settings: ViewSettings): Result<Unit> = resultOf {
        // TODO Phase 3+: per-folder store
        Unit
    }

    override suspend fun resetFolderSettings(folder: FPath): Result<Unit> = resultOf {
        Unit
    }
}
