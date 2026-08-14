package com.fileforge.pro.feature.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.repository.StorageSourceRepository
import com.fileforge.pro.engine.storageanalyzer.LargeFilesFinder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LargeFilesUiState(
    val isLoading: Boolean = false,
    val files: List<FFile> = emptyList(),
    val minSize: Long = LargeFilesFinder.DEFAULT_THRESHOLD,
    val scopePath: FPath? = null,
)

@HiltViewModel
class LargeFilesViewModel @Inject constructor(
    private val finder: LargeFilesFinder,
    private val storageSourceRepo: StorageSourceRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LargeFilesUiState())
    val uiState: StateFlow<LargeFilesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            val sources = storageSourceRepo.getSources()
            val first = sources.firstOrNull()
            if (first != null) {
                _uiState.update { it.copy(scopePath = FPath.root(first.id)) }
                load()
            }
        }
    }

    fun setThreshold(bytes: Long) {
        _uiState.update { it.copy(minSize = bytes) }
        load()
    }

    fun load() {
        val scope = _uiState.value.scopePath ?: return
        val threshold = _uiState.value.minSize
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, files = emptyList()) }
            finder.find(scope, threshold).collect { files ->
                _uiState.update { it.copy(files = files, isLoading = false) }
            }
        }
    }
}
