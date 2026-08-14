package com.fileforge.pro.feature.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.engine.vault.VaultEngine
import com.fileforge.pro.engine.vault.VaultEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val isUnlocking: Boolean = false,
    val unlockError: String? = null,
    val entries: List<VaultEntry> = emptyList(),
    val isLoading: Boolean = false,
    val hasVault: Boolean = false,
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val engine: VaultEngine,
    private val dispatchers: AppDispatchers,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isUnlocked = engine.isUnlocked) }
            if (engine.isUnlocked) loadEntries()
        }
    }

    fun unlock(password: String) {
        viewModelScope.launch(dispatchers.io) {
            _uiState.update { it.copy(isUnlocking = true, unlockError = null) }
            val ok = engine.unlock(password.toCharArray())
            _uiState.update {
                it.copy(
                    isUnlocking = false,
                    isUnlocked = ok,
                    unlockError = if (ok) null else "Wrong password",
                )
            }
            if (ok) loadEntries()
        }
    }

    fun lock() {
        engine.lock()
        _uiState.update { it.copy(isUnlocked = false, entries = emptyList()) }
    }

    fun addFile(file: FFile) {
        viewModelScope.launch(dispatchers.io) {
            when (val r = engine.addEntry(file)) {
                is Result.Ok -> loadEntries()
                is Result.Err -> _uiState.update { it.copy(unlockError = r.error.message) }
            }
        }
    }

    fun extractEntry(entry: VaultEntry) {
        viewModelScope.launch(dispatchers.io) {
            // Extract to Downloads for simplicity
            val destDir = java.io.File("/storage/emulated/0/Download/FileForgeVault")
            engine.extractEntry(entry.id, destDir)
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        viewModelScope.launch(dispatchers.io) {
            engine.deleteEntry(entry.id)
            loadEntries()
        }
    }

    private suspend fun loadEntries() {
        _uiState.update { it.copy(isLoading = true) }
        when (val r = engine.listEntries()) {
            is Result.Ok -> _uiState.update {
                it.copy(entries = r.value, isLoading = false, hasVault = true)
            }
            is Result.Err -> _uiState.update { it.copy(isLoading = false) }
        }
    }
}
