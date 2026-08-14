package com.fileforge.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fileforge.pro.data.database.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {

    @Query("SELECT * FROM recents ORDER BY lastAccessed DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecentEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM recents WHERE sourceId = :sourceId AND path = :path)")
    suspend fun exists(sourceId: String, path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecentEntity): Long

    @Query("UPDATE recents SET lastAccessed = :ts, accessCount = accessCount + 1 WHERE sourceId = :sourceId AND path = :path")
    suspend fun touch(sourceId: String, path: String, ts: Long): Int

    @Query("DELETE FROM recents WHERE sourceId = :sourceId AND path = :path")
    suspend fun delete(sourceId: String, path: String): Int

    @Query("DELETE FROM recents")
    suspend fun clear(): Int
}
