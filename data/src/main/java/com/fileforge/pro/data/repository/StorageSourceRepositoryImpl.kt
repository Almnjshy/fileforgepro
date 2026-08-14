package com.fileforge.pro.data.repository

import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.domain.model.StorageSource
import com.fileforge.pro.domain.repository.StorageSourceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageSourceRepositoryImpl @Inject constructor(
    private val registry: StorageProviderRegistry,
) : StorageSourceRepository {

    private val _sources = MutableStateFlow<List<StorageSource>>(emptyList())
    override fun observeSources(): StateFlow<List<StorageSource>> = _sources.asStateFlow()

    override suspend fun getSources(): List<StorageSource> {
        return registry.all().map { it.source }
    }

    override suspend fun refreshSources(): List<StorageSource> {
        val list = registry.all().map { it.source }
        _sources.value = list
        return list
    }

    override fun getById(id: String): StorageSource? = registry.get(id)?.source
}
