package com.blackphantom.dondemierdaiba.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.data.ContentEntity
import com.blackphantom.dondemierdaiba.data.ContentType
import com.blackphantom.dondemierdaiba.data.ImageSource
import com.blackphantom.dondemierdaiba.data.VideoPlatform
import com.blackphantom.dondemierdaiba.util.PlatformVisuals
import com.blackphantom.dondemierdaiba.util.DurationFormatter
import com.blackphantom.dondemierdaiba.util.EpisodeWatched
import com.google.android.material.card.MaterialCardView
import androidx.core.content.ContextCompat

class ContentAdapter(
    private var isGrid: Boolean,
    private val onClick: (ContentEntity) -> Unit
) : ListAdapter<ContentEntity, ContentAdapter.VH>(DIFF) {

    fun setGridMode(grid: Boolean) {
        if (isGrid != grid) {
            isGrid = grid
            notifyDataSetChanged()
        }
    }

    override fun getItemViewType(position: Int) = if (isGrid) TYPE_GRID else TYPE_LIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (viewType == TYPE_GRID) R.layout.item_content_grid else R.layout.item_content_list
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)
        private val txtName: TextView = itemView.findViewById(R.id.txtName)
        private val txtMeta: TextView = itemView.findViewById(R.id.txtMeta)
        private val txtRemoteInfo: TextView? = itemView.findViewById(R.id.txtRemoteInfo)
        private val platformDot: android.view.View? = itemView.findViewById(R.id.platformDot)
        private val txtPlatformInitial: TextView? = itemView.findViewById(R.id.txtPlatformInitial)
        private val imgFavorite: ImageView? = itemView.findViewById(R.id.imgFavorite)

        fun bind(item: ContentEntity, onClick: (ContentEntity) -> Unit) {
            txtName.text = item.name
            imgFavorite?.visibility = if (item.isFavorite) android.view.View.VISIBLE else android.view.View.GONE

            txtMeta.text = when (item.typeEnum) {
                ContentType.SERIE -> itemView.context.getString(
                    R.string.season_short
                ) + "${item.season} · " + itemView.context.getString(R.string.episode_short) +
                    "${item.episode} · ${DurationFormatter.format(item.minute)}"
                ContentType.PELICULA -> DurationFormatter.format(item.minute)
                ContentType.VIDEO -> {
                    val platform = VideoPlatform.fromNameOrNull(item.platform)
                    val platformLabel = platform?.label ?: ""
                    "$platformLabel · ${DurationFormatter.format(item.minute)}"
                }
            }

            if (item.typeEnum == ContentType.VIDEO) {
                val platform = VideoPlatform.fromNameOrNull(item.platform)
                platformDot?.visibility = android.view.View.VISIBLE
                platformDot?.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(platform?.colorHex ?: "#E8792E"))
                }
            } else {
                platformDot?.visibility = android.view.View.GONE
            }

            // Para series vinculadas a TVmaze: mostramos chiquito hasta dónde va
            // lo último emitido, para que de un vistazo se note si te quedaste
            // atrás ("che, me faltan 2 temporadas").
            val hasRemoteInfo = item.typeEnum == ContentType.SERIE &&
                item.tvMazeId != null && item.remoteSeason > 0
            if (hasRemoteInfo) {
                val isUpToDate = item.season > item.remoteSeason ||
                    (item.season == item.remoteSeason && item.episode >= item.remoteEpisode)
                txtRemoteInfo?.visibility = android.view.View.VISIBLE
                if (isUpToDate) {
                    txtRemoteInfo?.text = itemView.context.getString(R.string.remote_up_to_date)
                    txtRemoteInfo?.setTextColor(
                        androidx.core.content.ContextCompat.getColor(itemView.context, R.color.accent_cyan)
                    )
                } else {
                    txtRemoteInfo?.text = itemView.context.getString(
                        R.string.remote_latest, item.remoteSeason, item.remoteEpisode
                    )
                    txtRemoteInfo?.setTextColor(
                        androidx.core.content.ContextCompat.getColor(itemView.context, R.color.accent_pink)
                    )
                }
            } else {
                txtRemoteInfo?.visibility = android.view.View.GONE
            }

            // Halo verde fino: se resalta el contenido que ya se terminó de
            // ver por completo (serie con todas las temporadas conocidas
            // vistas, o película/video marcados como vistos).
            val isFullyCompleted = when (item.typeEnum) {
                ContentType.SERIE -> EpisodeWatched.isSeriesComplete(item.episodeWatchedJson, item.episodeCountsJson)
                else -> item.isCompleted
            }
            (itemView as? MaterialCardView)?.apply {
                if (isFullyCompleted) {
                    strokeColor = ContextCompat.getColor(itemView.context, R.color.completed_green)
                    strokeWidth = (2 * itemView.context.resources.displayMetrics.density).toInt()
                } else {
                    strokeColor = ContextCompat.getColor(itemView.context, R.color.glass_stroke)
                    strokeWidth = (1 * itemView.context.resources.displayMetrics.density).toInt()
                }
            }

            // Radio en píxeles equivalente a los 16dp de bg_glass_input: sin
            // convertir por densidad, en pantallas de alta densidad el
            // recorte quedaba más chico que el marco y se veían las puntas
            // cuadradas de la foto asomando por las esquinas.
            val corner = RoundedCorners((16 * itemView.context.resources.displayMetrics.density).toInt())
            when (item.imageSourceEnum) {
                ImageSource.LOCAL, ImageSource.WEB -> {
                    imgThumb.background = itemView.context.getDrawable(R.drawable.bg_glass_input)
                    Glide.with(itemView)
                        .load(item.imageUri)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .transform(CenterCrop(), corner)
                        .into(imgThumb)
                }
                ImageSource.PRESET -> {
                    val platform = VideoPlatform.fromNameOrNull(item.imageUri)
                    val colorHex = platform?.colorHex ?: "#E8792E"
                    Glide.with(itemView).clear(imgThumb)
                    imgThumb.setImageDrawable(null)
                    imgThumb.background = PlatformVisuals.glassBackground(itemView.context, colorHex)
                    txtPlatformInitial?.apply {
                        visibility = android.view.View.VISIBLE
                        text = PlatformVisuals.initial(platform)
                        setTextColor(Color.parseColor(colorHex))
                    }
                }
                ImageSource.NONE -> {
                    imgThumb.background = itemView.context.getDrawable(R.drawable.bg_glass_input)
                    imgThumb.setImageResource(R.drawable.ic_image_placeholder)
                }
            }

            if (item.imageSourceEnum != ImageSource.PRESET) {
                txtPlatformInitial?.visibility = android.view.View.GONE
            }

            itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        const val TYPE_LIST = 0
        const val TYPE_GRID = 1

        private val DIFF = object : DiffUtil.ItemCallback<ContentEntity>() {
            override fun areItemsTheSame(oldItem: ContentEntity, newItem: ContentEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ContentEntity, newItem: ContentEntity) =
                oldItem == newItem
        }
    }
}
