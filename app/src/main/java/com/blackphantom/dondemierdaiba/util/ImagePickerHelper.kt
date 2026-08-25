package com.blackphantom.dondemierdaiba.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.blackphantom.dondemierdaiba.R
import com.google.android.material.bottomsheet.BottomSheetDialog

object ImagePickerHelper {

    fun showImageSourceChooser(
        context: Context,
        onPickGallery: () -> Unit,
        onWebUrl: (String) -> Unit
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
            webUrlRow.visibility = if (webUrlRow.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        view.findViewById<View>(R.id.btnConfirmWebUrl).setOnClickListener {
            val url = edtWebUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                dialog.dismiss()
                onWebUrl(url)
            }
        }

        dialog.show()
    }
}
