package com.fileforge.pro.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.engine.media.MediaMetadata
import com.fileforge.pro.engine.media.MediaPreviewEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaViewerUiState(
    val isLoading: Boolean = false,
    val metadata: MediaMetadata? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val mediaEngine: MediaPreviewEngine,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaViewerUiState())
    val uiState: StateFlow<MediaViewerUiState> = _uiState.asStateFlow()

    fun load(file: FFile) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val metadata = mediaEngine.extractMetadata(file)
            _uiState.update { it.copy(isLoading = false, metadata = metadata) }
        }
    }
}
