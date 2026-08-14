package com.fileforge.pro.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fileforge.pro.data.database.entity.WindowStateEntity

@Dao
interface WindowStateDao {

    @Query("SELECT * FROM window_states ORDER BY zOrder ASC")
    suspend fun getAll(): List<WindowStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WindowStateEntity>)

    @Query("DELETE FROM window_states")
    suspend fun clear(): Int
}
