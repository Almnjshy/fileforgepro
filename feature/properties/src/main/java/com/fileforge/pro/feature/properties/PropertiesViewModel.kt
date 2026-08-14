package com.fileforge.pro.feature.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PropertiesUiState(
    val isLoading: Boolean = false,
    val file: FFile? = null,
)

@HiltViewModel
class PropertiesViewModel @Inject constructor(
    private val fileRepo: FileRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertiesUiState())
    val uiState: StateFlow<PropertiesUiState> = _uiState.asStateFlow()

    fun load(file: FFile) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = fileRepo.stat(file.path)) {
                is com.fileforge.pro.domain.result.Result.Ok -> {
                    _uiState.update { it.copy(isLoading = false, file = r.value) }
                }
                is com.fileforge.pro.domain.result.Result.Err -> {
                    _uiState.update { it.copy(isLoading = false, file = file) }
                }
            }
        }
    }
}
