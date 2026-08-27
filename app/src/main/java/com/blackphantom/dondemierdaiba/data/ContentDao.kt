package com.blackphantom.dondemierdaiba.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ContentDao {

    @Query("SELECT * FROM content_items WHERE type = :type AND name LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun observeByType(type: String, query: String): LiveData<List<ContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ContentEntity): Long

    @Update
    suspend fun update(item: ContentEntity)

    @Delete
    suspend fun delete(item: ContentEntity)

    @Query("SELECT * FROM content_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ContentEntity?

    @Query("SELECT * FROM content_items WHERE type = 'SERIE' AND tvMazeId IS NOT NULL")
    suspend fun getSeriesLinkedToTvMaze(): List<ContentEntity>

    @Query("SELECT * FROM content_items ORDER BY type, updatedAt DESC")
    suspend fun getAllOnce(): List<ContentEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ContentEntity>): List<Long>
}
