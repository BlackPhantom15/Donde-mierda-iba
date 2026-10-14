package com.blackphantom.dondemierdaiba.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Historial de avisos que mandó la app (capítulo o temporada nueva), para que
 * se puedan volver a ver aunque se borre la notificación del sistema.
 */
@Entity(tableName = "notification_log")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val title: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
