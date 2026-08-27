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
import com.blackphantom.dondemierdaiba.network.OpenverseApi

class ImageResultAdapter(
    private val onClick: (OpenverseApi.ImageResult) -> Unit
) : ListAdapter<OpenverseApi.ImageResult, ImageResultAdapter.VH>(DIFF) {

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

        fun bind(item: OpenverseApi.ImageResult, onClick: (OpenverseApi.ImageResult) -> Unit) {
            Glide.with(itemView)
                .load(item.thumbnailUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .transform(CenterCrop(), RoundedCorners(14))
                .into(imgThumb)

            itemView.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OpenverseApi.ImageResult>() {
            override fun areItemsTheSame(oldItem: OpenverseApi.ImageResult, newItem: OpenverseApi.ImageResult) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: OpenverseApi.ImageResult, newItem: OpenverseApi.ImageResult) =
                oldItem == newItem
        }
    }
}
