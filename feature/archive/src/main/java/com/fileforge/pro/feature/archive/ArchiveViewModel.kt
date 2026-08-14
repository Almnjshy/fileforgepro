package com.fileforge.pro.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.engine.archive.ArchiveEngine
import com.fileforge.pro.engine.archive.ArchiveListing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val isLoading: Boolean = false,
    val isExtracting: Boolean = false,
    val archive: FFile? = null,
    val listing: ArchiveListing? = null,
    val errorMessage: String? = null,
    val extractedCount: Int = 0,
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val engine: ArchiveEngine,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    fun load(archive: FFile) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, archive = archive, errorMessage = null) }
            engine.list(archive).collect { listing ->
                _uiState.update {
                    it.copy(
                        isLoading = !listing.isComplete,
                        listing = listing,
                    )
                }
            }
        }
    }

    fun extract(destinationPath: com.fileforge.pro.domain.model.FPath) {
        val archive = _uiState.value.archive ?: return
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isExtracting = true, errorMessage = null) }
            when (val r = engine.extract(archive, destinationPath)) {
                is Result.Ok -> _uiState.update {
                    it.copy(isExtracting = false, extractedCount = r.value)
                }
                is Result.Err -> _uiState.update {
                    it.copy(isExtracting = false, errorMessage = r.error.message)
                }
            }
        }
    }
}
