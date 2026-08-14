package com.fileforge.pro.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_history",
    indices = [Index(value = ["query"], unique = true)],
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val usedAt: Long,
    val useCount: Int = 1,
)
