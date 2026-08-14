package com.fileforge.pro.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.core.common.FormatUtils
import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.core.storage.StorageStats
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.StorageSource
import com.fileforge.pro.domain.repository.FavoritesRepository
import com.fileforge.pro.domain.repository.RecentEntry
import com.fileforge.pro.domain.repository.RecentRepository
import com.fileforge.pro.domain.repository.StorageSourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val sources: List<StorageSource> = emptyList(),
    val primarySource: StorageSource? = null,
    val totalBytes: Long = 0,
    val freeBytes: Long = 0,
    val usedBytes: Long = 0,
    val usedFraction: Float = 0f,
    val recentFiles: List<RecentEntry> = emptyList(),
    val favorites: List<FFile> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val registry: StorageProviderRegistry,
    private val storageSourceRepo: StorageSourceRepository,
    private val recentRepo: RecentRepository,
    private val favoritesRepo: FavoritesRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch(dispatchers.io) {
            recentRepo.observe(10).collect { recent ->
                _uiState.update { it.copy(recentFiles = recent) }
            }
        }
        viewModelScope.launch(dispatchers.io) {
            favoritesRepo.observe().collect { favs ->
                _uiState.update { it.copy(favorites = favs) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            val providers = registry.all()
            var total = 0L
            var free = 0L
            val sources = providers.map { p ->
                val stats: StorageStats? = p.queryStorageStats()
                if (stats != null) {
                    total += stats.totalBytes
                    free += stats.freeBytes
                }
                p.source
            }
            val primary = sources.firstOrNull { it.id == "internal" } ?: sources.firstOrNull()
            _uiState.update {
                it.copy(
                    sources = sources,
                    primarySource = primary,
                    totalBytes = total,
                    freeBytes = free,
                    usedBytes = total - free,
                    usedFraction = if (total == 0L) 0f else (total - free).toFloat() / total,
                    isLoading = false,
                )
            }
        }
    }

    fun pathForSource(source: StorageSource): FPath = FPath.root(source.id)

    fun formatBytes(bytes: Long): String = FormatUtils.formatBytes(bytes)
}
