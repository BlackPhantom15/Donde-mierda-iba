package com.blackphantom.dondemierdaiba.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ContentDao {

    @Query("""
        SELECT * FROM content_items
        WHERE type = :type AND name LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN type = 'SERIE' AND tvMazeId IS NOT NULL AND remoteSeason > 0
                      AND (remoteSeason > season OR (remoteSeason = season AND remoteEpisode > episode))
                 THEN 0 ELSE 1 END,
            updatedAt DESC
    """)
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

    // ---------- Usados por el widget de pantalla de inicio ----------
    // Los widgets de Android (RemoteViewsFactory) corren de forma síncrona en
    // su propio proceso/hilo, por eso estos métodos NO son suspend.

    @Query("SELECT * FROM content_items WHERE type = 'SERIE' AND isPinnedToWidget = 1 ORDER BY updatedAt DESC")
    fun getPinnedSeriesSync(): List<ContentEntity>

    @Query("SELECT * FROM content_items WHERE type = 'SERIE' ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentSeriesSync(limit: Int): List<ContentEntity>

    @Query("SELECT * FROM content_items WHERE id = :id LIMIT 1")
    fun getByIdSync(id: Long): ContentEntity?

    @Query("UPDATE content_items SET episode = :episode, updatedAt = :now WHERE id = :id")
    fun updateEpisodeSync(id: Long, episode: Int, now: Long)

    @Query("UPDATE content_items SET season = :season, updatedAt = :now WHERE id = :id")
    fun updateSeasonSync(id: Long, season: Int, now: Long)

    @Query("UPDATE content_items SET minute = :minute, updatedAt = :now WHERE id = :id")
    fun updateMinuteSync(id: Long, minute: Int, now: Long)

    @Query("UPDATE content_items SET isPinnedToWidget = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinnedToWidget(id: Long, pinned: Boolean, now: Long)
}
