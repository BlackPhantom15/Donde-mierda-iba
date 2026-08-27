package com.blackphantom.dondemierdaiba.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.network.TvMazeApi
import com.blackphantom.dondemierdaiba.util.NotificationHelper

/**
 * Recorre las series vinculadas a TVmaze y, si hay un capítulo o temporada
 * nueva ya emitida (no futura), dispara una notificación local y actualiza
 * lo que ya sabíamos para no volver a avisar de lo mismo.
 */
class NewEpisodeCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).contentDao()
        val linkedSeries = dao.getSeriesLinkedToTvMaze()

        if (linkedSeries.isEmpty()) return Result.success()

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        for (item in linkedSeries) {
            val showId = item.tvMazeId ?: continue
            val latest = TvMazeApi.latestAiredEpisode(showId) ?: continue

            val hayNovedad = latest.season > item.remoteSeason ||
                (latest.season == item.remoteSeason && latest.number > item.remoteEpisode)

            if (hayNovedad) {
                if (canNotify) {
                    NotificationHelper.notifyNewEpisode(applicationContext, item, latest)
                }
                dao.update(
                    item.copy(
                        remoteSeason = latest.season,
                        remoteEpisode = latest.number
                    )
                )
            }
        }

        return Result.success()
    }
}
