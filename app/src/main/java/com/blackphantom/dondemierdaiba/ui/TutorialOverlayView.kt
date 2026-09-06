package com.blackphantom.dondemierdaiba.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.View
import android.widget.FrameLayout

/**
 * Fondo oscuro de pantalla completa con un "agujero" recortado sobre el
 * elemento que el tutorial está explicando en cada paso (efecto spotlight).
 * Se dibuja a mano con Canvas para no sumar ninguna librería externa.
 */
class TutorialOverlayView(context: Context) : FrameLayout(context) {

    enum class HoleShape { CIRCLE, ROUNDED_RECT, NONE }

    private var holeRect: RectF? = null
    private var holeShape: HoleShape = HoleShape.NONE
    private val cornerRadiusPx = 18f * resources.displayMetrics.density

    private val scrimPaint = Paint().apply {
        color = Color.parseColor("#D41E1C22")
        isAntiAlias = true
    }

    private val clearPaint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * resources.displayMetrics.density
        color = Color.parseColor("#E8792E")
    }

    init {
        setWillNotDraw(false)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        // Toda la superficie consume el toque: mientras el tutorial está
        // activo no se puede interactuar con lo que hay debajo, para que
        // nadie se pierda un paso sin querer.
        isClickable = true
        isFocusable = true
    }

    /** Actualiza dónde va el agujero. [targetView] null = paso de bienvenida, sin agujero. */
    fun setTarget(targetView: View?, shape: HoleShape, extraPaddingDp: Float = 10f) {
        if (targetView == null) {
            holeRect = null
            holeShape = HoleShape.NONE
            invalidate()
            return
        }

        val overlayLoc = IntArray(2)
        getLocationOnScreen(overlayLoc)
        val targetLoc = IntArray(2)
        targetView.getLocationOnScreen(targetLoc)

        val pad = extraPaddingDp * resources.displayMetrics.density
        val left = (targetLoc[0] - overlayLoc[0]).toFloat() - pad
        val top = (targetLoc[1] - overlayLoc[1]).toFloat() - pad
        val right = left + targetView.width + pad * 2
        val bottom = top + targetView.height + pad * 2

        holeRect = RectF(left, top, right, bottom)
        holeShape = shape
        invalidate()
    }

    /** Devuelve el agujero actual en coordenadas de este overlay (para posicionar el globo de texto). */
    fun currentHoleRect(): RectF? = holeRect

    override fun onDraw(canvas: Canvas) {
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)

        holeRect?.let { rect ->
            when (holeShape) {
                HoleShape.CIRCLE -> {
                    val radius = maxOf(rect.width(), rect.height()) / 2f
                    canvas.drawCircle(rect.centerX(), rect.centerY(), radius, clearPaint)
                }
                HoleShape.ROUNDED_RECT -> {
                    canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, clearPaint)
                }
                HoleShape.NONE -> { /* sin agujero: pantalla de bienvenida */ }
            }
        }
        canvas.restoreToCount(saveCount)

        // Brillo sutil alrededor del agujero, para que se note que ahí está la posta.
        holeRect?.let { rect ->
            when (holeShape) {
                HoleShape.CIRCLE -> {
                    val radius = maxOf(rect.width(), rect.height()) / 2f
                    canvas.drawCircle(rect.centerX(), rect.centerY(), radius, glowPaint)
                }
                HoleShape.ROUNDED_RECT -> {
                    canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, glowPaint)
                }
                HoleShape.NONE -> {}
            }
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean = true
}
