package com.fileforge.pro.feature.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileforge.pro.engine.network.ConnectionType
import com.fileforge.pro.engine.network.NetworkConnection
import com.fileforge.pro.engine.network.NetworkSourcesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NetworkUiState(
    val connections: List<NetworkConnection> = emptyList(),
    val showAddDialog: Boolean = false,
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val repo: NetworkSourcesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.connections.collect { conns ->
                _uiState.update { it.copy(connections = conns) }
            }
        }
    }

    fun showAddDialog(show: Boolean) {
        _uiState.update { it.copy(showAddDialog = show) }
    }

    fun addConnection(
        type: ConnectionType,
        displayName: String,
        host: String,
        port: Int,
        username: String,
        password: String,
    ): Boolean {
        val conn = NetworkConnection(
            id = "net-${UUID.randomUUID().toString().take(8)}",
            displayName = displayName,
            type = type,
            host = host,
            port = port,
            username = username,
            password = password,
        )
        val ok = repo.add(conn)
        if (ok) _uiState.update { it.copy(showAddDialog = false) }
        return ok
    }

    fun removeConnection(id: String) {
        repo.remove(id)
    }
}
