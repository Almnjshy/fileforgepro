package com.fileforge.pro.feature.browser.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.ConflictResolution
import com.fileforge.pro.domain.model.FileOperation
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.model.OperationState
import com.fileforge.pro.domain.repository.FileOperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OperationsPanelUiState(
    val operations: List<FileOperation> = emptyList(),
    val isPanelVisible: Boolean = false,
)

@HiltViewModel
class OperationsPanelViewModel @Inject constructor(
    private val operationRepo: FileOperationRepository,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperationsPanelUiState())
    val uiState: StateFlow<OperationsPanelUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(dispatchers.io) {
            operationRepo.observeOperations().collect { list ->
                _uiState.value = OperationsPanelUiState(
                    operations = list,
                    isPanelVisible = list.any { it.state in activeStates },
                )
            }
        }
    }

    fun copy(sources: List<FPath>, destination: FPath) = viewModelScope.launch(dispatchers.io) {
        operationRepo.enqueueCopy(sources, destination)
    }

    fun move(sources: List<FPath>, destination: FPath) = viewModelScope.launch(dispatchers.io) {
        operationRepo.enqueueMove(sources, destination)
    }

    fun delete(sources: List<FPath>) = viewModelScope.launch(dispatchers.io) {
        operationRepo.enqueueDelete(sources)
    }

    fun rename(path: FPath, newName: String) = viewModelScope.launch(dispatchers.io) {
        operationRepo.enqueueRename(path, newName)
    }

    fun createFolder(parent: FPath, name: String) = viewModelScope.launch(dispatchers.io) {
        operationRepo.enqueueCreateFolder(parent, name)
    }

    fun createFile(parent: FPath, name: String) = viewModelScope.launch(dispatchers.io) {
        operationRepo.enqueueCreateFile(parent, name)
    }

    fun pause(id: String) = viewModelScope.launch(dispatchers.io) { operationRepo.pause(id) }
    fun resume(id: String) = viewModelScope.launch(dispatchers.io) { operationRepo.resume(id) }
    fun cancel(id: String) = viewModelScope.launch(dispatchers.io) { operationRepo.cancel(id) }
    fun retry(id: String) = viewModelScope.launch(dispatchers.io) { operationRepo.retry(id) }

    fun resolveConflict(id: String, resolution: ConflictResolution) = viewModelScope.launch(dispatchers.io) {
        operationRepo.resolveConflict(id, resolution)
    }

    companion object {
        private val activeStates = setOf(
            OperationState.QUEUED,
            OperationState.RUNNING,
            OperationState.PAUSED,
            OperationState.AWAITING_CONFLICT,
        )
    }
}
