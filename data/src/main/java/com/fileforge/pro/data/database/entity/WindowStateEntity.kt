package com.fileforge.pro.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "window_states")
data class WindowStateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String,           // WindowType.name
    val payload: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val zOrder: Int,
    val state: String,          // WindowState.name
    val isFocused: Boolean,
    val savedAt: Long,
)
