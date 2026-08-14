package com.fileforge.pro.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fileforge.pro.data.database.dao.FavoriteDao
import com.fileforge.pro.data.database.dao.RecentDao
import com.fileforge.pro.data.database.dao.SearchHistoryDao
import com.fileforge.pro.data.database.dao.WindowStateDao
import com.fileforge.pro.data.database.entity.FavoriteEntity
import com.fileforge.pro.data.database.entity.RecentEntity
import com.fileforge.pro.data.database.entity.SearchHistoryEntity
import com.fileforge.pro.data.database.entity.WindowStateEntity

/**
 * Single Room database holding all persisted user data (Master Spec §67).
 *
 * NOTE: Never store file contents here — only metadata.
 */
@Database(
    entities = [
        FavoriteEntity::class,
        RecentEntity::class,
        SearchHistoryEntity::class,
        WindowStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FileForgeDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun windowStateDao(): WindowStateDao

    companion object {
        const val DB_NAME = "fileforge.db"
    }
}
