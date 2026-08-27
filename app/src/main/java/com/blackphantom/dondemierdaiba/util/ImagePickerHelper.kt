package com.blackphantom.dondemierdaiba.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import com.blackphantom.dondemierdaiba.R
import com.blackphantom.dondemierdaiba.dialogs.ImageWebSearchDialog
import com.google.android.material.bottomsheet.BottomSheetDialog

object ImagePickerHelper {

    /**
     * Muestra el selector de origen de imagen: galería, buscador web (grilla de
     * fotos libres de copyright, tocás y listo, como al cambiar la foto de un
     * grupo de WhatsApp) o, como opción secundaria, pegar un link a mano.
     *
     * [fragmentManager] debe ser el childFragmentManager del fragment que llama
     * a esto, para poder anidar el ImageWebSearchDialog y recibir su resultado
     * vía FragmentResult (ver REQUEST_KEY en ImageWebSearchDialog).
     */
    fun showImageSourceChooser(
        context: Context,
        fragmentManager: FragmentManager,
        initialQuery: String = "",
        onPickGallery: () -> Unit,
        onManualUrl: (String) -> Unit
    ) {
        val dialog = BottomSheetDialog(context)
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog_image_source, null)
        dialog.setContentView(view)
        dialog.setOnShowListener {
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }

        val webUrlRow = view.findViewById<View>(R.id.webUrlRow)
        val edtWebUrl = view.findViewById<android.widget.EditText>(R.id.edtWebUrl)

        view.findViewById<View>(R.id.btnGallery).setOnClickListener {
            dialog.dismiss()
            onPickGallery()
        }

        view.findViewById<View>(R.id.btnWeb).setOnClickListener {
            dialog.dismiss()
            ImageWebSearchDialog.newInstance(initialQuery).show(fragmentManager, "image_web_search")
        }

        view.findViewById<View>(R.id.btnManualLink).setOnClickListener {
            webUrlRow.visibility = if (webUrlRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        view.findViewById<View>(R.id.btnConfirmWebUrl).setOnClickListener {
            val url = edtWebUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                dialog.dismiss()
                onManualUrl(url)
            }
        }

        dialog.show()
    }
}
