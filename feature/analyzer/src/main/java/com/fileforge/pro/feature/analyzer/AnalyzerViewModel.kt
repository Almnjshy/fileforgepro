package com.fileforge.pro.feature.analyzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.FileType
import com.fileforge.pro.domain.repository.StorageSourceRepository
import com.fileforge.pro.engine.storageanalyzer.StorageAnalysisResult
import com.fileforge.pro.engine.storageanalyzer.StorageAnalyzerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyzerUiState(
    val isAnalyzing: Boolean = false,
    val result: StorageAnalysisResult? = null,
    val errorMessage: String? = null,
    val scopePath: FPath? = null,
)

@HiltViewModel
class AnalyzerViewModel @Inject constructor(
    private val analyzerEngine: StorageAnalyzerEngine,
    private val storageSourceRepo: StorageSourceRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyzerUiState())
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            val sources = storageSourceRepo.getSources()
            val first = sources.firstOrNull()
            if (first != null) {
                _uiState.update { it.copy(scopePath = FPath.root(first.id)) }
                analyze()
            }
        }
    }

    fun analyze() {
        val scope = _uiState.value.scopePath ?: return
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            analyzerEngine.analyze(scope).collect { result ->
                _uiState.update { it.copy(result = result, isAnalyzing = !result.isComplete) }
            }
        }
    }
}
