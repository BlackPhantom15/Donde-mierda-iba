package com.blackphantom.dondemierdaiba.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.ui.MainActivity
import com.blackphantom.dondemierdaiba.util.EpisodeCounts
import com.blackphantom.dondemierdaiba.work.EpisodeCheckScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget de pantalla de inicio "Viendo ahora". Muestra las series marcadas
 * como pin (o las más recientes si no hay ninguna marcada) y deja sumar o
 * restar capítulos sin abrir la app, tocando los botones +/- de cada fila.
 * Tocar el nombre de la serie sí abre la app, directo en su editor.
 */
class SeriesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context, appWidgetId = widgetId))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_CHECK_UPDATES) {
            EpisodeCheckScheduler.checkNow(context)
            Toast.makeText(context, R.string.checking_updates, Toast.LENGTH_SHORT).show()
            return
        }

        val action = intent.getStringExtra(EXTRA_ACTION) ?: return
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        if (itemId <= 0) return

        when (action) {
            ACTION_OPEN_ITEM -> {
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(MainActivity.EXTRA_OPEN_EDIT_ITEM_ID, itemId)
                }
                context.startActivity(openIntent)
            }
            ACTION_INC_EPISODE, ACTION_DEC_EPISODE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getInstance(context).contentDao()
                        val item = dao.getByIdSync(itemId)
                        if (item != null) {
                            val delta = if (action == ACTION_INC_EPISODE) 1 else -1
                            val maxForSeason = EpisodeCounts.fromJson(item.episodeCountsJson)[item.season] ?: 999
                            val newEpisode = (item.episode + delta).coerceIn(1, maxOf(1, maxForSeason))

                            if (action == ACTION_INC_EPISODE && item.episode >= maxForSeason && maxForSeason < 999) {
                                showToastOnMainThread(context, R.string.widget_season_complete)
                            } else {
                                dao.updateEpisodeSync(itemId, newEpisode, System.currentTimeMillis())
                                requestRefresh(context)
                            }
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_INC_SEASON -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getInstance(context).contentDao()
                        val item = dao.getByIdSync(itemId)
                        if (item != null) {
                            val counts = EpisodeCounts.fromJson(item.episodeCountsJson)
                            val maxSeason = counts.keys.maxOrNull() ?: 200

                            if (item.season >= maxSeason && counts.isNotEmpty()) {
                                showToastOnMainThread(context, R.string.widget_no_more_seasons)
                            } else {
                                val newSeason = (item.season + 1).coerceIn(1, maxOf(1, maxSeason))
                                dao.updateSeasonSync(itemId, newSeason, System.currentTimeMillis())
                                requestRefresh(context)
                            }
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_INC_MINUTE -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getInstance(context).contentDao()
                        val item = dao.getByIdSync(itemId)
                        if (item != null) {
                            // De a 5 minutos: sumar de a uno sería demasiado lento de usar.
                            val newMinute = (item.minute + 5).coerceIn(0, 999)
                            dao.updateMinuteSync(itemId, newMinute, System.currentTimeMillis())
                            requestRefresh(context)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    override fun onEnabled(context: Context) {}
    override fun onDisabled(context: Context) {}

    companion object {
        const val ACTION_WIDGET_CLICK = "com.blackphantom.dondemierdaiba.widget.ACTION_WIDGET_CLICK"
        const val ACTION_CHECK_UPDATES = "com.blackphantom.dondemierdaiba.widget.ACTION_CHECK_UPDATES"
        const val EXTRA_ACTION = "extra_widget_action"
        const val EXTRA_ITEM_ID = "extra_widget_item_id"

        const val ACTION_OPEN_ITEM = "OPEN_ITEM"
        const val ACTION_INC_EPISODE = "INC_EPISODE"
        const val ACTION_DEC_EPISODE = "DEC_EPISODE"
        const val ACTION_INC_SEASON = "INC_SEASON"
        const val ACTION_INC_MINUTE = "INC_MINUTE"

        fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews {
            val rv = RemoteViews(context.packageName, R.layout.widget_series_list)

            // Adapter de la lista (RemoteViewsService)
            val serviceIntent = Intent(context, SeriesWidgetService::class.java).apply {
                data = android.net.Uri.parse("content://widget/$appWidgetId")
            }
            rv.setRemoteAdapter(R.id.widgetListView, serviceIntent)
            rv.setEmptyView(R.id.widgetListView, R.id.widgetEmptyView)

            // Plantilla de PendingIntent compartida por todas las filas: cada
            // fila completa sus propios extras vía setOnClickFillInIntent
            // (ver SeriesWidgetFactory). Tiene que ser mutable para eso.
            val templateIntent = Intent(context, SeriesWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_CLICK
            }
            val templatePendingIntent = PendingIntent.getBroadcast(
                context, 0, templateIntent, mutableFlags()
            )
            rv.setPendingIntentTemplate(R.id.widgetListView, templatePendingIntent)

            // Botón "+" del header: abre la app directo en "agregar serie".
            val addIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(MainActivity.EXTRA_OPEN_ADD_SERIE, true)
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, 1, addIntent, immutableFlags()
            )
            rv.setOnClickPendingIntent(R.id.widgetBtnAdd, addPendingIntent)

            // Botón de "buscar novedades" del header: dispara el chequeo contra
            // TVmaze en segundo plano sin abrir la app.
            val checkUpdatesIntent = Intent(context, SeriesWidgetProvider::class.java).apply {
                action = ACTION_CHECK_UPDATES
            }
            val checkUpdatesPendingIntent = PendingIntent.getBroadcast(
                context, 2, checkUpdatesIntent, immutableFlags()
            )
            rv.setOnClickPendingIntent(R.id.widgetBtnCheckUpdates, checkUpdatesPendingIntent)

            return rv
        }

        /** Pide refrescar todas las instancias del widget (ej: después de editar desde la app). */
        fun requestRefresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SeriesWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widgetListView)
            }
        }

        private fun mutableFlags(): Int {
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = flags or PendingIntent.FLAG_MUTABLE
            }
            return flags
        }

        private fun immutableFlags(): Int {
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }
            return flags
        }

        /** Toast.makeText necesita el hilo principal; acá estamos en uno de fondo. */
        private fun showToastOnMainThread(context: Context, resId: Int) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
