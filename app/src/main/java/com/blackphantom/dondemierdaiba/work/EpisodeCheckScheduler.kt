package com.blackphantom.dondemierdaiba.work

import android.content.Context
import androidx.work.*
import com.blackphantom.dondemierdaiba.update.UpdateCheckWorker
import com.blackphantom.dondemierdaiba.util.Prefs
import java.util.concurrent.TimeUnit

object EpisodeCheckScheduler {

    private const val PERIODIC_WORK_NAME = "new_episode_check_periodic"
    private const val ONE_TIME_WORK_NAME = "new_episode_check_now"
    private const val UPDATE_PERIODIC_NAME = "app_update_check_periodic"
    private const val UPDATE_ONE_TIME_NAME = "app_update_check_now"
    private const val UPDATE_STALE_MS = 6L * 60 * 60 * 1000

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

    /** Chequeo de versión nueva de la app (GitHub) cada 12 hs. Si la persona lo apagó, se cancela. */
    fun scheduleUpdateCheck(context: Context) {
        val wm = WorkManager.getInstance(context)
        if (!Prefs.isUpdateCheckEnabled(context)) {
            wm.cancelUniqueWork(UPDATE_PERIODIC_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(UPDATE_PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Al abrir la app: si hace más de 6 hs que no se mira, se mira ahora (así el aviso no espera al ciclo). */
    fun checkUpdateIfStale(context: Context) {
        if (!Prefs.isUpdateCheckEnabled(context)) return
        if (System.currentTimeMillis() - Prefs.getLastUpdateCheck(context) < UPDATE_STALE_MS) return
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UPDATE_ONE_TIME_NAME, ExistingWorkPolicy.KEEP, request)
    }
}
