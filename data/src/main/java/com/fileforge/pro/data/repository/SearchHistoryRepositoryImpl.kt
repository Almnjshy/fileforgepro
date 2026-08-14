package com.fileforge.pro.data.repository

import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import com.fileforge.pro.data.database.dao.SearchHistoryDao
import com.fileforge.pro.data.database.entity.SearchHistoryEntity
import com.fileforge.pro.domain.repository.SearchHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val dao: SearchHistoryDao,
) : SearchHistoryRepository {

    override fun observe(limit: Int): Flow<List<String>> =
        dao.observeRecent(limit).map { list -> list.map { it.query } }

    override suspend fun add(query: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            val now = System.currentTimeMillis()
            dao.upsert(SearchHistoryEntity(query = query, usedAt = now))
            Unit
        }
    }

    override suspend fun clear(): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            dao.clear()
            Unit
        }
    }
}
