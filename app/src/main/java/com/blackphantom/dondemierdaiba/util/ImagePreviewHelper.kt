package com.blackphantom.dondemierdaiba.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.blackphantom.dondemierdaiba.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.setBackgroundResource(android.R.color.transparent)
            if (sheet != null) {
                // Sin esto, el sheet arranca "colapsado" mostrando solo una
                // porción del contenido (como un peek), y la imagen se ve
                // cortada hasta que el usuario lo arrastra hacia arriba a
                // mano. Lo forzamos a abrir siempre expandido del todo.
                BottomSheetBehavior.from(sheet).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                }
            }
        }

        val imgFull = view.findViewById<ImageView>(R.id.imgFullPreview)
        Glide.with(context)
            .load(imageUri)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            // Tamaño de destino fijo: evita que Glide dependa de medir el
            // ImageView (que dentro de un BottomSheetDialog recién creado
            // puede no tener todavía un ancho/alto definitivo) y termine
            // pidiendo un bitmap más chico de lo que después se muestra.
            .override(1080, 1080)
            .transform(RoundedCorners(28))
            .into(imgFull)

        view.findViewById<View>(R.id.btnClosePreview).setOnClickListener { dialog.dismiss() }
        view.findViewById<View>(R.id.btnChangeImage).setOnClickListener {
            dialog.dismiss()
            onChangeClick()
        }

        dialog.show()
    }
}
