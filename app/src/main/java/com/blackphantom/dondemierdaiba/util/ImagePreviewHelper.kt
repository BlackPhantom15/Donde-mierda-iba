package com.blackphantom.dondemierdaiba.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.blackphantom.dondemierdaiba.R
import com.google.android.material.bottomsheet.BottomSheetDialog

object ImagePreviewHelper {

    /**
     * Muestra la imagen ya elegida en grande, con opción de cambiarla o
     * simplemente cerrar. Solo tiene sentido para imágenes reales (galería o
     * web); los badges de plataforma de los videos van directo al selector.
     */
    fun showPreview(
        context: Context,
        imageUri: String,
        onChangeClick: () -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_image_preview, null)
        dialog.setContentView(view)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }

        val imgFull = view.findViewById<ImageView>(R.id.imgFullPreview)
        Glide.with(context)
            .load(imageUri)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .transform(CenterCrop(), RoundedCorners(28))
            .into(imgFull)

        view.findViewById<View>(R.id.btnClosePreview).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnChangeImage).setOnClickListener {
            dialog.dismiss()
            onChangeClick()
        }

        dialog.show()
    }
}
