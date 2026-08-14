package com.fileforge.pro.core.storage

import com.fileforge.pro.domain.model.FPath
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry of all available [StorageProvider]s (Master Spec §5, §81).
 *
 * The browser and repositories never instantiate providers directly —
 * they ask the registry by `path.sourceId`. New providers (FTP, SMB, ...)
 * register themselves here at app startup.
 */
@Singleton
class StorageProviderRegistry @Inject constructor() {

    private val providers = ConcurrentHashMap<String, StorageProvider>()

    fun register(provider: StorageProvider) {
        providers[provider.sourceId] = provider
    }

    fun unregister(sourceId: String) {
        providers.remove(sourceId)
    }

    fun get(sourceId: String): StorageProvider? = providers[sourceId]

    fun get(path: FPath): StorageProvider? = providers[path.sourceId]

    fun all(): List<StorageProvider> = providers.values.toList().sortedBy { it.sourceId }

    fun contains(sourceId: String): Boolean = providers.containsKey(sourceId)

    val size: Int get() = providers.size
}
