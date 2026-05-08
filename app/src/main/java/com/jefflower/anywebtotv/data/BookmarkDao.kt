package com.jefflower.anywebtotv.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY position ASC, createdAt ASC")
    fun observeAll(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks ORDER BY position ASC, createdAt ASC")
    suspend fun all(): List<Bookmark>

    @Query("SELECT MAX(position) FROM bookmarks")
    suspend fun maxPosition(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: Bookmark): Long

    @Update
    suspend fun update(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Query("UPDATE bookmarks SET iconPath = :iconPath WHERE id = :id")
    suspend fun setIconPath(id: Long, iconPath: String)
}
