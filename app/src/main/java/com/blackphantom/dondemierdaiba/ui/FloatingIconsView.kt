package com.blackphantom.dondemierdaiba.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.blackphantom.dondemierdaiba.R
import kotlin.random.Random

/**
 * Fondo animado y liviano: dibuja unos pocos iconos genéricos (play, claqueta,
 * tv, rollo de película) que flotan lentamente de abajo hacia arriba, sin
 * usar ningún logo con copyright. Todo se dibuja en un solo Canvas para que
 * no pese en dispositivos viejos.
 */
class FloatingIconsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class FloatIcon(
        var x: Float,
        var y: Float,
        val speed: Float,
        val size: Float,
        val alpha: Int,
        val bitmap: Bitmap,
        val drift: Float
    )

    private val icons = mutableListOf<FloatIcon>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    private var initialized = false

    private val drawableRes = intArrayOf(
        R.drawable.ic_float_play,
        R.drawable.ic_float_clapper,
        R.drawable.ic_float_tv,
        R.drawable.ic_float_reel
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && !initialized) {
            setupIcons(w, h)
            initialized = true
            startAnimation()
        }
    }

    private fun setupIcons(w: Int, h: Int) {
        icons.clear()
        val count = 10 // pocos iconos: liviano en gama baja
        repeat(count) {
            val res = drawableRes[Random.nextInt(drawableRes.size)]
            val drawable = ContextCompat.getDrawable(context, res) ?: return@repeat
            val size = Random.nextInt(28, 56).toFloat()
            val bmp = drawable.toBitmap(size.toInt(), size.toInt())
            icons.add(
                FloatIcon(
                    x = Random.nextFloat() * w,
                    y = h + Random.nextFloat() * h,
                    speed = Random.nextFloat() * 0.6f + 0.25f,
                    size = size,
                    alpha = Random.nextInt(18, 46),
                    bitmap = bmp,
                    drift = (Random.nextFloat() - 0.5f) * 0.4f
                )
            )
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val h = height.takeIf { it > 0 } ?: return@addUpdateListener
                val w = width.takeIf { it > 0 } ?: return@addUpdateListener
                for (icon in icons) {
                    icon.y -= icon.speed
                    icon.x += icon.drift
                    if (icon.y < -icon.size) {
                        icon.y = h + icon.size
                        icon.x = Random.nextFloat() * w
                    }
                    if (icon.x < -icon.size) icon.x = w + icon.size
                    if (icon.x > w + icon.size) icon.x = -icon.size
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (icon in icons) {
            paint.alpha = icon.alpha
            canvas.drawBitmap(icon.bitmap, icon.x, icon.y, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
