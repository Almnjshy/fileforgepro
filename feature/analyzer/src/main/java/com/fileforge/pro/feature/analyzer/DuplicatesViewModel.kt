package com.fileforge.pro.feature.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.repository.StorageSourceRepository
import com.fileforge.pro.engine.storageanalyzer.DuplicateFinder
import com.fileforge.pro.engine.storageanalyzer.DuplicateResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicatesUiState(
    val isScanning: Boolean = false,
    val result: DuplicateResult? = null,
    val scopePath: FPath? = null,
)

@HiltViewModel
class DuplicatesViewModel @Inject constructor(
    private val finder: DuplicateFinder,
    private val storageSourceRepo: StorageSourceRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            val sources = storageSourceRepo.getSources()
            val first = sources.firstOrNull()
            if (first != null) {
                _uiState.update { it.copy(scopePath = FPath.root(first.id)) }
                scan()
            }
        }
    }

    fun scan() {
        val scope = _uiState.value.scopePath ?: return
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isScanning = true) }
            finder.find(scope).collect { result ->
                _uiState.update { it.copy(result = result, isScanning = !result.isComplete) }
            }
        }
    }
}
