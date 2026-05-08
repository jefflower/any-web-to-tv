package com.jefflower.anywebtotv.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val iconPath: String? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
