package com.blackphantom.dondemierdaiba.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object EpisodeCheckScheduler {

    private const val PERIODIC_WORK_NAME = "new_episode_check_periodic"
    private const val ONE_TIME_WORK_NAME = "new_episode_check_now"

    /** Se llama una vez al iniciar la app. No hace nada si ya estaba programado. */
    fun schedulePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NewEpisodeCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Chequeo inmediato bajo demanda (ej: botón "Buscar novedades" en el header). */
    fun checkNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<NewEpisodeCheckWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
