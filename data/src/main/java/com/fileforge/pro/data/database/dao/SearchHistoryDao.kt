package com.fileforge.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fileforge.pro.data.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY usedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SearchHistoryEntity): Long

    @Query("UPDATE search_history SET usedAt = :ts, useCount = useCount + 1 WHERE query = :query")
    suspend fun touch(query: String, ts: Long): Int

    @Query("DELETE FROM search_history")
    suspend fun clear(): Int
}
