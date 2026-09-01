package com.blackphantom.dondemierdaiba.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object EpisodeCheckScheduler {

    private const val PERIODIC_WORK_NAME = "new_episode_check_periodic"
    private const val ONE_TIME_WORK_NAME = "new_episode_check_now"

    /** Se llama una vez al iniciar la app. Actualiza el intervalo si ya estaba programado con otro. */
    fun schedulePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 6hs en vez de 12: como Android igual puede demorar el chequeo por
        // ahorro de batería (sobre todo en Xiaomi/Samsung/Huawei y otros
        // fabricantes agresivos con las apps en segundo plano), acortar el
        // intervalo reduce el peor caso de demora sin ser tan seguido como
        // para pesar en la batería.
        val request = PeriodicWorkRequestBuilder<NewEpisodeCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
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
