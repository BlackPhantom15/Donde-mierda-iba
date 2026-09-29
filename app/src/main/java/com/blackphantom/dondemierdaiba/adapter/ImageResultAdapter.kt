package com.blackphantom.dondemierdaiba.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.network.WikimediaApi

class ImageResultAdapter(
    private val onClick: (WikimediaApi.ImageResult) -> Unit
) : ListAdapter<WikimediaApi.ImageResult, ImageResultAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_result, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)

        fun bind(item: WikimediaApi.ImageResult, onClick: (WikimediaApi.ImageResult) -> Unit) {
            // Radio en píxeles equivalente a 14dp: sin convertir por
            // densidad, quedaba más chico que el de la tarjeta contenedora.
            val cornerPx = (14 * itemView.context.resources.displayMetrics.density).toInt()
            Glide.with(itemView)
                .load(item.thumbnailUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .transform(CenterCrop(), RoundedCorners(cornerPx))
                .into(imgThumb)

            itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WikimediaApi.ImageResult>() {
            override fun areItemsTheSame(oldItem: WikimediaApi.ImageResult, newItem: WikimediaApi.ImageResult) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: WikimediaApi.ImageResult, newItem: WikimediaApi.ImageResult) =
                oldItem == newItem
        }
    }
}
