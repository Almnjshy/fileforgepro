package com.fileforge.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fileforge.pro.data.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE sourceId = :sourceId AND path = :path)")
    suspend fun exists(sourceId: String, path: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity): Long

    @Query("DELETE FROM favorites WHERE sourceId = :sourceId AND path = :path")
    suspend fun delete(sourceId: String, path: String): Int

    @Query("DELETE FROM favorites")
    suspend fun clear(): Int
}
