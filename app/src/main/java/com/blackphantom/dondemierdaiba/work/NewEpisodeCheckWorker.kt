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
import com.blackphantom.dondemierdaiba.util.EpisodeCounts
import com.blackphantom.dondemierdaiba.util.NotificationHelper
import com.blackphantom.dondemierdaiba.widget.SeriesWidgetProvider

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

        var hayCambios = false

        for (item in linkedSeries) {
            val showId = item.tvMazeId ?: continue
            val latest = TvMazeApi.latestAiredEpisode(showId) ?: continue

            val hayNovedad = latest.season > item.remoteSeason ||
                (latest.season == item.remoteSeason && latest.number > item.remoteEpisode)

            if (hayNovedad) {
                hayCambios = true

                if (canNotify) {
                    NotificationHelper.notifyNewEpisode(applicationContext, item, latest)
                }

                // Aprovechamos que ya sabemos que hay algo nuevo para refrescar
                // también cuántos capítulos tiene cada temporada (así el
                // candado de capítulo del editor y del widget quedan al día).
                val counts = TvMazeApi.episodesBySeason(showId)
                val countsJson = if (counts != null) EpisodeCounts.toJson(counts) else item.episodeCountsJson

                dao.update(
                    item.copy(
                        remoteSeason = latest.season,
                        remoteEpisode = latest.number,
                        episodeCountsJson = countsJson
                    )
                )
            }
        }

        // Si hubo novedades (ya sea por el chequeo automático o por el botón
        // del widget), refrescamos el widget para que se vea al toque.
        if (hayCambios) {
            SeriesWidgetProvider.requestRefresh(applicationContext)
        }

        return Result.success()
    }
}
