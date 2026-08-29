package com.blackphantom.dondemierdaiba.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.AppDatabase
import com.blackphantom.dondemierdaiba.data.ContentEntity
import com.blackphantom.dondemierdaiba.data.ImageSource
import java.util.concurrent.TimeUnit

class SeriesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return SeriesWidgetFactory(applicationContext)
    }
}

class SeriesWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<ContentEntity> = emptyList()
    private val thumbSizePx by lazy { (40 * context.resources.displayMetrics.density).toInt() }

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Corre en el hilo del widget (no el hilo principal), por eso acá sí
        // podemos hacer la consulta de forma síncrona y bloqueante.
        val dao = AppDatabase.getInstance(context).contentDao()
        val pinned = dao.getPinnedSeriesSync()
        // Si todavía no marcaste ninguna serie como "viendo ahora", mostramos
        // las más recientes para que el widget no quede vacío de entrada.
        items = pinned.ifEmpty { dao.getRecentSeriesSync(6) }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_item_series)

        rv.setTextViewText(R.id.widgetItemName, item.name)
        rv.setTextViewText(
            R.id.widgetItemMeta,
            "T${item.season} · C${String.format("%02d", item.episode)} · ${item.minute} min"
        )
        rv.setTextColor(R.id.widgetItemMeta, metaColorFor(item))

        // Miniatura real si el usuario cargó una (galería o web); si no,
        // un punto de color generado a partir del id, para que cada fila
        // se distinga de un vistazo aunque no tenga imagen.
        val bitmap = loadThumbnail(item)
        if (bitmap != null) {
            rv.setImageViewBitmap(R.id.widgetThumb, bitmap)
            rv.setViewVisibility(R.id.widgetThumb, android.view.View.VISIBLE)
            rv.setViewVisibility(R.id.widgetDot, android.view.View.GONE)
        } else {
            val hue = (item.id * 47 % 360).toFloat()
            val color = Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.95f))
            rv.setInt(R.id.widgetDot, "setColorFilter", color)
            rv.setViewVisibility(R.id.widgetThumb, android.view.View.GONE)
            rv.setViewVisibility(R.id.widgetDot, android.view.View.VISIBLE)
        }

        // Tocar el nombre/meta abre la app directo en el editor de esa serie.
        rv.setOnClickFillInIntent(R.id.widgetRowContent, fillIntent(SeriesWidgetProvider.ACTION_OPEN_ITEM, item.id))

        // Steppers rápidos: temporada +1, capítulo -/+1, minuto +5.
        rv.setOnClickFillInIntent(R.id.widgetBtnSeasonPlus, fillIntent(SeriesWidgetProvider.ACTION_INC_SEASON, item.id))
        rv.setOnClickFillInIntent(R.id.widgetBtnMinus, fillIntent(SeriesWidgetProvider.ACTION_DEC_EPISODE, item.id))
        rv.setOnClickFillInIntent(R.id.widgetBtnPlus, fillIntent(SeriesWidgetProvider.ACTION_INC_EPISODE, item.id))
        rv.setOnClickFillInIntent(R.id.widgetBtnMinutePlus, fillIntent(SeriesWidgetProvider.ACTION_INC_MINUTE, item.id))

        return rv
    }

    private fun fillIntent(action: String, itemId: Long): Intent = Intent().apply {
        putExtra(SeriesWidgetProvider.EXTRA_ACTION, action)
        putExtra(SeriesWidgetProvider.EXTRA_ITEM_ID, itemId)
    }

    /** Mismo criterio de color que usa la lista principal de la app. */
    private fun metaColorFor(item: ContentEntity): Int {
        val hasRemoteInfo = item.tvMazeId != null && item.remoteSeason > 0
        if (!hasRemoteInfo) return Color.parseColor("#B6B0D6") // gris/lila neutro

        val isUpToDate = item.season > item.remoteSeason ||
            (item.season == item.remoteSeason && item.episode >= item.remoteEpisode)

        return if (isUpToDate) Color.parseColor("#22D3EE") else Color.parseColor("#F472B6")
    }

    /**
     * Carga la miniatura de forma síncrona y bloqueante (estamos en el hilo
     * del proceso del widget, no en el hilo principal de la UI, así que esto
     * no cuelga nada). Con timeout corto para no dejar el widget colgado si
     * la imagen es una URL lenta o no responde.
     */
    private fun loadThumbnail(item: ContentEntity): Bitmap? {
        if (item.imageSourceEnum != ImageSource.LOCAL && item.imageSourceEnum != ImageSource.WEB) return null
        val uriOrUrl = item.imageUri ?: return null

        return try {
            Glide.with(context)
                .asBitmap()
                .load(Uri.parse(uriOrUrl))
                .transform(CircleCrop())
                .submit(thumbSizePx, thumbSizePx)
                .get(2500, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            null
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = items[position].id
    override fun hasStableIds(): Boolean = true
}
