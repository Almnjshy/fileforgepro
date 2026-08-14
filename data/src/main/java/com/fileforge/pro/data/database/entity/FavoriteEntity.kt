package com.fileforge.pro.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["sourceId", "path"], unique = true)],
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val path: String,        // display path
    val name: String,
    val isDirectory: Boolean,
    val addedAt: Long,
)
