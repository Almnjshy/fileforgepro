package com.fileforge.pro.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recents",
    indices = [Index(value = ["sourceId", "path"], unique = true)],
)
data class RecentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val lastAccessed: Long,
    val accessCount: Int = 1,
)
