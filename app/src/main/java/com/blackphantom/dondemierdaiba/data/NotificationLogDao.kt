package com.blackphantom.dondemierdaiba.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NotificationLogDao {

    @Insert
    suspend fun insert(entry: NotificationLogEntity): Long

    @Query("SELECT * FROM notification_log ORDER BY timestamp DESC")
    fun observeAll(): LiveData<List<NotificationLogEntity>>

    @Query("SELECT COUNT(*) FROM notification_log WHERE isRead = 0")
    fun observeUnreadCount(): LiveData<Int>

    @Query("UPDATE notification_log SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllRead()

    @Query("DELETE FROM notification_log")
    suspend fun clearAll()

    @Query("DELETE FROM notification_log WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Mantenemos solo los últimos 100 avisos para que la tabla no crezca sin fin.
    @Query("DELETE FROM notification_log WHERE id NOT IN (SELECT id FROM notification_log ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trim()
}
