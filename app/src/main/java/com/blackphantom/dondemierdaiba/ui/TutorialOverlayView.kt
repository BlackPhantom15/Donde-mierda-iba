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

    enum class HoleShape { CIRCLE, ROUNDED_RECT, PILL, TILE, NONE }

    private var holeRect: RectF? = null
    private var holeShape: HoleShape = HoleShape.NONE
    private val cornerRadiusPx = 18f * resources.displayMetrics.density

    private val scrimPaint = Paint().apply {
        color = Color.parseColor("#E00B0E10")
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
        color = Color.parseColor("#FF7F32")
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

    /**
     * Radio de esquina del agujero. PILL = cápsula perfecta (mitad del alto), igual que la
     * barra de pestañas, el buscador y el botón de agregar; TILE = los cuadrados redondeados
     * del encabezado (15dp de la vista + el margen del agujero).
     */
    private fun cornerFor(rect: RectF): Float = when (holeShape) {
        HoleShape.PILL -> minOf(rect.width(), rect.height()) / 2f
        HoleShape.TILE -> 25f * resources.displayMetrics.density
        else -> cornerRadiusPx
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
                HoleShape.ROUNDED_RECT, HoleShape.PILL, HoleShape.TILE -> {
                    val r = cornerFor(rect)
                    canvas.drawRoundRect(rect, r, r, clearPaint)
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
                HoleShape.ROUNDED_RECT, HoleShape.PILL, HoleShape.TILE -> {
                    val r = cornerFor(rect)
                    canvas.drawRoundRect(rect, r, r, glowPaint)
                }
                HoleShape.NONE -> {}
            }
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean = true
}
