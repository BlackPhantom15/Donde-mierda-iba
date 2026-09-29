package com.blackphantom.dondemierdaiba.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.ContentEntity
import com.blackphantom.dondemierdaiba.network.TvMazeApi
import com.blackphantom.dondemierdaiba.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "new_episodes_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyNewEpisode(context: Context, item: ContentEntity, episode: TvMazeApi.LatestEpisode) {
        val isNewSeason = episode.season > item.remoteSeason
        val title = if (isNewSeason) {
            context.getString(R.string.notif_new_season_title, item.name)
        } else {
            context.getString(R.string.notif_new_episode_title, item.name)
        }
        val body = context.getString(
            R.string.notif_new_episode_body,
            episode.season, episode.number
        ) + if (episode.name.isNotBlank()) " · ${episode.name}" else ""

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } catch (e: Exception) {
            null
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setLargeIcon(largeIcon)
            .setColor(context.getColor(R.color.accent_orange))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // item.id (Long) cabe siempre en un Int para este uso: es solo un ID de
        // notificación local, no se persiste ni se compara fuera de la app.
        NotificationManagerCompat.from(context).notify(item.id.toInt(), notification)
    }
}
