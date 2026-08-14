package com.fileforge.pro.data.repository

import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import com.fileforge.pro.data.database.dao.RecentDao
import com.fileforge.pro.data.database.entity.RecentEntity
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.repository.RecentEntry
import com.fileforge.pro.domain.repository.RecentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentRepositoryImpl @Inject constructor(
    private val dao: RecentDao,
) : RecentRepository {

    override fun observe(limit: Int): Flow<List<RecentEntry>> =
        dao.observeRecent(limit).map { list -> list.map { it.toEntry() } }

    override suspend fun recordAccess(path: FPath, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            val now = System.currentTimeMillis()
            if (dao.exists(path.sourceId, path.displayPath)) {
                dao.touch(path.sourceId, path.displayPath, now)
            } else {
                dao.upsert(
                    RecentEntity(
                        sourceId = path.sourceId,
                        path = path.displayPath,
                        name = name,
                        isDirectory = false,
                        lastAccessed = now,
                    )
                )
            }
            Unit
        }
    }

    override suspend fun remove(path: FPath): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            dao.delete(path.sourceId, path.displayPath)
            Unit
        }
    }

    override suspend fun clearHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            dao.clear()
            Unit
        }
    }
}

private fun RecentEntity.toEntry(): RecentEntry = RecentEntry(
    path = FPath.fromString(sourceId, path),
    name = name,
    lastAccessed = lastAccessed,
)
