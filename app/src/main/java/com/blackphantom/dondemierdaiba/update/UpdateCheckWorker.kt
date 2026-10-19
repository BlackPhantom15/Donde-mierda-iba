package com.blackphantom.dondemierdaiba.update

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blackphantom.dondemierdaiba.util.NotificationHelper
import com.blackphantom.dondemierdaiba.util.Prefs

/**
 * Revisa en segundo plano si GitHub tiene una versión nueva y avisa UNA sola
 * vez por versión. No descarga ni instala nada: eso lo decide la persona
 * desde el diálogo de actualización.
 */
class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!Prefs.isUpdateCheckEnabled(ctx)) return Result.success()

        when (val result = UpdateManager.check(ctx)) {
            is UpdateManager.CheckResult.Available -> {
                val tag = result.release.tag
                Prefs.setPendingUpdate(ctx, tag)

                val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED

                if (tag != Prefs.getLastNotifiedUpdate(ctx) && canNotify) {
                    NotificationHelper.notifyUpdateAvailable(ctx, tag)
                    Prefs.setLastNotifiedUpdate(ctx, tag)
                }
            }
            is UpdateManager.CheckResult.UpToDate -> Prefs.setPendingUpdate(ctx, "")
            is UpdateManager.CheckResult.Error -> Unit // sin internet: se reintenta en el próximo ciclo
        }
        Prefs.setLastUpdateCheck(ctx, System.currentTimeMillis())
        return Result.success()
    }
}
