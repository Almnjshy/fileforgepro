package com.fileforge.pro.engine.network

import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.engine.network.ftp.FtpConnection
import com.fileforge.pro.engine.network.ftp.FtpProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted network connection (Master Spec §51).
 *
 * For Phase 14 we hold connections in memory; persistence to DataStore
 * will be added in Phase 17.
 */
data class NetworkConnection(
    val id: String,
    val displayName: String,
    val type: ConnectionType,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,  // stored encrypted in production
    val isPassive: Boolean = true,
)

enum class ConnectionType { FTP, SFTP, SMB, WEBDAV }

/**
 * Manages user-saved network storage connections (Master Spec §51).
 *
 * Responsibilities:
 *   - Persist user-added connections
 *   - Register [StorageProvider]s for active connections
 *   - List available connections for the Network screen
 */
@Singleton
class NetworkSourcesRepository @Inject constructor(
    private val providerRegistry: StorageProviderRegistry,
) {
    private val _connections = MutableStateFlow<List<NetworkConnection>>(emptyList())
    val connections: StateFlow<List<NetworkConnection>> = _connections.asStateFlow()

    fun add(connection: NetworkConnection): Boolean {
        // Register the appropriate provider
        val provider = when (connection.type) {
            ConnectionType.FTP -> FtpProvider(
                sourceId = connection.id,
                connection = FtpConnection(
                    id = connection.id,
                    displayName = connection.displayName,
                    host = connection.host,
                    port = connection.port,
                    username = connection.username,
                    password = connection.password,
                    isPassive = connection.isPassive,
                ),
            )
            // SFTP / SMB / WebDAV — Phase 14+ stubs
            ConnectionType.SFTP, ConnectionType.SMB, ConnectionType.WEBDAV -> {
                return false // not yet implemented
            }
        }
        providerRegistry.register(provider)
        _connections.update { it + connection }
        return true
    }

    fun remove(id: String) {
        providerRegistry.unregister(id)
        _connections.update { it.filterNot { c -> c.id == id } }
    }

    fun getById(id: String): NetworkConnection? = _connections.value.firstOrNull { it.id == id }
}
