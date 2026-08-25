package com.blackphantom.dondemierdaiba.util

import android.graphics.Paint
import android.widget.EditText
import android.widget.NumberPicker

/**
 * El framework de Android no expone un atributo XML para el color de texto de un
 * NumberPicker, así que hay que forzarlo por reflexión. Sin esto, el texto queda
 * con el color por defecto del tema y puede volverse invisible según el fondo
 * (como pasaba con los "candados" de temporada/capítulo/minuto).
 */
object NumberPickerStyler {

    fun style(picker: NumberPicker, textColor: Int, dividerColor: Int? = null) {
        // Color del número actualmente seleccionado (el EditText interno)
        for (i in 0 until picker.childCount) {
            val child = picker.getChildAt(i)
            if (child is EditText) {
                child.setTextColor(textColor)
            }
        }

        // Color de los números de arriba/abajo (la "rueda")
        try {
            val paintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
            paintField.isAccessible = true
            val paint = paintField.get(picker) as Paint
            paint.color = textColor
            picker.invalidate()
        } catch (_: Exception) {
            // Si el fabricante cambió el nombre interno del campo, no hacemos nada:
            // no es crítico, solo perdemos el color de la rueda superior/inferior.
        }

        if (dividerColor != null) {
            try {
                val dividerField = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
                dividerField.isAccessible = true
                dividerField.set(picker, android.graphics.drawable.ColorDrawable(dividerColor))
            } catch (_: Exception) {
                // idem, no crítico
            }
        }
    }
}
