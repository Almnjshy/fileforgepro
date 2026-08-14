package com.fileforge.pro.data.repository

import com.fileforge.pro.domain.result.Result
import com.fileforge.pro.domain.result.resultOf
import com.fileforge.pro.data.database.dao.FavoriteDao
import com.fileforge.pro.data.database.entity.FavoriteEntity
import com.fileforge.pro.domain.model.FFile
import com.fileforge.pro.domain.model.FPath
import com.fileforge.pro.domain.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val dao: FavoriteDao,
) : FavoritesRepository {

    override fun observe(): Flow<List<FFile>> =
        dao.observeAll().map { list -> list.map { it.toFFile() } }

    override suspend fun add(file: FFile): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            dao.insert(
                FavoriteEntity(
                    sourceId = file.path.sourceId,
                    path = file.path.displayPath,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    addedAt = System.currentTimeMillis(),
                )
            )
            Unit
        }
    }

    override suspend fun remove(path: FPath): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            dao.delete(path.sourceId, path.displayPath)
            Unit
        }
    }

    override suspend fun contains(path: FPath): Boolean = withContext(Dispatchers.IO) {
        dao.exists(path.sourceId, path.displayPath)
    }

    override suspend fun clear(): Result<Unit> = withContext(Dispatchers.IO) {
        resultOf {
            dao.clear()
            Unit
        }
    }
}

private fun FavoriteEntity.toFFile(): FFile = FFile(
    path = FPath.fromString(sourceId, path),
    name = name,
    isDirectory = isDirectory,
    size = 0L,
    lastModified = addedAt,
)
