package com.blackphantom.dondemierdaiba.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.ColorUtils
import com.blackphantom.dondemierdaiba.data.VideoPlatform

/**
 * Genera el look "vidrio esmerilado" (glassmorphism) que usamos para representar
 * el ícono de una plataforma de video cuando no hay una imagen custom: un relleno
 * translúcido del color de la plataforma + un borde sutil, en vez de un cuadrado
 * de color plano.
 */
object PlatformVisuals {

    fun glassBackground(context: Context, colorHex: String, cornerRadiusDp: Float = 16f): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val color = Color.parseColor(colorHex)
        val fill = ColorUtils.setAlphaComponent(color, 46) // ~18% opacidad
        val stroke = ColorUtils.setAlphaComponent(color, 130) // ~51% opacidad
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusDp * density
            setColor(fill)
            setStroke((1.5f * density).toInt(), stroke)
        }
    }

    fun initial(platform: VideoPlatform?): String =
        (platform?.label?.firstOrNull() ?: '?').uppercase()
}
