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
        private val platformDot: android.view.View? = itemView.findViewById(R.id.platformDot)
        private val txtPlatformInitial: TextView? = itemView.findViewById(R.id.txtPlatformInitial)

        fun bind(item: ContentEntity, onClick: (ContentEntity) -> Unit) {
            txtName.text = item.name

            txtMeta.text = when (item.typeEnum) {
                ContentType.SERIE -> itemView.context.getString(
                    R.string.season_short
                ) + "${item.season} · " + itemView.context.getString(R.string.episode_short) +
                    "${item.episode} · ${item.minute} ${itemView.context.getString(R.string.minute_short)}"
                ContentType.PELICULA -> "${item.minute} ${itemView.context.getString(R.string.minute_short)}"
                ContentType.VIDEO -> {
                    val platform = VideoPlatform.fromNameOrNull(item.platform)
                    val platformLabel = platform?.label ?: ""
                    "$platformLabel · ${item.minute} ${itemView.context.getString(R.string.minute_short)}"
                }
            }

            if (item.typeEnum == ContentType.VIDEO) {
                val platform = VideoPlatform.fromNameOrNull(item.platform)
                platformDot?.visibility = android.view.View.VISIBLE
                platformDot?.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(platform?.colorHex ?: "#8B5CF6"))
                }
            } else {
                platformDot?.visibility = android.view.View.GONE
            }

            val corner = RoundedCorners(20)
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
                    val colorHex = platform?.colorHex ?: "#8B5CF6"
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
